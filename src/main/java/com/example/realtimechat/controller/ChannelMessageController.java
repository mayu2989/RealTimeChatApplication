package com.example.realtimechat.controller;

import com.example.realtimechat.dto.ChannelMessageResponse;
import com.example.realtimechat.dto.MessageRequest;
import com.example.realtimechat.service.ChannelMessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/channel-messages")
@RequiredArgsConstructor
public class ChannelMessageController {

    private final ChannelMessageService channelMessageService;

    @PostMapping("/send/{channelId}")
    public ResponseEntity<ChannelMessageResponse> sendMessage(
            @PathVariable String channelId,
            @Valid @RequestBody MessageRequest request,
            Principal principal) {
        return ResponseEntity.ok(
                channelMessageService.sendMessage(
                        principal.getName(), channelId, request));
    }

    @GetMapping("/{channelId}")
    public ResponseEntity<List<ChannelMessageResponse>> getMessages(
            @PathVariable String channelId,
            Principal principal) {
        return ResponseEntity.ok(
                channelMessageService.getMessages(principal.getName(), channelId));
    }

    @PutMapping("/edit/{messageId}")
    public ResponseEntity<ChannelMessageResponse> editMessage(
            @PathVariable String messageId,
            @Valid @RequestBody MessageRequest request,
            Principal principal) {
        return ResponseEntity.ok(
                channelMessageService.editMessage(
                        principal.getName(), messageId, request));
    }

    @DeleteMapping("/delete/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable String messageId,
            Principal principal) {
        channelMessageService.deleteMessage(principal.getName(), messageId);
        return ResponseEntity.ok().build();
    }
}