package com.example.realtimechat.controller;

import com.example.realtimechat.dto.FriendRequestResponse;
import com.example.realtimechat.dto.UserResponse;
import com.example.realtimechat.service.FriendService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/friends")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;

    @GetMapping
    public List<UserResponse> getFriends(Principal principal) {
        return friendService.getFriends(principal.getName());
    }

    @GetMapping("/requests/incoming")
    public List<FriendRequestResponse> getIncomingRequests(Principal principal) {
        return friendService.getIncomingRequests(principal.getName());
    }

    @GetMapping("/requests/outgoing")
    public List<FriendRequestResponse> getOutgoingRequests(Principal principal) {
        return friendService.getOutgoingRequests(principal.getName());
    }

    @PostMapping("/requests/{receiverId}")
    @ResponseStatus(HttpStatus.CREATED)
    public FriendRequestResponse sendRequest(@PathVariable String receiverId, Principal principal) {
        return friendService.sendRequest(principal.getName(), receiverId);
    }

    @PostMapping("/requests/{requestId}/accept")
    public FriendRequestResponse acceptRequest(@PathVariable String requestId, Principal principal) {
        return friendService.acceptRequest(principal.getName(), requestId);
    }

    @PostMapping("/requests/{requestId}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void rejectRequest(@PathVariable String requestId, Principal principal) {
        friendService.rejectRequest(principal.getName(), requestId);
    }

    @DeleteMapping("/{friendId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeFriend(@PathVariable String friendId, Principal principal) {
        friendService.removeFriend(principal.getName(), friendId);
    }
}
