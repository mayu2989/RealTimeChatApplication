package com.example.realtimechat.repository;

import com.example.realtimechat.entity.FriendRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FriendRequestRepository extends JpaRepository<FriendRequest, String> {

    @Query("""
            SELECT fr FROM FriendRequest fr
            JOIN FETCH fr.requester
            JOIN FETCH fr.receiver
            WHERE fr.id = :requestId
            """)
    Optional<FriendRequest> findByIdWithUsers(@Param("requestId") String requestId);

    @Query("""
            SELECT fr FROM FriendRequest fr
            JOIN FETCH fr.requester
            JOIN FETCH fr.receiver
            WHERE fr.status = 'ACCEPTED'
            AND (fr.requester.id = :userId OR fr.receiver.id = :userId)
            """)
    List<FriendRequest> findAllAcceptedForUser(@Param("userId") String userId);

    @Query("""
            SELECT fr FROM FriendRequest fr
            JOIN FETCH fr.requester
            JOIN FETCH fr.receiver
            WHERE fr.receiver.id = :userId AND fr.status = 'PENDING'
            ORDER BY fr.createdAt DESC
            """)
    List<FriendRequest> findIncomingPending(@Param("userId") String userId);

    @Query("""
            SELECT fr FROM FriendRequest fr
            JOIN FETCH fr.requester
            JOIN FETCH fr.receiver
            WHERE fr.requester.id = :userId AND fr.status = 'PENDING'
            ORDER BY fr.createdAt DESC
            """)
    List<FriendRequest> findOutgoingPending(@Param("userId") String userId);

    @Query("""
            SELECT fr FROM FriendRequest fr
            WHERE fr.requester.id = :requesterId AND fr.receiver.id = :receiverId
            """)
    Optional<FriendRequest> findByRequesterAndReceiver(@Param("requesterId") String requesterId,
                                                       @Param("receiverId") String receiverId);

    @Query("""
            SELECT COUNT(fr) > 0 FROM FriendRequest fr
            WHERE fr.status = 'ACCEPTED'
            AND ((fr.requester.id = :userId1 AND fr.receiver.id = :userId2)
              OR (fr.requester.id = :userId2 AND fr.receiver.id = :userId1))
            """)
    boolean areFriends(@Param("userId1") String userId1, @Param("userId2") String userId2);

    @Query("""
            SELECT fr FROM FriendRequest fr
            WHERE fr.requester.id = :userId OR fr.receiver.id = :userId
            """)
    List<FriendRequest> findAllByUserId(@Param("userId") String userId);
}
