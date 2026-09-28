package meu.negocio.com.br.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import meu.negocio.com.br.dto.Sessao;

@RestController
@RequestMapping("/api/v1/sessao")
public class SessaoController {

    @GetMapping
    public ResponseEntity<Sessao> getSessao(Authentication authentication) {
        return ResponseEntity.ok(new Sessao(authentication.getName()));
    }
}
