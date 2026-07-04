package com.example.realtimechat.service;

import com.example.realtimechat.dto.UpdateUserRequest;
import com.example.realtimechat.dto.UserProfileResponse;
import com.example.realtimechat.dto.UserResponse;
import com.example.realtimechat.entity.Channel;
import com.example.realtimechat.entity.User;
import com.example.realtimechat.entity.Workspace;
import com.example.realtimechat.exception.ApiException;
import com.example.realtimechat.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final FriendRequestRepository friendRequestRepository;
    private final DirectMessageRepository directMessageRepository;
    private final ChannelMessageRepository channelMessageRepository;
    private final ChannelMemberRepository channelMemberRepository;
    private final ChannelRepository channelRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final WorkspaceRepository workspaceRepository;
    private final PasswordEncoder passwordEncoder;

    public void setOnlineStatus(String email, Boolean isOnline) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        user.setIsOnline(isOnline);
        if (!isOnline) {
            user.setLastSeen(LocalDateTime.now());
        }
        userRepository.save(user);
    }

    public UserProfileResponse getProfile(String userEmail) {
        User user = requireUserByEmail(userEmail);
        return mapToProfile(user);
    }

    @Transactional
    public UserProfileResponse updateProfile(String userEmail, UpdateUserRequest request) {
        User user = requireUserByEmail(userEmail);

        if (request.getUsername() != null && !request.getUsername().isBlank()
                && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new ApiException("Username already taken", HttpStatus.CONFLICT);
            }
            user.setUsername(request.getUsername().trim());
        }

        if (request.getEmail() != null && !request.getEmail().isBlank()
                && !request.getEmail().equalsIgnoreCase(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new ApiException("Email already in use", HttpStatus.CONFLICT);
            }
            user.setEmail(request.getEmail().trim());
        }

        if (request.getDisplayName() != null && !request.getDisplayName().isBlank()) {
            user.setDisplayName(request.getDisplayName().trim());
        }

        if (request.getPhone() != null && !request.getPhone().isBlank()
                && !request.getPhone().equals(user.getPhone())) {
            if (userRepository.existsByPhone(request.getPhone())) {
                throw new ApiException("Phone number already in use", HttpStatus.CONFLICT);
            }
            user.setPhone(request.getPhone().trim());
        }

        if (request.getNewPassword() != null && !request.getNewPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        }

        userRepository.save(user);
        return mapToProfile(user);
    }

    @Transactional
    public void deleteAccount(String userEmail, String password) {
        User user = requireUserByEmail(userEmail);

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new ApiException("Incorrect password", HttpStatus.UNAUTHORIZED);
        }

        String userId = user.getId();

        List<Workspace> ownedWorkspaces = workspaceRepository.findByOwnerId(userId);
        for (Workspace workspace : ownedWorkspaces) {
            deleteWorkspaceData(workspace.getId());
            workspaceRepository.delete(workspace);
        }

        workspaceMemberRepository.findAllByUserId(userId)
                .forEach(workspaceMemberRepository::delete);

        channelMemberRepository.findAllByUserId(userId)
                .forEach(channelMemberRepository::delete);

        channelMessageRepository.deleteAllBySenderId(userId);
        directMessageRepository.deleteAllByUserId(userId);
        friendRequestRepository.findAllByUserId(userId)
                .forEach(friendRequestRepository::delete);

        channelRepository.findAllByCreatedById(userId).forEach(channel -> {
            deleteChannelData(channel.getId());
            channelRepository.delete(channel);
        });

        userRepository.delete(user);
    }

    private void deleteWorkspaceData(String workspaceId) {
        List<Channel> channels = channelRepository.findAllByWorkspaceId(workspaceId);
        for (Channel channel : channels) {
            deleteChannelData(channel.getId());
            channelRepository.delete(channel);
        }
        workspaceMemberRepository.deleteAllByWorkspaceId(workspaceId);
    }

    private void deleteChannelData(String channelId) {
        channelMessageRepository.deleteAllByChannelId(channelId);
        channelMemberRepository.deleteAllByChannelId(channelId);
    }

    public UserResponse getUserById(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        return mapToResponse(user);
    }

    public UserResponse getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        return mapToResponse(user);
    }

    public List<UserResponse> searchUsers(String keyword) {
        return userRepository.searchUsers(keyword)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private User requireUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
    }

    private UserProfileResponse mapToProfile(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .isOnline(user.getIsOnline())
                .lastSeen(user.getLastSeen())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .isOnline(user.getIsOnline())
                .build();
    }
}
