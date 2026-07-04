package com.example.realtimechat.event;

public record FriendUpdateEvent(
        String recipientEmail,
        String type,
        String requestId,
        String requesterId,
        String receiverId
) {
    public static final String INCOMING_REQUEST = "INCOMING_REQUEST";
    public static final String ACCEPTED = "ACCEPTED";
    public static final String REJECTED = "REJECTED";
    public static final String REMOVED = "REMOVED";
}
