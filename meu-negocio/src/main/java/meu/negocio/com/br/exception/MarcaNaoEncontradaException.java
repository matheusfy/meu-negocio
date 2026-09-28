package meu.negocio.com.br.exception;

public class MarcaNaoEncontradaException extends RuntimeException {

    public MarcaNaoEncontradaException(Long id) {
        super("Marca com id " + id + " não encontrada");
    }
}
