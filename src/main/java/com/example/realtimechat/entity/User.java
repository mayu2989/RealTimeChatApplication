package com.example.realtimechat.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false,unique = true)
    private String email;

    @Column(nullable = false)
    private String password;


    private String displayName;

    private String avatarUrl;

    @Builder.Default
    private Boolean isOnline = false;

    private LocalDateTime lastSeen;

    private LocalDateTime createdAt;

    @Column(unique = true)
    private String phone;

    @PrePersist
    public void prePersist(){
        this.createdAt = LocalDateTime.now();
        this.isOnline = false;
    }
}
