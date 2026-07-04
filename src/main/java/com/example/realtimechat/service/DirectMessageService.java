package com.example.realtimechat.service;


import com.example.realtimechat.dto.DirectMessageResponse;
import com.example.realtimechat.dto.MessageRequest;
import com.example.realtimechat.dto.UnreadCountResponse;
import com.example.realtimechat.entity.DirectMessage;
import com.example.realtimechat.entity.User;
import com.example.realtimechat.exception.ApiException;
import com.example.realtimechat.repository.DirectMessageRepository;
import com.example.realtimechat.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DirectMessageService {

    private final DirectMessageRepository directMessageRepository;
    private final UserRepository userRepository;
    private final FriendService friendService;
    private final SimpMessagingTemplate messagingTemplate;

    public DirectMessageResponse sendMessage(String senderEmail,
                                             String receiverId,
                                             MessageRequest request) {
        User sender = userRepository.findByEmail(senderEmail)
                .orElseThrow(() -> new ApiException("Sender not found", HttpStatus.NOT_FOUND));

        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new ApiException("Receiver not found", HttpStatus.NOT_FOUND));

        friendService.ensureFriends(sender.getId(), receiver.getId());

        DirectMessage message = DirectMessage.builder()
                .sender(sender)
                .receiver(receiver)
                .content(request.getContent())
                .messageType(request.getMessageType())
                .fileUrl(request.getFileUrl())
                .build();

        directMessageRepository.save(message);

        DirectMessageResponse response = mapToResponse(message);

        // deliver via WebSocket to receiver
        messagingTemplate.convertAndSendToUser(
                receiver.getEmail(),
                "/queue/messages",
                response
        );

        return response;
    }

    @Transactional
    public List<DirectMessageResponse> getConversation(String senderEmail,
                                                       String receiverId) {
        User sender = userRepository.findByEmail(senderEmail)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        friendService.ensureFriends(sender.getId(), receiverId);

        directMessageRepository.markConversationAsRead(sender.getId(), receiverId);

        return directMessageRepository
                .findConversation(sender.getId(), receiverId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<DirectMessageResponse> getUnreadMessages(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        return directMessageRepository
                .findUnreadByReceiverId(user.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<UnreadCountResponse> getUnreadCounts(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        Map<String, Long> counts = directMessageRepository
                .findUnreadByReceiverId(user.getId())
                .stream()
                .collect(Collectors.groupingBy(
                        m -> m.getSender().getId(),
                        Collectors.counting()));

        return counts.entrySet().stream()
                .map(e -> UnreadCountResponse.builder()
                        .senderId(e.getKey())
                        .count(e.getValue())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public void markConversationAsRead(String userEmail, String senderId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        directMessageRepository.markConversationAsRead(user.getId(), senderId);
    }

    public void markAsRead(String messageId) {
        DirectMessage message = directMessageRepository.findById(messageId)
                .orElseThrow(() -> new ApiException("Message not found", HttpStatus.NOT_FOUND));
        message.setIsRead(true);
        directMessageRepository.save(message);
    }

    private DirectMessageResponse mapToResponse(DirectMessage message) {
        return DirectMessageResponse.builder()
                .id(message.getId())
                .senderId(message.getSender().getId())
                .senderUsername(message.getSender().getUsername())
                .receiverId(message.getReceiver().getId())
                .content(message.getContent())
                .messageType(message.getMessageType())
                .fileUrl(message.getFileUrl())
                .isRead(message.getIsRead())
                .isEdited(message.getIsEdited())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
