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

import meu.negocio.com.br.dto.CustoInsumoDTO;
import meu.negocio.com.br.entity.CustoInsumo;
import meu.negocio.com.br.service.CustoInsumoService;

@RestController
@RequestMapping("/api/v1/insumos")
public class CustoInsumoController {

    private final CustoInsumoService custoInsumoService;

    public CustoInsumoController(CustoInsumoService custoInsumoService) {
        this.custoInsumoService = custoInsumoService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustoInsumoDTO> getInsumo(@PathVariable("id") Long id) {
        CustoInsumo custoInsumo = custoInsumoService.findById(id);
        return ResponseEntity.ok(new CustoInsumoDTO(custoInsumo));
    }

    @GetMapping
    public ResponseEntity<Page<CustoInsumoDTO>> getAllInsumos(Pageable pageable) {
        Page<CustoInsumoDTO> insumos = custoInsumoService.findAll(pageable).map(CustoInsumoDTO::new);
        return ResponseEntity.ok(insumos);
    }

    @PostMapping
    public ResponseEntity<CustoInsumoDTO> createInsumo(@RequestBody @Validated CustoInsumoDTO custoInsumoDTO) {
        CustoInsumo custoInsumo = custoInsumoService.save(custoInsumoDTO.toEntity());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(custoInsumo.getId())
            .toUri();
        return ResponseEntity.created(location).body(new CustoInsumoDTO(custoInsumo));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustoInsumoDTO> updateInsumo(
        @PathVariable("id") Long id,
        @RequestBody @Validated CustoInsumoDTO custoInsumoDTO
    ) {
        CustoInsumo custoInsumo = custoInsumoService.update(id, custoInsumoDTO.toEntity());
        return ResponseEntity.ok(new CustoInsumoDTO(custoInsumo));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInsumo(@PathVariable("id") Long id) {
        custoInsumoService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

}
