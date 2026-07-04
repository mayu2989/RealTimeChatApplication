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
public class ChannelMessageResponse {
    private String id;
    private String channelId;
    private String senderId;
    private String senderUsername;
    private String content;
    private String messageType;
    private String fileUrl;
    private Boolean isEdited;
    private LocalDateTime createdAt;
}
