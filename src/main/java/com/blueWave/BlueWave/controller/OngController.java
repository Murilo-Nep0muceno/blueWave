package com.blueWave.BlueWave.controller;

import com.blueWave.BlueWave.model.Ong;
import com.blueWave.BlueWave.model.Vagas;
import com.blueWave.BlueWave.model.Voluntario;
import com.blueWave.BlueWave.repository.OngRepository;
import com.blueWave.BlueWave.repository.VagasRepository;
import com.blueWave.BlueWave.repository.VoluntarioRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import jakarta.validation.Valid;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/ongForm")
@Validated
public class OngController {

    @Autowired
    private OngRepository ongRepository;

    @Autowired
    private VagasRepository vagasRepository;

    @Autowired
    private VoluntarioRepository voluntarioRepository;

    @GetMapping
    public ModelAndView form(){
        ModelAndView mv = new ModelAndView("ongForm");
        return mv;
    }

    @PostMapping
    public ResponseEntity<?> cadastrarOng(@Valid @RequestBody Ong ong, BindingResult bindingResult){
        try {
            // Verificar se há erros de validação
            if (bindingResult.hasErrors()) {
                Map<String, Object> errorResponse = new HashMap<>();
                Map<String, String> fieldErrors = new HashMap<>();

                for (FieldError error : bindingResult.getFieldErrors()) {
                    fieldErrors.put(error.getField(), error.getDefaultMessage());
                }

                errorResponse.put("message", "Dados inválidos");
                errorResponse.put("errors", fieldErrors);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            // Verificar se CNPJ já existe
            if (ongRepository.existsByCnpj(ong.getCnpj())) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "CNPJ já cadastrado no sistema");
                error.put("field", "cnpj");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
            }

            // Verificar se email já existe
            if (ongRepository.existsByEmail(ong.getEmail())) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Email já cadastrado no sistema");
                error.put("field", "email");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
            }

            // Verificar se telefone já existe
            if (ongRepository.existsByTelefone(ong.getTelefone())) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Telefone já cadastrado no sistema");
                error.put("field", "telefone");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
            }

            // Criptografar senha
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            String senhaCriptografada = encoder.encode(ong.getSenha());
            ong.setSenha(senhaCriptografada);

            // Salvar ONG
            Ong ongSalva = ongRepository.save(ong);

            // Remover senha da resposta por segurança
            ongSalva.setSenha(null);

            Map<String, Object> successResponse = new HashMap<>();
            successResponse.put("message", "ONG cadastrada com sucesso!");
            successResponse.put("ong", ongSalva);

            return ResponseEntity.status(HttpStatus.CREATED).body(successResponse);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Erro interno do servidor. Tente novamente mais tarde.");
            error.put("details", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }


    // Método para verificar se é voluntário logado
    private boolean isVoluntarioLoggedIn(HttpSession session) {
        String userType = (String) session.getAttribute("userType");
        String userEmail = (String) session.getAttribute("userEmail");
        return "voluntario".equals(userType) && userEmail != null;
    }

    // Método para obter voluntário logado
    private Voluntario getLoggedVoluntario(HttpSession session) {
        if (!isVoluntarioLoggedIn(session)) {
            return null;
        }
        String userEmail = (String) session.getAttribute("userEmail");
        return voluntarioRepository.findByEmail(userEmail);
    }

    @GetMapping("/perfil/{ongId}")
    @Transactional(readOnly = true)
    public ModelAndView verPerfilOng(@PathVariable Long ongId, HttpSession session) {
        // Verificar se é voluntário logado
        if (!isVoluntarioLoggedIn(session)) {
            return new ModelAndView("redirect:/login/voluntario");
        }

        Voluntario voluntario = getLoggedVoluntario(session);
        if (voluntario == null) {
            return new ModelAndView("redirect:/login/voluntario");
        }

        try {
            // Buscar a ONG
            Ong ong = ongRepository.findById(ongId).orElse(null);
            if (ong == null) {
                ModelAndView mv = new ModelAndView("redirect:/inscricao/minhasVagas");
                return mv;
            }

            // Buscar vagas ativas da ONG
            List<Vagas> vagasAtivas = vagasRepository.findByOngId(ongId)
                    .stream()
                    .filter(vaga ->
                            vaga.getStatus() == Vagas.StatusVaga.ATIVA &&
                                    (vaga.getData().isAfter(LocalDate.now()) || vaga.getData().equals(LocalDate.now()))
                    )
                    .toList();

            // Contar total de vagas da ONG (incluindo encerradas)
            List<Vagas> todasVagas = vagasRepository.findByOngId(ongId);
            long totalVagas = todasVagas.size();
            long vagasEncerradas = todasVagas.stream()
                    .filter(vaga ->
                            vaga.getData().isBefore(LocalDate.now()) ||
                                    vaga.getStatus() != Vagas.StatusVaga.ATIVA
                    )
                    .count();

            ModelAndView mv = new ModelAndView("perfilOng");
            mv.addObject("nomeVoluntario", voluntario.getNomeVoluntario());
            mv.addObject("ong", ong);
            mv.addObject("vagasAtivas", vagasAtivas);
            mv.addObject("totalVagas", totalVagas);
            mv.addObject("totalVagasAtivas", vagasAtivas.size());
            mv.addObject("totalVagasEncerradas", vagasEncerradas);

            return mv;
        } catch (Exception e) {
            e.printStackTrace();
            return new ModelAndView("redirect:/inscricao/minhasVagas");
        }
    }
}