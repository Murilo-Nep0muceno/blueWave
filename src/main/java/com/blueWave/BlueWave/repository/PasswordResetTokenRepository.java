package com.blueWave.BlueWave.repository;

import com.blueWave.BlueWave.model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    // Buscar token válido (não usado e não expirado) - Método usado no controller
    PasswordResetToken findByTokenAndUsedFalse(String token);

    // Buscar token válido com verificação de expiração
    @Query("SELECT p FROM PasswordResetToken p WHERE p.token = :token AND p.used = false AND p.expirationTime > :now")
    Optional<PasswordResetToken> findValidToken(@Param("token") String token, @Param("now") LocalDateTime now);

    // Buscar token por email
    List<PasswordResetToken> findByEmail(String email);

    // Buscar tokens por email e que não foram usados
    List<PasswordResetToken> findByEmailAndUsedFalse(String email);

    // Deletar tokens por email (para limpar tokens antigos)
    @Modifying
    @Transactional
    void deleteByEmail(String email);

    // Deletar tokens expirados (para limpeza automática)
    @Modifying
    @Transactional
    @Query("DELETE FROM PasswordResetToken p WHERE p.expirationTime < :now")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);

    // Buscar tokens expirados
    List<PasswordResetToken> findByExpirationTimeBefore(LocalDateTime dateTime);

    // Verificar se existe token válido para um email
    @Query("SELECT COUNT(p) > 0 FROM PasswordResetToken p WHERE p.email = :email AND p.used = false AND p.expirationTime > :now")
    boolean existsValidTokenForEmail(@Param("email") String email, @Param("now") LocalDateTime now);

    // Buscar token mais recente por email
    @Query("SELECT p FROM PasswordResetToken p WHERE p.email = :email ORDER BY p.createdAt DESC")
    List<PasswordResetToken> findByEmailOrderByCreatedAtDesc(@Param("email") String email);

    // Marcar todos os tokens de um email como usados
    @Modifying
    @Transactional
    @Query("UPDATE PasswordResetToken p SET p.used = true, p.usedAt = :now WHERE p.email = :email AND p.used = false")
    void markAllTokensAsUsedForEmail(@Param("email") String email, @Param("now") LocalDateTime now);
}