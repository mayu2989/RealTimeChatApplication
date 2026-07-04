package com.example.realtimechat.controller;

import com.example.realtimechat.dto.MessageRequest;
import com.example.realtimechat.service.ChannelMessageService;
import com.example.realtimechat.service.DirectMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class WebSocketController {

    private final DirectMessageService directMessageService;
    private final ChannelMessageService channelMessageService;

    @MessageMapping("/dm.send/{receiverId}")
    public void sendDirectMessage(@DestinationVariable String receiverId,
                                  @Payload MessageRequest request,
                                  Principal principal) {
        directMessageService.sendMessage(principal.getName(), receiverId, request);
    }

    @MessageMapping("/channel.send/{channelId}")
    public void sendChannelMessage(@DestinationVariable String channelId,
                                   @Payload MessageRequest request,
                                   Principal principal) {
        channelMessageService.sendMessage(principal.getName(), channelId, request);
    }
}