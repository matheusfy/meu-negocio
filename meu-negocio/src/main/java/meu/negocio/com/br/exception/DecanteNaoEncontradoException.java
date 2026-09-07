package meu.negocio.com.br.exception;

public class DecanteNaoEncontradoException extends RuntimeException {

    public DecanteNaoEncontradoException(Long id) {
        super("Decante com id " + id + " não encontrado");
    }
}
