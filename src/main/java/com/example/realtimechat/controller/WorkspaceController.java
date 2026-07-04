package com.example.realtimechat.controller;

import com.example.realtimechat.dto.UserResponse;
import com.example.realtimechat.dto.WorkspaceRequest;
import com.example.realtimechat.dto.WorkspaceResponse;
import com.example.realtimechat.service.WorkspaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    @PostMapping
    public ResponseEntity<WorkspaceResponse> createWorkspace(
            @Valid @RequestBody WorkspaceRequest request,
            Principal principal) {
        return ResponseEntity.ok(
                workspaceService.createWorkspace(principal.getName(), request));
    }

    @GetMapping
    public ResponseEntity<List<WorkspaceResponse>> getMyWorkspaces(
            Principal principal) {
        return ResponseEntity.ok(
                workspaceService.getMyWorkspaces(principal.getName()));
    }

    @GetMapping("/{workspaceId}")
    public ResponseEntity<WorkspaceResponse> getWorkspace(
            @PathVariable String workspaceId) {
        return ResponseEntity.ok(workspaceService.getWorkspace(workspaceId));
    }

    @PostMapping("/{workspaceId}/members/{userId}")
    public ResponseEntity<Void> addMember(
            @PathVariable String workspaceId,
            @PathVariable String userId,
            Principal principal) {
        workspaceService.addMember(principal.getName(), workspaceId, userId);
        return ResponseEntity.ok().build();
    }


    @GetMapping("/{workspaceId}/members")
    public ResponseEntity<List<UserResponse>> getMembers(
            @PathVariable String workspaceId) {
        return ResponseEntity.ok(workspaceService.getMembers(workspaceId));
    }

    @DeleteMapping("/{workspaceId}/members/{userId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable String workspaceId,
            @PathVariable String userId) {
        workspaceService.removeMember(workspaceId, userId);
        return ResponseEntity.ok().build();
    }
}