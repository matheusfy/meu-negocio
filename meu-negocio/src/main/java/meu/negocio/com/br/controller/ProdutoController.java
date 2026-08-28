package meu.negocio.com.br.controller;

import java.net.URI;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import meu.negocio.com.br.dto.ProdutoDTO;
import meu.negocio.com.br.entity.Produto;
import meu.negocio.com.br.service.ProdutoService;

@RestController
@RequestMapping("/api/v1/produtos")
public class ProdutoController {

    private final ProdutoService produtoService;

    public ProdutoController(ProdutoService produtoService) {
        this.produtoService = produtoService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProdutoDTO> getProduto(@PathVariable("id") Long id) {
        Produto produto = produtoService.findById(id);
        return ResponseEntity.ok(new ProdutoDTO(produto));
    }

    @GetMapping
    public ResponseEntity<Page<ProdutoDTO>> getAllProdutos(Pageable pageable) {
        Page<ProdutoDTO> produtos = produtoService.findAll(pageable).map(ProdutoDTO::new);
        return ResponseEntity.ok(produtos);
    }

    @PostMapping
    public ResponseEntity<ProdutoDTO> createProduto(@RequestBody @Validated ProdutoDTO produtoDTO) {
        Produto produto = produtoService.save(produtoDTO.toEntity());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(produto.getId())
            .toUri();
        return ResponseEntity.created(location).body(new ProdutoDTO(produto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProdutoDTO> updateProduto(@PathVariable("id") Long id, @RequestBody @Validated ProdutoDTO produtoDTO) {
        Produto produto = produtoService.update(id, produtoDTO.toEntity());
        return ResponseEntity.ok(new ProdutoDTO(produto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduto(@PathVariable("id") Long id) {
        produtoService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

}
