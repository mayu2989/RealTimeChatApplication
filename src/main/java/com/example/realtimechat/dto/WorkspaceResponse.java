package com.example.realtimechat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkspaceResponse {
    private String id;
    private String name;
    private String description;
    private String ownerId;
    private String ownerUsername;
    private LocalDateTime createdAt;
}
