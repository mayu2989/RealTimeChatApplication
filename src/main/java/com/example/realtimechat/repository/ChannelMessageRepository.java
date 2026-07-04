package com.example.realtimechat.repository;


import com.example.realtimechat.entity.ChannelMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChannelMessageRepository extends JpaRepository<ChannelMessage, String> {
    List<ChannelMessage> findByChannelIdOrderByCreatedAtAsc(String channelId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM ChannelMessage m WHERE m.sender.id = :userId")
    void deleteAllBySenderId(@Param("userId") String userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM ChannelMessage m WHERE m.channel.id = :channelId")
    void deleteAllByChannelId(@Param("channelId") String channelId);
}