package meu.negocio.com.br.exception;

public class CustoInsumoNaoEncontradoException extends RuntimeException {

    public CustoInsumoNaoEncontradoException(Long id) {
        super("Insumo com id " + id + " não encontrado");
    }
}
