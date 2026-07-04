package com.example.realtimechat.controller;

import com.example.realtimechat.dto.ChannelRequest;
import com.example.realtimechat.dto.ChannelResponse;
import com.example.realtimechat.dto.UserResponse;
import com.example.realtimechat.service.ChannelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/channels")
@RequiredArgsConstructor
public class ChannelController {

    private final ChannelService channelService;

    @PostMapping("/workspace/{workspaceId}")
    public ResponseEntity<ChannelResponse> createChannel(
            @PathVariable String workspaceId,
            @Valid @RequestBody ChannelRequest request,
            Principal principal) {
        return ResponseEntity.ok(
                channelService.createChannel(
                        principal.getName(), workspaceId, request));
    }

    @GetMapping("/workspace/{workspaceId}")
    public ResponseEntity<List<ChannelResponse>> getChannels(
            @PathVariable String workspaceId) {
        return ResponseEntity.ok(channelService.getChannels(workspaceId));
    }

    @GetMapping("/workspace/{workspaceId}/public")
    public ResponseEntity<List<ChannelResponse>> getPublicChannels(
            @PathVariable String workspaceId) {
        return ResponseEntity.ok(channelService.getPublicChannels(workspaceId));
    }

    @GetMapping("/{channelId}")
    public ResponseEntity<ChannelResponse> getChannel(
            @PathVariable String channelId) {
        return ResponseEntity.ok(channelService.getChannel(channelId));
    }

    @PostMapping("/{channelId}/join")
    public ResponseEntity<Void> joinChannel(
            @PathVariable String channelId,
            Principal principal) {
        channelService.joinChannel(principal.getName(), channelId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{channelId}/leave")
    public ResponseEntity<Void> leaveChannel(
            @PathVariable String channelId,
            Principal principal) {
        channelService.leaveChannel(principal.getName(), channelId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{channelId}/members")
    public ResponseEntity<List<UserResponse>> getChannelMembers(
            @PathVariable String channelId) {
        return ResponseEntity.ok(channelService.getChannelMembers(channelId));
    }
}

