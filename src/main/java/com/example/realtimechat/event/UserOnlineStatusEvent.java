package com.example.realtimechat.event;

public record UserOnlineStatusEvent(String email, boolean isOnline) {}
