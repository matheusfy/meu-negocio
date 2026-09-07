package meu.negocio.com.br.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import meu.negocio.com.br.dto.Resultado;
import meu.negocio.com.br.dto.Resultado.ResultadoPorPerfume;
import meu.negocio.com.br.entity.Produto;
import meu.negocio.com.br.entity.Venda;
import meu.negocio.com.br.repository.ProdutoRepository;
import meu.negocio.com.br.repository.VendaRepository;

/**
 * Faturamento e lucro até o momento, a partir dos snapshots congelados em cada {@link Venda}
 * (ver {@code planejamento_vendas_e_estoque.md}, §3.5). Não recalcula nada da venda.
 */
@Service
public class ResultadoService {

    private static final int ESCALA_DINHEIRO = 2;
    private static final int ESCALA_PERCENTUAL = 2;
    private static final BigDecimal CEM = new BigDecimal("100");

    private final VendaRepository vendaRepository;
    private final ProdutoRepository produtoRepository;

    public ResultadoService(VendaRepository vendaRepository, ProdutoRepository produtoRepository) {
        this.vendaRepository = vendaRepository;
        this.produtoRepository = produtoRepository;
    }

    public Resultado calcular(LocalDate de, LocalDate ate) {
        List<Venda> vendas = (de != null && ate != null)
            ? vendaRepository.findByAtivoTrueAndDataVendaBetween(de, ate)
            : vendaRepository.findByAtivoTrue();
        Map<Long, String> nomes = new LinkedHashMap<>();
        for (Produto p : produtoRepository.findAll()) {
            nomes.put(p.getId(), p.getNome() == null ? "" : p.getNome());
        }
        return calcular(vendas, nomes, de, ate);
    }

    /** Agregação pura. Testável sem banco: recebe as vendas e o mapa de nomes prontos. */
    public Resultado calcular(List<Venda> vendas, Map<Long, String> nomes, LocalDate de, LocalDate ate) {
        BigDecimal faturamento = BigDecimal.ZERO;
        BigDecimal custo = BigDecimal.ZERO;
        Map<Long, Acumulador> porProduto = new LinkedHashMap<>();

        for (Venda venda : vendas) {
            BigDecimal receita = nvl(venda.getReceitaTotal());
            BigDecimal custoVenda = nvl(venda.getCustoUnitario()).multiply(BigDecimal.valueOf(venda.getQuantidade()));
            BigDecimal lucro = nvl(venda.getLucroTotal());
            faturamento = faturamento.add(receita);
            custo = custo.add(custoVenda);
            porProduto.computeIfAbsent(venda.getProdutoId(), k -> new Acumulador())
                .somar(receita, lucro);
        }

        faturamento = dinheiro(faturamento);
        custo = dinheiro(custo);
        BigDecimal lucroBruto = dinheiro(faturamento.subtract(custo));
        int numVendas = vendas.size();
        BigDecimal ticketMedio = numVendas == 0
            ? BigDecimal.ZERO.setScale(ESCALA_DINHEIRO)
            : faturamento.divide(BigDecimal.valueOf(numVendas), ESCALA_DINHEIRO, RoundingMode.HALF_UP);
        BigDecimal margemMedia = percentual(lucroBruto, faturamento);

        List<ResultadoPorPerfume> porPerfume = new ArrayList<>();
        for (Map.Entry<Long, Acumulador> entrada : porProduto.entrySet()) {
            Acumulador acc = entrada.getValue();
            porPerfume.add(new ResultadoPorPerfume(
                entrada.getKey(),
                nomes.getOrDefault(entrada.getKey(), ""),
                dinheiro(acc.faturamento),
                dinheiro(acc.lucro),
                acc.vendas
            ));
        }
        porPerfume.sort(Comparator.comparing(ResultadoPorPerfume::lucro).reversed());

        return new Resultado(de, ate, faturamento, custo, lucroBruto, margemMedia, numVendas, ticketMedio, porPerfume);
    }

    private static final class Acumulador {
        private BigDecimal faturamento = BigDecimal.ZERO;
        private BigDecimal lucro = BigDecimal.ZERO;
        private int vendas;

        private void somar(BigDecimal receita, BigDecimal lucroVenda) {
            this.faturamento = this.faturamento.add(receita);
            this.lucro = this.lucro.add(lucroVenda);
            this.vendas += 1;
        }
    }

    private static BigDecimal percentual(BigDecimal parte, BigDecimal total) {
        if (total.signum() == 0) {
            return BigDecimal.ZERO.setScale(ESCALA_PERCENTUAL);
        }
        return parte.multiply(CEM).divide(total, ESCALA_PERCENTUAL, RoundingMode.HALF_UP);
    }

    private static BigDecimal dinheiro(BigDecimal valor) {
        return valor.setScale(ESCALA_DINHEIRO, RoundingMode.HALF_UP);
    }

    private static BigDecimal nvl(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }
}
