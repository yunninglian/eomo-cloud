package com.prato.cloud.mq;

import com.prato.cloud.BaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate;

/**
 * 业务通知MQ实现
 */
public abstract class Mq4PayNotify extends BaseService {
    private static final Logger log = LoggerFactory.getLogger(Mq4PayNotify.class);
    @Autowired
    private RestTemplate restTemplate;

    /**
     * 发送延迟消息
     *
     * @param msg
     */
    public abstract void send(String msg);

    public void receive(String msg) {
        log.info("do notify task, msg={}", msg);
        JSONObject msgObj = JSON.parseObject(msg);
        String respUrl = msgObj.getString("url");
        String orderId = msgObj.getString("orderId");
        int count = msgObj.getInteger("count");
        if(StringUtils.isEmpty(respUrl)) {
            log.warn("notify url is empty. respUrl={}", respUrl);
            return;
        }
        try {
            String notifyResult = "";
            log.info("==>MQ通知业务系统开始[orderId：{}][count：{}][time：{}]", orderId, count, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            try {
                URI uri = new URI(respUrl);
                notifyResult = restTemplate.postForObject(uri, null, String.class);
            }catch (Exception e) {
                log.error(e, "通知商户系统异常");
            }
            log.info("<==MQ通知业务系统结束[orderId：{}][count：{}][time：{}]", orderId, count, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            // 验证结果
            log.info("notify response , OrderID={}", orderId);
            if(notifyResult.trim().equalsIgnoreCase("success")){
                //log.info("{} notify success, url:{}", _notifyInfo.getBusiId(), respUrl);
                //修改订单表
                try {
                    int result = super.baseUpdateStatus4Complete(orderId);
                    log.info("修改payOrderId={},订单状态为处理完成->{}", orderId, result == 1 ? "成功" : "失败");
                } catch (Exception e) {
                    log.error(e, "修改订单状态为处理完成异常");
                }
                // 修改通知次数
                try {
                    int result = super.baseUpdateNotify(orderId, (byte) 1);
                    log.info("修改payOrderId={},通知业务系统次数->{}", orderId, result == 1 ? "成功" : "失败");
                }catch (Exception e) {
                    log.error(e, "修改通知次数异常");
                }
                return ; // 通知成功结束
            }else {
                // 通知失败，延时再通知
                int cnt = count+1;
                log.info("notify count={}", cnt);
                // 修改通知次数
                try {
                    int result = super.baseUpdateNotify(orderId, (byte) cnt);
                    log.info("修改payOrderId={},通知业务系统次数->{}", orderId, result == 1 ? "成功" : "失败");
                }catch (Exception e) {
                    log.error(e, "修改通知次数异常");
                }

                if (cnt > 5) {
                    log.info("notify count>5 stop. url={}", respUrl);
                    return ;
                }
                msgObj.put("count", cnt);
                this.send(msgObj.toJSONString(), cnt * 60 * 1000);
            }
            log.warn("notify failed. url:{}, response body:{}", respUrl, notifyResult.toString());
        } catch(Exception e) {
            log.info("<==MQ通知业务系统结束[orderId：{}][count：{}][time：{}]", orderId, count, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            log.error(e, "notify exception. url:%s", respUrl);
        }
    }
}
