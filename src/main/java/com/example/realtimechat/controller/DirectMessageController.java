package com.example.realtimechat.controller;

import com.example.realtimechat.dto.DirectMessageResponse;
import com.example.realtimechat.dto.MessageRequest;
import com.example.realtimechat.dto.UnreadCountResponse;
import com.example.realtimechat.service.DirectMessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/dm")
@RequiredArgsConstructor
public class DirectMessageController {

    private final DirectMessageService directMessageService;

    // REST endpoint to send DM
    @PostMapping("/send/{receiverId}")
    public ResponseEntity<DirectMessageResponse> sendMessage(
            @PathVariable String receiverId,
            @Valid @RequestBody MessageRequest request,
            Principal principal) {
        return ResponseEntity.ok(
                directMessageService.sendMessage(
                        principal.getName(), receiverId, request));
    }

    // Get full conversation between two users
    @GetMapping("/conversation/{receiverId}")
    public ResponseEntity<List<DirectMessageResponse>> getConversation(
            @PathVariable String receiverId,
            Principal principal) {
        return ResponseEntity.ok(
                directMessageService.getConversation(
                        principal.getName(), receiverId));
    }

    // Get unread messages
    @GetMapping("/unread")
    public ResponseEntity<List<DirectMessageResponse>> getUnread(
            Principal principal) {
        return ResponseEntity.ok(
                directMessageService.getUnreadMessages(principal.getName()));
    }

    // Unread count per sender
    @GetMapping("/unread/counts")
    public ResponseEntity<List<UnreadCountResponse>> getUnreadCounts(
            Principal principal) {
        return ResponseEntity.ok(
                directMessageService.getUnreadCounts(principal.getName()));
    }

    // Mark all messages from a sender as read
    @PutMapping("/read/conversation/{senderId}")
    public ResponseEntity<Void> markConversationAsRead(
            @PathVariable String senderId,
            Principal principal) {
        directMessageService.markConversationAsRead(principal.getName(), senderId);
        return ResponseEntity.ok().build();
    }

    // Mark message as read
    @PutMapping("/read/{messageId}")
    public ResponseEntity<Void> markAsRead(@PathVariable String messageId) {
        directMessageService.markAsRead(messageId);
        return ResponseEntity.ok().build();
    }
}
