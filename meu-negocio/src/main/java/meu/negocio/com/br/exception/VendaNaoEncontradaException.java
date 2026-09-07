package meu.negocio.com.br.exception;

public class VendaNaoEncontradaException extends RuntimeException {

    public VendaNaoEncontradaException(Long id) {
        super("Venda com id " + id + " não encontrada");
    }
}
