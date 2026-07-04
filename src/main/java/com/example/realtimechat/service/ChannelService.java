package com.example.realtimechat.service;

import com.example.realtimechat.dto.ChannelRequest;
import com.example.realtimechat.dto.ChannelResponse;
import com.example.realtimechat.dto.UserResponse;
import com.example.realtimechat.entity.Channel;
import com.example.realtimechat.entity.ChannelMember;
import com.example.realtimechat.entity.User;
import com.example.realtimechat.entity.Workspace;
import com.example.realtimechat.exception.ApiException;
import com.example.realtimechat.repository.ChannelMemberRepository;
import com.example.realtimechat.repository.ChannelRepository;
import com.example.realtimechat.repository.UserRepository;
import com.example.realtimechat.repository.WorkspaceMemberRepository;
import com.example.realtimechat.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChannelService {

    private final ChannelRepository channelRepository;
    private final ChannelMemberRepository channelMemberRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserRepository userRepository;

    @Transactional
    public ChannelResponse createChannel(String userEmail,
                                         String workspaceId,
                                         ChannelRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ApiException("Workspace not found", HttpStatus.NOT_FOUND));

        if (!workspace.getOwner().getId().equals(user.getId())) {
            throw new ApiException("Only the workspace owner can create channels", HttpStatus.FORBIDDEN);
        }

        Channel channel = Channel.builder()
                .name(request.getName())
                .description(request.getDescription())
                .isPrivate(request.getIsPrivate())
                .workspace(workspace)
                .createdBy(user)
                .build();

        channelRepository.save(channel);

        if (Boolean.TRUE.equals(request.getIsPrivate())) {
            channelMemberRepository.save(ChannelMember.builder()
                    .channel(channel)
                    .user(user)
                    .build());
        } else {
            addAllWorkspaceMembersToChannel(channel, workspaceId);
        }

        return mapToResponse(channel);
    }

    private void addAllWorkspaceMembersToChannel(Channel channel, String workspaceId) {
        workspaceMemberRepository.findAllByWorkspaceId(workspaceId).forEach(wm -> {
            if (!channelMemberRepository.existsByChannelAndUser(
                    channel.getId(), wm.getUser().getId())) {
                channelMemberRepository.save(ChannelMember.builder()
                        .channel(channel)
                        .user(wm.getUser())
                        .build());
            }
        });
    }

    public List<ChannelResponse> getChannels(String workspaceId) {
        return channelRepository.findAllByWorkspaceId(workspaceId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<ChannelResponse> getPublicChannels(String workspaceId) {
        return channelRepository.findAllPublicByWorkspaceId(workspaceId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public ChannelResponse getChannel(String channelId) {
        Channel channel = channelRepository.findByIdWithWorkspace(channelId)
                .orElseThrow(() -> new ApiException("Channel not found", HttpStatus.NOT_FOUND));
        return mapToResponse(channel);
    }

    @Transactional
    public void joinChannel(String userEmail, String channelId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        Channel channel = channelRepository.findByIdWithWorkspace(channelId)
                .orElseThrow(() -> new ApiException("Channel not found", HttpStatus.NOT_FOUND));

        if (channelMemberRepository.existsByChannelAndUser(channelId, user.getId())) {
            return;
        }

        if (!workspaceMemberRepository.existsByWorkspaceAndUser(
                channel.getWorkspace().getId(), user.getId())) {
            throw new ApiException("You are not a member of this workspace", HttpStatus.FORBIDDEN);
        }

        if (Boolean.TRUE.equals(channel.getIsPrivate())) {
            throw new ApiException("This is a private channel", HttpStatus.FORBIDDEN);
        }

        channelMemberRepository.save(ChannelMember.builder()
                .channel(channel)
                .user(user)
                .build());
    }

    public void leaveChannel(String userEmail, String channelId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        ChannelMember member = channelMemberRepository
                .findByChannelAndUser(channelId, user.getId())
                .orElseThrow(() -> new ApiException("Not a member", HttpStatus.NOT_FOUND));

        channelMemberRepository.delete(member);
    }

    public List<UserResponse> getChannelMembers(String channelId) {
        return channelMemberRepository.findAllByChannelId(channelId)
                .stream()
                .map(cm -> UserResponse.builder()
                        .id(cm.getUser().getId())
                        .username(cm.getUser().getUsername())
                        .email(cm.getUser().getEmail())
                        .displayName(cm.getUser().getDisplayName())
                        .isOnline(cm.getUser().getIsOnline())
                        .build())
                .collect(Collectors.toList());
    }

    private ChannelResponse mapToResponse(Channel channel) {
        return ChannelResponse.builder()
                .id(channel.getId())
                .name(channel.getName())
                .description(channel.getDescription())
                .isPrivate(channel.getIsPrivate())
                .workspaceId(channel.getWorkspace().getId())
                .createdById(channel.getCreatedBy().getId())
                .createdByUsername(channel.getCreatedBy().getUsername())
                .createdAt(channel.getCreatedAt())
                .build();
    }
}
