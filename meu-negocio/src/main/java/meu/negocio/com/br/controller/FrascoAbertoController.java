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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import meu.negocio.com.br.dto.FrascoAbertoDTO;
import meu.negocio.com.br.entity.FrascoAberto;
import meu.negocio.com.br.service.FrascoAbertoService;

@RestController
@RequestMapping("/api/v1/frascos-abertos")
public class FrascoAbertoController {

    private final FrascoAbertoService frascoAbertoService;

    public FrascoAbertoController(FrascoAbertoService frascoAbertoService) {
        this.frascoAbertoService = frascoAbertoService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<FrascoAbertoDTO> getFrasco(@PathVariable("id") Long id) {
        FrascoAberto frasco = frascoAbertoService.findById(id);
        return ResponseEntity.ok(new FrascoAbertoDTO(frasco));
    }

    @GetMapping
    public ResponseEntity<Page<FrascoAbertoDTO>> getAllFrascos(
        @RequestParam(value = "produtoId", required = false) Long produtoId,
        @RequestParam(value = "status", required = false) String status,
        Pageable pageable
    ) {
        Page<FrascoAbertoDTO> frascos = frascoAbertoService.findAll(produtoId, status, pageable)
            .map(FrascoAbertoDTO::new);
        return ResponseEntity.ok(frascos);
    }

    @PostMapping
    public ResponseEntity<FrascoAbertoDTO> abrirFrasco(@RequestBody @Validated FrascoAbertoDTO frascoDTO) {
        FrascoAberto frasco = frascoAbertoService.abrir(
            frascoDTO.produtoId(), frascoDTO.dataAbertura(), frascoDTO.observacao());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(frasco.getId())
            .toUri();
        return ResponseEntity.created(location).body(new FrascoAbertoDTO(frasco));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> descartarFrasco(@PathVariable("id") Long id) {
        frascoAbertoService.descartar(id);
        return ResponseEntity.noContent().build();
    }

}
