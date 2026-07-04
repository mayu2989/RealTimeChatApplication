package com.example.realtimechat.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChannelRequest {

    @NotBlank(message = "Channel name is required")
    private String name;

    private String description;

    @Builder.Default
    private Boolean isPrivate = false;
}