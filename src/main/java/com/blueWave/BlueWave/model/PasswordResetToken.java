package com.blueWave.BlueWave.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String token;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(name = "user_type", nullable = false, length = 20)
    private String userType; // "voluntario" ou "ong"

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expiration_time", nullable = false)
    private LocalDateTime expirationTime;

    @Column(nullable = false)
    private Boolean used = false;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (expirationTime == null) {
            // Token válido por 1 hora
            expirationTime = LocalDateTime.now().plusHours(1);
        }
    }

    // Construtor personalizado
    public PasswordResetToken(String token, String email, String userType) {
        this.token = token;
        this.email = email;
        this.userType = userType;
        this.createdAt = LocalDateTime.now();
        this.expirationTime = LocalDateTime.now().plusHours(1);
        this.used = false;
    }

    // Método para verificar se o token está expirado
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expirationTime);
    }

    // Método para verificar se o token é válido (não usado e não expirado)
    public boolean isValid() {
        return !used && !isExpired();
    }

    // Método para marcar como usado
    public void markAsUsed() {
        this.used = true;
        this.usedAt = LocalDateTime.now();
    }
}