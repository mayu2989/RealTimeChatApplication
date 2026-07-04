package com.example.realtimechat.service;

import com.example.realtimechat.dto.FriendRequestResponse;
import com.example.realtimechat.dto.UserResponse;
import com.example.realtimechat.entity.FriendRequest;
import com.example.realtimechat.entity.User;
import com.example.realtimechat.exception.ApiException;
import com.example.realtimechat.event.FriendUpdateEvent;
import com.example.realtimechat.repository.FriendRequestRepository;
import com.example.realtimechat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FriendService {

    private final FriendRequestRepository friendRequestRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public List<UserResponse> getFriends(String userEmail) {
        User user = requireUser(userEmail);

        return friendRequestRepository.findAllAcceptedForUser(user.getId())
                .stream()
                .map(fr -> mapUserToResponse(otherUser(fr, user.getId())))
                .collect(Collectors.toList());
    }

    public List<FriendRequestResponse> getIncomingRequests(String userEmail) {
        User user = requireUser(userEmail);
        return friendRequestRepository.findIncomingPending(user.getId())
                .stream()
                .map(this::mapRequestToResponse)
                .collect(Collectors.toList());
    }

    public List<FriendRequestResponse> getOutgoingRequests(String userEmail) {
        User user = requireUser(userEmail);
        return friendRequestRepository.findOutgoingPending(user.getId())
                .stream()
                .map(this::mapRequestToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public FriendRequestResponse sendRequest(String userEmail, String receiverId) {
        User requester = requireUser(userEmail);

        if (requester.getId().equals(receiverId)) {
            throw new ApiException("Cannot send a friend request to yourself", HttpStatus.BAD_REQUEST);
        }

        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        if (friendRequestRepository.areFriends(requester.getId(), receiverId)) {
            throw new ApiException("Already friends", HttpStatus.CONFLICT);
        }

        Optional<FriendRequest> reverse = friendRequestRepository
                .findByRequesterAndReceiver(receiverId, requester.getId());

        if (reverse.isPresent()) {
            FriendRequest existing = reverse.get();
            if (FriendRequest.STATUS_PENDING.equals(existing.getStatus())) {
                return acceptRequest(userEmail, existing.getId());
            }
            if (FriendRequest.STATUS_ACCEPTED.equals(existing.getStatus())) {
                throw new ApiException("Already friends", HttpStatus.CONFLICT);
            }
        }

        Optional<FriendRequest> existing = friendRequestRepository
                .findByRequesterAndReceiver(requester.getId(), receiverId);

        if (existing.isPresent()) {
            FriendRequest request = existing.get();
        if (FriendRequest.STATUS_PENDING.equals(request.getStatus())) {
            return mapRequestToResponse(friendRequestRepository.findByIdWithUsers(request.getId())
                    .orElseThrow());
        }
            if (FriendRequest.STATUS_ACCEPTED.equals(request.getStatus())) {
                throw new ApiException("Already friends", HttpStatus.CONFLICT);
            }
            request.setStatus(FriendRequest.STATUS_PENDING);
            request.setRespondedAt(null);
            friendRequestRepository.save(request);
            FriendRequestResponse response = mapRequestToResponse(request);
            publishFriendEvent(FriendUpdateEvent.INCOMING_REQUEST, receiver.getEmail(),
                    response.getId(), requester.getId(), receiver.getId());
            return response;
        }

        FriendRequest request = friendRequestRepository.save(FriendRequest.builder()
                .requester(requester)
                .receiver(receiver)
                .status(FriendRequest.STATUS_PENDING)
                .build());

        FriendRequestResponse response = mapRequestToResponse(
                friendRequestRepository.findByIdWithUsers(request.getId()).orElseThrow());

        publishFriendEvent(FriendUpdateEvent.INCOMING_REQUEST, receiver.getEmail(),
                response.getId(), requester.getId(), receiver.getId());

        return response;
    }

    @Transactional
    public FriendRequestResponse acceptRequest(String userEmail, String requestId) {
        User user = requireUser(userEmail);
        FriendRequest request = friendRequestRepository.findByIdWithUsers(requestId)
                .orElseThrow(() -> new ApiException("Friend request not found", HttpStatus.NOT_FOUND));

        if (!request.getReceiver().getId().equals(user.getId())) {
            throw new ApiException("You can only accept requests sent to you", HttpStatus.FORBIDDEN);
        }

        if (!FriendRequest.STATUS_PENDING.equals(request.getStatus())) {
            throw new ApiException("Request is no longer pending", HttpStatus.BAD_REQUEST);
        }

        request.setStatus(FriendRequest.STATUS_ACCEPTED);
        request.setRespondedAt(LocalDateTime.now());
        friendRequestRepository.save(request);

        FriendRequestResponse response = mapRequestToResponse(request);

        publishFriendEvent(FriendUpdateEvent.ACCEPTED, request.getRequester().getEmail(),
                request.getId(), request.getRequester().getId(), request.getReceiver().getId());

        return response;
    }

    @Transactional
    public void rejectRequest(String userEmail, String requestId) {
        User user = requireUser(userEmail);
        FriendRequest request = friendRequestRepository.findByIdWithUsers(requestId)
                .orElseThrow(() -> new ApiException("Friend request not found", HttpStatus.NOT_FOUND));

        if (!request.getReceiver().getId().equals(user.getId())) {
            throw new ApiException("You can only reject requests sent to you", HttpStatus.FORBIDDEN);
        }

        if (!FriendRequest.STATUS_PENDING.equals(request.getStatus())) {
            throw new ApiException("Request is no longer pending", HttpStatus.BAD_REQUEST);
        }

        request.setStatus(FriendRequest.STATUS_REJECTED);
        request.setRespondedAt(LocalDateTime.now());
        friendRequestRepository.save(request);

        publishFriendEvent(FriendUpdateEvent.REJECTED, request.getRequester().getEmail(),
                request.getId(), request.getRequester().getId(), request.getReceiver().getId());
    }

    @Transactional
    public void removeFriend(String userEmail, String friendId) {
        User user = requireUser(userEmail);

        if (!friendRequestRepository.areFriends(user.getId(), friendId)) {
            throw new ApiException("Not friends", HttpStatus.NOT_FOUND);
        }

        friendRequestRepository.findByRequesterAndReceiver(user.getId(), friendId)
                .ifPresent(friendRequestRepository::delete);
        friendRequestRepository.findByRequesterAndReceiver(friendId, user.getId())
                .ifPresent(friendRequestRepository::delete);

        userRepository.findById(friendId).ifPresent(friend ->
                publishFriendEvent(FriendUpdateEvent.REMOVED, friend.getEmail(),
                        null, user.getId(), friendId));
    }

    public boolean areFriends(String userId1, String userId2) {
        return friendRequestRepository.areFriends(userId1, userId2);
    }

    public void ensureFriends(String userId1, String userId2) {
        if (!areFriends(userId1, userId2)) {
            throw new ApiException("You can only interact with accepted friends", HttpStatus.FORBIDDEN);
        }
    }

    private User requireUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
    }

    private User otherUser(FriendRequest request, String userId) {
        return request.getRequester().getId().equals(userId)
                ? request.getReceiver()
                : request.getRequester();
    }

    private UserResponse mapUserToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .isOnline(user.getIsOnline())
                .build();
    }

    private FriendRequestResponse mapRequestToResponse(FriendRequest request) {
        return FriendRequestResponse.builder()
                .id(request.getId())
                .requesterId(request.getRequester().getId())
                .requesterUsername(request.getRequester().getUsername())
                .requesterDisplayName(request.getRequester().getDisplayName())
                .receiverId(request.getReceiver().getId())
                .receiverUsername(request.getReceiver().getUsername())
                .receiverDisplayName(request.getReceiver().getDisplayName())
                .status(request.getStatus())
                .createdAt(request.getCreatedAt())
                .build();
    }

    private void publishFriendEvent(String type, String recipientEmail,
                                    String requestId, String requesterId, String receiverId) {
        eventPublisher.publishEvent(new FriendUpdateEvent(
                recipientEmail, type, requestId, requesterId, receiverId));
    }
}
