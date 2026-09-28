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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import meu.negocio.com.br.dto.LoteCompraDTO;
import meu.negocio.com.br.entity.LoteCompra;
import meu.negocio.com.br.service.LoteCompraService;

@RestController
@RequestMapping("/api/v1/lotes")
public class LoteCompraController {

    private final LoteCompraService loteCompraService;

    public LoteCompraController(LoteCompraService loteCompraService) {
        this.loteCompraService = loteCompraService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<LoteCompraDTO> getLote(@PathVariable("id") Long id) {
        LoteCompra lote = loteCompraService.findById(id);
        return ResponseEntity.ok(new LoteCompraDTO(lote));
    }

    @GetMapping
    public ResponseEntity<Page<LoteCompraDTO>> getAllLotes(
        @RequestParam(value = "produtoId", required = false) Long produtoId,
        Pageable pageable
    ) {
        Page<LoteCompraDTO> lotes = loteCompraService.findAll(produtoId, pageable).map(LoteCompraDTO::new);
        return ResponseEntity.ok(lotes);
    }

    @PostMapping
    public ResponseEntity<LoteCompraDTO> createLote(@RequestBody @Validated LoteCompraDTO loteDTO) {
        LoteCompra lote = loteCompraService.save(loteDTO.toEntity());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(lote.getId())
            .toUri();
        return ResponseEntity.created(location).body(new LoteCompraDTO(lote));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LoteCompraDTO> updateLote(
        @PathVariable("id") Long id,
        @RequestBody @Validated LoteCompraDTO loteDTO
    ) {
        LoteCompra lote = loteCompraService.update(id, loteDTO.toEntity());
        return ResponseEntity.ok(new LoteCompraDTO(lote));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLote(@PathVariable("id") Long id) {
        loteCompraService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

}
