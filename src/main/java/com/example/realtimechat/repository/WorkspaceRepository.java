package com.example.realtimechat.repository;

import com.example.realtimechat.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkspaceRepository extends JpaRepository<Workspace,String> {
    List<Workspace> findByOwnerId(String ownerId);
}
