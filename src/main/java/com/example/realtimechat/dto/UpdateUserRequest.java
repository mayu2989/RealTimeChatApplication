package com.example.realtimechat.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserRequest {

    @Size(min = 2, max = 50)
    private String username;

    @Email
    private String email;

    @Size(min = 2, max = 100)
    private String displayName;

    private String phone;

    @Size(min = 6, max = 100)
    private String newPassword;
}
