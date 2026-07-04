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
public class DirectMessageResponse {
    private String id;
    private String senderId;
    private String senderUsername;
    private String receiverId;
    private String content;
    private String messageType;
    private String fileUrl;
    private Boolean isRead;
    private Boolean isEdited;
    private LocalDateTime createdAt;
}