package com.prato.cloud.service;

import com.prato.cloud.entity.Relationship;
import com.prato.cloud.entity.User;
import org.springframework.data.domain.Page;

import java.awt.print.Pageable;
import java.util.List;

public interface RelationshipService {
    /**
     * 列出所有的关注者
     *
     * @return
     */
    Page<User> listFollows(Long userId, Pageable pageable);
    /**
     * 列出所有的粉丝
     *
     * @return
     */
    Page<User> listFans(Long userId, Pageable pageable);
    /**
     * 列出互相关注的id
     *
     * @param userId
     * @return
     */
    List<Integer> listFriends(Long userId);
    /**
     * 添加关系
     *
     * @param relationship
     */
    void saveRelationship(Relationship relationship);
    /**
     * 去除关系
     *
     * @param relationship
     */
    void removeRelationship(Relationship relationship);
    /**
     * 更新关注数
     */
    void updateFollowSize(Long userId);
    /**
     * 更新粉丝数
     */
    void updateFanSize(Long userId);
}
