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

import meu.negocio.com.br.dto.DecanteDTO;
import meu.negocio.com.br.entity.Decante;
import meu.negocio.com.br.service.DecanteService;

@RestController
@RequestMapping("/api/v1/decantes")
public class DecanteController {

    private final DecanteService decanteService;

    public DecanteController(DecanteService decanteService) {
        this.decanteService = decanteService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<DecanteDTO> getDecante(@PathVariable("id") Long id) {
        Decante decante = decanteService.findById(id);
        return ResponseEntity.ok(new DecanteDTO(decante));
    }

    @GetMapping
    public ResponseEntity<Page<DecanteDTO>> getAllDecantes(
        @RequestParam(value = "produtoId", required = false) Long produtoId,
        Pageable pageable
    ) {
        Page<DecanteDTO> decantes = decanteService.findAll(produtoId, pageable).map(DecanteDTO::new);
        return ResponseEntity.ok(decantes);
    }

    @PostMapping
    public ResponseEntity<DecanteDTO> createDecante(@RequestBody @Validated DecanteDTO decanteDTO) {
        Decante decante = decanteService.save(decanteDTO.toEntity());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(decante.getId())
            .toUri();
        return ResponseEntity.created(location).body(new DecanteDTO(decante));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DecanteDTO> updateDecante(
        @PathVariable("id") Long id,
        @RequestBody @Validated DecanteDTO decanteDTO
    ) {
        Decante decante = decanteService.update(id, decanteDTO.toEntity());
        return ResponseEntity.ok(new DecanteDTO(decante));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDecante(@PathVariable("id") Long id) {
        decanteService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

}
