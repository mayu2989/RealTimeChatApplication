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
public class FriendRequestResponse {
    private String id;
    private String requesterId;
    private String requesterUsername;
    private String requesterDisplayName;
    private String receiverId;
    private String receiverUsername;
    private String receiverDisplayName;
    private String status;
    private LocalDateTime createdAt;
}
