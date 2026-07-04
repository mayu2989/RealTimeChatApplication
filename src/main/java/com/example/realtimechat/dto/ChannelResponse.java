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
public class ChannelResponse {
    private String id;
    private String name;
    private String description;
    private Boolean isPrivate;
    private String workspaceId;
    private String createdById;
    private String createdByUsername;
    private LocalDateTime createdAt;
}