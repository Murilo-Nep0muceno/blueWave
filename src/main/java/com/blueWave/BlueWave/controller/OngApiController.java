package com.blueWave.BlueWave.controller;

import com.blueWave.BlueWave.model.Ong;
import com.blueWave.BlueWave.model.Vagas;
import com.blueWave.BlueWave.model.Voluntario;
import com.blueWave.BlueWave.repository.OngRepository;
import com.blueWave.BlueWave.repository.VagasRepository;
import com.blueWave.BlueWave.repository.VoluntarioRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/api/ong")
public class OngApiController {

    @Autowired
    private OngRepository ongRepository;

    @Autowired
    private VagasRepository vagasRepository;

    @Autowired
    private VoluntarioRepository voluntarioRepository;

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
    @ResponseBody
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getOngProfile(@PathVariable Long ongId, HttpSession session) {
        // Verificar se é voluntário logado
        if (!isVoluntarioLoggedIn(session)) {
            return ResponseEntity.status(401).body(Map.of("error", "Não autorizado"));
        }

        try {
            // Buscar a ONG
            Ong ong = ongRepository.findById(ongId).orElse(null);
            if (ong == null) {
                return ResponseEntity.notFound().build();
            }

            // Buscar estatísticas de vagas da ONG
            List<Vagas> todasVagas = vagasRepository.findByOngId(ongId);
            long totalVagas = todasVagas.size();

            long vagasAtivas = todasVagas.stream()
                    .filter(vaga ->
                            vaga.getStatus() == Vagas.StatusVaga.ATIVA &&
                                    (vaga.getData().isAfter(LocalDate.now()) || vaga.getData().equals(LocalDate.now()))
                    )
                    .count();

            // Montar resposta
            Map<String, Object> response = new HashMap<>();
            response.put("id", ong.getId());
            response.put("nome", ong.getNome());
            response.put("cnpj", ong.getCnpj());
            response.put("email", ong.getEmail());
            response.put("telefone", ong.getTelefone());
            response.put("descricao", ong.getDescricao());
            response.put("logoUrl", ong.getLogoUrl());
            response.put("site", ong.getSite());
            response.put("facebook", ong.getFacebook());
            response.put("instagram", ong.getInstagram());

            // Endereço
            response.put("cep", ong.getCep());
            response.put("logradouro", ong.getLogradouro());
            response.put("numero", ong.getNumero());
            response.put("complemento", ong.getComplemento());
            response.put("bairro", ong.getBairro());
            response.put("cidade", ong.getCidade());
            response.put("estado", ong.getEstado());

            // Estatísticas
            response.put("totalVagas", totalVagas);
            response.put("vagasAtivas", vagasAtivas);

            // Datas
            response.put("dataFundacao", ong.getDataFundacao());
            response.put("dataCadastro", ong.getDataCadastro());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Erro interno do servidor"));
        }
    }
}