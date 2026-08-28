package meu.negocio.com.br.exception;

public class ProdutoNaoEncontradoException extends RuntimeException {

    public ProdutoNaoEncontradoException(Long id) {
        super("Produto com id " + id + " não encontrado");
    }
}
