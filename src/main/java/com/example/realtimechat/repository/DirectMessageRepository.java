package com.example.realtimechat.repository;

import com.example.realtimechat.entity.DirectMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DirectMessageRepository extends JpaRepository<DirectMessage, String> {

    @Query("SELECT m FROM DirectMessage m WHERE " +
            "(m.sender.id = :senderId AND m.receiver.id = :receiverId) OR " +
            "(m.sender.id = :receiverId AND m.receiver.id = :senderId) " +
            "ORDER BY m.createdAt ASC")
    List<DirectMessage> findConversation(@Param("senderId") String senderId,
                                         @Param("receiverId") String receiverId);

    @Query("""
            SELECT m FROM DirectMessage m
            JOIN FETCH m.sender
            JOIN FETCH m.receiver
            WHERE m.receiver.id = :receiverId AND m.isRead = false
            ORDER BY m.createdAt ASC
            """)
    List<DirectMessage> findUnreadByReceiverId(@Param("receiverId") String receiverId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE DirectMessage m SET m.isRead = true
            WHERE m.receiver.id = :receiverId
            AND m.sender.id = :senderId
            AND m.isRead = false
            """)
    int markConversationAsRead(@Param("receiverId") String receiverId,
                               @Param("senderId") String senderId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM DirectMessage m WHERE m.sender.id = :userId OR m.receiver.id = :userId")
    void deleteAllByUserId(@Param("userId") String userId);
}
