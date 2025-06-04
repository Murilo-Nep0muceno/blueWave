package com.blueWave.BlueWave.controller;

import com.blueWave.BlueWave.model.Voluntario;
import com.blueWave.BlueWave.repository.VoluntarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/voluntarioForm")
@Validated
@CrossOrigin(origins = "*")
public class VoluntarioController {

    @Autowired
    private VoluntarioRepository voluntarioRepository;

    @GetMapping
    public ModelAndView form(){
        ModelAndView mv = new ModelAndView("voluntarioForm");
        return mv;
    }

    @PostMapping
    public ResponseEntity<?> cadastrarVoluntario(@Valid @RequestBody Voluntario voluntario, BindingResult bindingResult) {
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

            // Verificar se email já existe
            if (voluntarioRepository.existsByEmail(voluntario.getEmail())) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Email já cadastrado no sistema");
                error.put("field", "email");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
            }

            // Verificar se CPF já existe
            if (voluntarioRepository.existsByCpf(voluntario.getCpf())) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "CPF já cadastrado no sistema");
                error.put("field", "cpf");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
            }

            // Verificar se telefone já existe
            if (voluntarioRepository.existsByTelefone(voluntario.getTelefone())) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Telefone já cadastrado no sistema");
                error.put("field", "telefone");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
            }

            // Criptografar senha
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            String senhaCriptografada = encoder.encode(voluntario.getSenha());
            voluntario.setSenha(senhaCriptografada);

            // Salvar Voluntário
            Voluntario voluntarioSalvo = voluntarioRepository.save(voluntario);

            // Remover senha da resposta por segurança
            voluntarioSalvo.setSenha(null);

            Map<String, Object> successResponse = new HashMap<>();
            successResponse.put("message", "Voluntário cadastrado com sucesso!");
            successResponse.put("voluntario", voluntarioSalvo);

            return ResponseEntity.status(HttpStatus.CREATED).body(successResponse);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Erro interno do servidor. Tente novamente mais tarde.");
            error.put("details", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

}