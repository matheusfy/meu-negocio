package meu.negocio.com.br.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import meu.negocio.com.br.dto.AnaliseProduto;
import meu.negocio.com.br.dto.AnaliseViabilidade;
import meu.negocio.com.br.dto.ComparacaoDecantes;
import meu.negocio.com.br.service.AnaliseViabilidadeService;
import meu.negocio.com.br.service.RecomendacaoService;

@RestController
@RequestMapping("/api/v1")
public class AnaliseViabilidadeController {

    private final AnaliseViabilidadeService analiseViabilidadeService;
    private final RecomendacaoService recomendacaoService;

    public AnaliseViabilidadeController(AnaliseViabilidadeService analiseViabilidadeService,
            RecomendacaoService recomendacaoService) {
        this.analiseViabilidadeService = analiseViabilidadeService;
        this.recomendacaoService = recomendacaoService;
    }

    @GetMapping("/decantes/{id}/viabilidade")
    public ResponseEntity<AnaliseViabilidade> analisarDecante(@PathVariable("id") Long id) {
        return ResponseEntity.ok(analiseViabilidadeService.analisarDecante(id));
    }

    @GetMapping("/produtos/{id}/viabilidade-decantes")
    public ResponseEntity<ComparacaoDecantes> compararDecantesDoProduto(@PathVariable("id") Long id) {
        return ResponseEntity.ok(analiseViabilidadeService.compararProduto(id));
    }

    /** Comparação entre os tamanhos + conselhos já traduzidos para linguagem simples. */
    @GetMapping("/produtos/{id}/analise")
    public ResponseEntity<AnaliseProduto> analisarProduto(@PathVariable("id") Long id) {
        ComparacaoDecantes comparacao = analiseViabilidadeService.compararProduto(id);
        return ResponseEntity.ok(new AnaliseProduto(
            id, comparacao, recomendacaoService.recomendar(comparacao)));
    }

}
