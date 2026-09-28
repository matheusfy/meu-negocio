package meu.negocio.com.br.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import meu.negocio.com.br.dto.Resultado;
import meu.negocio.com.br.service.ResultadoService;

@RestController
@RequestMapping("/api/v1")
public class ResultadoController {

    private final ResultadoService resultadoService;

    public ResultadoController(ResultadoService resultadoService) {
        this.resultadoService = resultadoService;
    }

    /** Faturamento e lucro até o momento (ou no período {@code de..ate}). */
    @GetMapping("/resultado")
    public ResponseEntity<Resultado> getResultado(
        @RequestParam(value = "de", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
        @RequestParam(value = "ate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate
    ) {
        return ResponseEntity.ok(resultadoService.calcular(de, ate));
    }

}
