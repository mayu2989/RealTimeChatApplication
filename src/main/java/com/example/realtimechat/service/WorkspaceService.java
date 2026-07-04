package com.example.realtimechat.service;


import com.example.realtimechat.dto.UserResponse;
import com.example.realtimechat.dto.WorkspaceRequest;
import com.example.realtimechat.dto.WorkspaceResponse;
import com.example.realtimechat.entity.Channel;
import com.example.realtimechat.entity.ChannelMember;
import com.example.realtimechat.entity.User;
import com.example.realtimechat.entity.Workspace;
import com.example.realtimechat.entity.WorkspaceMember;
import com.example.realtimechat.exception.ApiException;
import com.example.realtimechat.repository.ChannelMemberRepository;
import com.example.realtimechat.repository.ChannelRepository;
import com.example.realtimechat.repository.UserRepository;
import com.example.realtimechat.repository.WorkspaceMemberRepository;
import com.example.realtimechat.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final ChannelRepository channelRepository;
    private final ChannelMemberRepository channelMemberRepository;
    private final UserRepository userRepository;
    private final FriendService friendService;

    public WorkspaceResponse createWorkspace(String ownerEmail,
                                             WorkspaceRequest request) {
        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        Workspace workspace = Workspace.builder()
                .name(request.getName())
                .description(request.getDescription())
                .owner(owner)
                .build();

        workspaceRepository.save(workspace);

        // auto-add owner as ADMIN member
        WorkspaceMember member = WorkspaceMember.builder()
                .workspace(workspace)
                .user(owner)
                .role("ADMIN")
                .build();

        workspaceMemberRepository.save(member);

        return mapToResponse(workspace);
    }

    public List<WorkspaceResponse> getMyWorkspaces(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        return workspaceMemberRepository.findAllByUserId(user.getId())
                .stream()
                .map(wm -> mapToResponse(wm.getWorkspace()))
                .collect(Collectors.toList());
    }

    public WorkspaceResponse getWorkspace(String workspaceId) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() ->new ApiException("Workspace not found", HttpStatus.NOT_FOUND));
        return mapToResponse(workspace);
    }

    public void addMember(String ownerEmail, String workspaceId, String userId) {
        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ApiException("Workspace not found", HttpStatus.NOT_FOUND));

        if (!workspace.getOwner().getId().equals(owner.getId())) {
            throw new ApiException("Only the workspace owner can add members", HttpStatus.FORBIDDEN);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        friendService.ensureFriends(owner.getId(), userId);

        if (workspaceMemberRepository.existsByWorkspaceAndUser(
                workspaceId, userId)) {
            throw new ApiException("User already a member", HttpStatus.CONFLICT);
        }

        WorkspaceMember member = WorkspaceMember.builder()
                .workspace(workspace)
                .user(user)
                .role("MEMBER")
                .build();

        workspaceMemberRepository.save(member);

        channelRepository.findAllPublicByWorkspaceId(workspaceId).forEach(channel -> {
            if (!channelMemberRepository.existsByChannelAndUser(channel.getId(), userId)) {
                channelMemberRepository.save(ChannelMember.builder()
                        .channel(channel)
                        .user(user)
                        .build());
            }
        });
    }

    private WorkspaceResponse mapToResponse(Workspace workspace) {
        return WorkspaceResponse.builder()
                .id(workspace.getId())
                .name(workspace.getName())
                .description(workspace.getDescription())
                .ownerId(workspace.getOwner().getId())
                .ownerUsername(workspace.getOwner().getUsername())
                .createdAt(workspace.getCreatedAt())
                .build();
    }

    public List<UserResponse> getMembers(String workspaceId) {
        return workspaceMemberRepository.findAllByWorkspaceId(workspaceId)
                .stream()
                .map(wm -> UserResponse.builder()
                        .id(wm.getUser().getId())
                        .username(wm.getUser().getUsername())
                        .email(wm.getUser().getEmail())
                        .displayName(wm.getUser().getDisplayName())
                        .isOnline(wm.getUser().getIsOnline())
                        .role(wm.getRole())
                        .build())
                .collect(Collectors.toList());
    }

    public void removeMember(String workspaceId, String userId) {
        WorkspaceMember member = workspaceMemberRepository
                .findByWorkspaceAndUser(workspaceId, userId)
                .orElseThrow(() -> new ApiException("Member not found", HttpStatus.NOT_FOUND));
        workspaceMemberRepository.delete(member);
    }
}
