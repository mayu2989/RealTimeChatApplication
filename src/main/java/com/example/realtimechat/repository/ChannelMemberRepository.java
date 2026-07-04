package com.example.realtimechat.repository;

import com.example.realtimechat.entity.ChannelMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChannelMemberRepository extends JpaRepository<ChannelMember, String> {

    @Query("SELECT cm FROM ChannelMember cm JOIN FETCH cm.user WHERE cm.channel.id = :channelId")
    List<ChannelMember> findAllByChannelId(@Param("channelId") String channelId);

    @Query("SELECT cm FROM ChannelMember cm WHERE cm.user.id = :userId")
    List<ChannelMember> findAllByUserId(@Param("userId") String userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM ChannelMember cm WHERE cm.channel.id = :channelId")
    void deleteAllByChannelId(@Param("channelId") String channelId);

    @Query("SELECT cm FROM ChannelMember cm WHERE cm.channel.id = :channelId AND cm.user.id = :userId")
    Optional<ChannelMember> findByChannelAndUser(@Param("channelId") String channelId,
                                                 @Param("userId") String userId);

    @Query("SELECT COUNT(cm) > 0 FROM ChannelMember cm WHERE cm.channel.id = :channelId AND cm.user.id = :userId")
    boolean existsByChannelAndUser(@Param("channelId") String channelId,
                                   @Param("userId") String userId);
}
