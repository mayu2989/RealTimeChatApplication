package com.example.realtimechat.service;

import com.example.realtimechat.event.FriendUpdateEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class FriendEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void onFriendUpdate(FriendUpdateEvent event) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", event.type());
        payload.put("requestId", event.requestId());
        payload.put("requesterId", event.requesterId());
        payload.put("receiverId", event.receiverId());

        messagingTemplate.convertAndSendToUser(
                event.recipientEmail(),
                "/queue/friends",
                payload
        );
    }
}
