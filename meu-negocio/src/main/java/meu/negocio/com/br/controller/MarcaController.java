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

import meu.negocio.com.br.dto.MarcaDTO;
import meu.negocio.com.br.entity.Marca;
import meu.negocio.com.br.service.MarcaService;

@RestController
@RequestMapping("/api/v1/marcas")
public class MarcaController {

    private final MarcaService marcaService;

    public MarcaController(MarcaService marcaService) {
        this.marcaService = marcaService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<MarcaDTO> getMarca(@PathVariable("id") Long id) {
        Marca marca = marcaService.findById(id);
        return ResponseEntity.ok(new MarcaDTO(marca));
    }

    @GetMapping
    public ResponseEntity<Page<MarcaDTO>> getAllMarcas(Pageable pageable) {
        Page<MarcaDTO> marcas = marcaService.findAll(pageable).map(MarcaDTO::new);
        return ResponseEntity.ok(marcas);
    }

    @PostMapping
    public ResponseEntity<MarcaDTO> createMarca(@RequestBody @Validated MarcaDTO marcaDTO) {
        Marca marca = marcaService.save(marcaDTO.toEntity());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(marca.getId())
            .toUri();
        return ResponseEntity.created(location).body(new MarcaDTO(marca));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MarcaDTO> updateMarca(
        @PathVariable("id") Long id,
        @RequestBody @Validated MarcaDTO marcaDTO
    ) {
        Marca marca = marcaService.update(id, marcaDTO.toEntity());
        return ResponseEntity.ok(new MarcaDTO(marca));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMarca(@PathVariable("id") Long id) {
        marcaService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

}
