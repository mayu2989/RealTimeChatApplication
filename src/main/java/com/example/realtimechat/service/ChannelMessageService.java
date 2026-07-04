package com.example.realtimechat.service;

import com.example.realtimechat.dto.ChannelMessageResponse;
import com.example.realtimechat.dto.MessageRequest;
import com.example.realtimechat.entity.Channel;
import com.example.realtimechat.entity.ChannelMember;
import com.example.realtimechat.entity.ChannelMessage;
import com.example.realtimechat.entity.User;
import com.example.realtimechat.exception.ApiException;
import com.example.realtimechat.repository.ChannelMemberRepository;
import com.example.realtimechat.repository.ChannelMessageRepository;
import com.example.realtimechat.repository.ChannelRepository;
import com.example.realtimechat.repository.UserRepository;
import com.example.realtimechat.repository.WorkspaceMemberRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChannelMessageService {

    private final ChannelMessageRepository channelMessageRepository;
    private final ChannelRepository channelRepository;
    private final ChannelMemberRepository channelMemberRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserRepository userRepository;

    @Lazy
    private final SimpMessagingTemplate messagingTemplate;

    public ChannelMessageService(ChannelMessageRepository channelMessageRepository,
                                 ChannelRepository channelRepository,
                                 ChannelMemberRepository channelMemberRepository,
                                 WorkspaceMemberRepository workspaceMemberRepository,
                                 UserRepository userRepository,
                                 @Lazy SimpMessagingTemplate messagingTemplate) {
        this.channelMessageRepository = channelMessageRepository;
        this.channelRepository = channelRepository;
        this.channelMemberRepository = channelMemberRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public ChannelMessageResponse sendMessage(String senderEmail,
                                              String channelId,
                                              MessageRequest request) {
        User sender = userRepository.findByEmail(senderEmail)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        Channel channel = channelRepository.findByIdWithWorkspace(channelId)
                .orElseThrow(() -> new ApiException("Channel not found", HttpStatus.NOT_FOUND));

        ensureCanParticipate(sender, channel);

        ChannelMessage message = ChannelMessage.builder()
                .channel(channel)
                .sender(sender)
                .content(request.getContent())
                .messageType(request.getMessageType())
                .fileUrl(request.getFileUrl())
                .build();

        channelMessageRepository.save(message);

        ChannelMessageResponse response = mapToResponse(message);

        messagingTemplate.convertAndSend(
                "/topic/channel." + channelId,
                response
        );

        return response;
    }

    @Transactional
    public List<ChannelMessageResponse> getMessages(String userEmail, String channelId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        Channel channel = channelRepository.findByIdWithWorkspace(channelId)
                .orElseThrow(() -> new ApiException("Channel not found", HttpStatus.NOT_FOUND));

        ensureCanParticipate(user, channel);

        return channelMessageRepository
                .findByChannelIdOrderByCreatedAtAsc(channelId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public ChannelMessageResponse editMessage(String userEmail,
                                              String messageId,
                                              MessageRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        ChannelMessage message = channelMessageRepository.findById(messageId)
                .orElseThrow(() -> new ApiException("Message not found", HttpStatus.NOT_FOUND));

        if (!message.getSender().getId().equals(user.getId())) {
            throw new ApiException("You can only edit your own messages", HttpStatus.FORBIDDEN);
        }

        message.setContent(request.getContent());
        message.setIsEdited(true);
        channelMessageRepository.save(message);

        ChannelMessageResponse response = mapToResponse(message);

        messagingTemplate.convertAndSend(
                "/topic/channel." + message.getChannel().getId(),
                response
        );

        return response;
    }

    public void deleteMessage(String userEmail, String messageId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        ChannelMessage message = channelMessageRepository.findById(messageId)
                .orElseThrow(() -> new ApiException("Message not found", HttpStatus.NOT_FOUND));

        if (!message.getSender().getId().equals(user.getId())) {
            throw new ApiException("You can only delete your own messages", HttpStatus.FORBIDDEN);
        }

        channelMessageRepository.delete(message);
    }

    private void ensureCanParticipate(User user, Channel channel) {
        if (channelMemberRepository.existsByChannelAndUser(channel.getId(), user.getId())) {
            return;
        }

        String workspaceId = channel.getWorkspace().getId();
        boolean isWorkspaceMember = workspaceMemberRepository.existsByWorkspaceAndUser(
                workspaceId, user.getId());

        if (!isWorkspaceMember) {
            throw new ApiException("You are not a member of this workspace", HttpStatus.FORBIDDEN);
        }

        if (Boolean.TRUE.equals(channel.getIsPrivate())) {
            throw new ApiException("You are not a member of this private channel", HttpStatus.FORBIDDEN);
        }

        channelMemberRepository.save(ChannelMember.builder()
                .channel(channel)
                .user(user)
                .build());
    }

    private ChannelMessageResponse mapToResponse(ChannelMessage message) {
        return ChannelMessageResponse.builder()
                .id(message.getId())
                .channelId(message.getChannel().getId())
                .senderId(message.getSender().getId())
                .senderUsername(message.getSender().getUsername())
                .content(message.getContent())
                .messageType(message.getMessageType())
                .fileUrl(message.getFileUrl())
                .isEdited(message.getIsEdited())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
