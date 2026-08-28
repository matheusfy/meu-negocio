package meu.negocio.com.br.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import meu.negocio.com.br.dto.AnaliseViabilidade;
import meu.negocio.com.br.dto.ComparacaoDecantes;
import meu.negocio.com.br.service.AnaliseViabilidadeService;

@RestController
@RequestMapping("/api/v1")
public class AnaliseViabilidadeController {

    private final AnaliseViabilidadeService analiseViabilidadeService;

    public AnaliseViabilidadeController(AnaliseViabilidadeService analiseViabilidadeService) {
        this.analiseViabilidadeService = analiseViabilidadeService;
    }

    @GetMapping("/decantes/{id}/viabilidade")
    public ResponseEntity<AnaliseViabilidade> analisarDecante(@PathVariable("id") Long id) {
        return ResponseEntity.ok(analiseViabilidadeService.analisarDecante(id));
    }

    @GetMapping("/produtos/{id}/viabilidade-decantes")
    public ResponseEntity<ComparacaoDecantes> compararDecantesDoProduto(@PathVariable("id") Long id) {
        return ResponseEntity.ok(analiseViabilidadeService.compararProduto(id));
    }

}
