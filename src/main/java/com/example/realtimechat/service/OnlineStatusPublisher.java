package com.example.realtimechat.service;

import com.example.realtimechat.event.UserOnlineStatusEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OnlineStatusPublisher {

    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void onUserOnlineStatus(UserOnlineStatusEvent event) {
        userService.setOnlineStatus(event.email(), event.isOnline());

        Map<String, Object> payload = new HashMap<>();
        payload.put("email", event.email());
        payload.put("isOnline", event.isOnline());

        messagingTemplate.convertAndSend("/topic/online-status", (Object) payload);
    }
}
