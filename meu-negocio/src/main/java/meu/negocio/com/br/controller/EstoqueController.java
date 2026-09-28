package meu.negocio.com.br.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import meu.negocio.com.br.dto.EstoqueProduto;
import meu.negocio.com.br.service.EstoqueService;

@RestController
@RequestMapping("/api/v1")
public class EstoqueController {

    private final EstoqueService estoqueService;

    public EstoqueController(EstoqueService estoqueService) {
        this.estoqueService = estoqueService;
    }

    /** Foto do estoque de um perfume: frascos lacrados/abertos, custo médio e valor parado. */
    @GetMapping("/produtos/{id}/estoque")
    public ResponseEntity<EstoqueProduto> getEstoqueDoProduto(@PathVariable("id") Long id) {
        return ResponseEntity.ok(estoqueService.calcular(id));
    }

}
