package com.example.realtimechat.repository;

import com.example.realtimechat.entity.Channel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChannelRepository extends JpaRepository<Channel, String> {

    @Query("SELECT c FROM Channel c JOIN FETCH c.workspace WHERE c.workspace.id = :workspaceId")
    List<Channel> findAllByWorkspaceId(@Param("workspaceId") String workspaceId);

    @Query("SELECT c FROM Channel c JOIN FETCH c.workspace WHERE c.workspace.id = :workspaceId AND c.isPrivate = false")
    List<Channel> findAllPublicByWorkspaceId(@Param("workspaceId") String workspaceId);

    @Query("SELECT c FROM Channel c JOIN FETCH c.workspace WHERE c.id = :channelId")
    Optional<Channel> findByIdWithWorkspace(@Param("channelId") String channelId);

    @Query("SELECT c FROM Channel c WHERE c.createdBy.id = :userId")
    List<Channel> findAllByCreatedById(@Param("userId") String userId);
}
