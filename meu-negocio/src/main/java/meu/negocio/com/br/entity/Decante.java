package meu.negocio.com.br.entity;

import java.math.BigDecimal;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Configuração de venda de um {@link Produto} em um tamanho específico (ex: decante de 5 ml).
 * Guarda o preço de venda e os custos de embalagem daquele tamanho; os cálculos de
 * viabilidade (custo por ml, lucro, margem) ficam no serviço de análise, não aqui.
 */
@Getter
@Setter
@Entity
@Table(
    name = "decantes",
    indexes = {
        @Index(name = "idx_decantes_produto_id", columnList = "produto_id"),
        @Index(name = "idx_decantes_user_id", columnList = "user_id")
    }
)
public class Decante extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long produtoId;
    private BigDecimal volumeMl;
    private BigDecimal precoVenda;
    private BigDecimal custoEmbalagem;
    private BigDecimal custoSeringa;
    private BigDecimal custoEtiqueta;
    private int userId = 1;
    private boolean ativo;

    public Decante(Long produtoId, BigDecimal volumeMl, BigDecimal precoVenda,
            BigDecimal custoEmbalagem, BigDecimal custoSeringa, BigDecimal custoEtiqueta) {
        this.produtoId = exigirProdutoValido(produtoId);
        this.volumeMl = exigirVolumeValido(volumeMl);
        this.precoVenda = exigirValorNaoNegativo(precoVenda, "Preço de venda");
        this.custoEmbalagem = exigirValorNaoNegativo(custoEmbalagem, "Custo de embalagem");
        this.custoSeringa = exigirValorNaoNegativo(custoSeringa, "Custo de seringa");
        this.custoEtiqueta = exigirValorNaoNegativo(custoEtiqueta, "Custo de etiqueta");
        this.ativo = true;
    }

    public Decante() {
        // Construtor padrão necessário para JPA
    }

    public void ativar() {
        this.ativo = true;
    }

    public void desativar() {
        this.ativo = false;
    }

    public void atualizarPrecoVenda(BigDecimal novoPreco) {
        this.precoVenda = exigirValorNaoNegativo(novoPreco, "Preço de venda");
    }

    /**
     * Soma dos custos de embalagem do decante (frasco/embalagem + seringa + etiqueta).
     * Não inclui o custo do perfume, que depende do {@link Produto} e é calculado na análise.
     */
    public BigDecimal custoEmbalagemTotal() {
        return custoEmbalagem.add(custoSeringa).add(custoEtiqueta);
    }

    private static Long exigirProdutoValido(Long produtoId) {
        if (produtoId == null) {
            throw new IllegalArgumentException("Produto do decante é obrigatório");
        }
        return produtoId;
    }

    private static BigDecimal exigirVolumeValido(BigDecimal volumeMl) {
        if (volumeMl == null || volumeMl.signum() <= 0) {
            throw new IllegalArgumentException("Volume do decante deve ser maior que zero");
        }
        return volumeMl;
    }

    private static BigDecimal exigirValorNaoNegativo(BigDecimal valor, String campo) {
        if (valor == null || valor.signum() < 0) {
            throw new IllegalArgumentException(campo + " não pode ser negativo");
        }
        return valor;
    }
}
