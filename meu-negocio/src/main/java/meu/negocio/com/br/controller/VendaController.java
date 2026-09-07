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

import meu.negocio.com.br.dto.VendaDTO;
import meu.negocio.com.br.entity.Venda;
import meu.negocio.com.br.service.VendaService;

@RestController
@RequestMapping("/api/v1/vendas")
public class VendaController {

    private final VendaService vendaService;

    public VendaController(VendaService vendaService) {
        this.vendaService = vendaService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<VendaDTO> getVenda(@PathVariable("id") Long id) {
        Venda venda = vendaService.findById(id);
        return ResponseEntity.ok(new VendaDTO(venda));
    }

    @GetMapping
    public ResponseEntity<Page<VendaDTO>> getAllVendas(
        @RequestParam(value = "produtoId", required = false) Long produtoId,
        @RequestParam(value = "tipo", required = false) String tipo,
        Pageable pageable
    ) {
        Page<VendaDTO> vendas = vendaService.findAll(produtoId, tipo, pageable).map(VendaDTO::new);
        return ResponseEntity.ok(vendas);
    }

    @PostMapping
    public ResponseEntity<VendaDTO> createVenda(@RequestBody @Validated VendaDTO vendaDTO) {
        Venda venda = vendaService.registrar(vendaDTO.toEntity());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(venda.getId())
            .toUri();
        return ResponseEntity.created(location).body(new VendaDTO(venda));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VendaDTO> updateVenda(
        @PathVariable("id") Long id,
        @RequestBody @Validated VendaDTO vendaDTO
    ) {
        Venda venda = vendaService.update(id, vendaDTO.toEntity());
        return ResponseEntity.ok(new VendaDTO(venda));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVenda(@PathVariable("id") Long id) {
        vendaService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

}
