package com.blueWave.BlueWave.repository;

import com.blueWave.BlueWave.model.Admin;
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
public interface AdminRepository extends JpaRepository<Admin, Long> {

    // Buscar admin por email
    Optional<Admin> findByEmail(String email);

    // Verificar se existe admin com email
    boolean existsByEmail(String email);

    // Buscar admins ativos
    List<Admin> findByAtivoTrue();

    // Buscar admins por nível de acesso
    List<Admin> findByNivelAcesso(Admin.NivelAcesso nivelAcesso);

    // Buscar admins ativos por nível de acesso
    @Query("SELECT a FROM Admin a WHERE a.ativo = true AND a.nivelAcesso = :nivel")
    List<Admin> findActiveAdminsByNivel(@Param("nivel") Admin.NivelAcesso nivel);

    // Buscar admins bloqueados
    @Query("SELECT a FROM Admin a WHERE a.bloqueadoAte IS NOT NULL AND a.bloqueadoAte > :agora")
    List<Admin> findAdminsBloqueados(@Param("agora") LocalDateTime agora);

    // Buscar admins que nunca logaram
    @Query("SELECT a FROM Admin a WHERE a.ultimoLogin IS NULL")
    List<Admin> findAdminsNuncaLogaram();

    // Buscar admins com login recente
    @Query("SELECT a FROM Admin a WHERE a.ultimoLogin > :data")
    List<Admin> findAdminsComLoginRecente(@Param("data") LocalDateTime data);

    // Contar admins ativos
    long countByAtivoTrue();

    // Contar admins por nível
    long countByNivelAcesso(Admin.NivelAcesso nivelAcesso);

    // Buscar admin para autenticação (ativo e não bloqueado)
    @Query("SELECT a FROM Admin a WHERE a.email = :email AND a.ativo = true AND (a.bloqueadoAte IS NULL OR a.bloqueadoAte <= :agora)")
    Optional<Admin> findByEmailForAuth(@Param("email") String email, @Param("agora") LocalDateTime agora);

    // Limpar bloqueios expirados
    @Modifying
    @Transactional
    @Query("UPDATE Admin a SET a.bloqueadoAte = NULL WHERE a.bloqueadoAte IS NOT NULL AND a.bloqueadoAte <= :agora")
    void limparBloqueiosExpirados(@Param("agora") LocalDateTime agora);
}