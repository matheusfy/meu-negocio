package meu.negocio.com.br.exception;

public class FrascoAbertoNaoEncontradoException extends RuntimeException {

    public FrascoAbertoNaoEncontradoException(Long id) {
        super("Frasco aberto com id " + id + " não encontrado");
    }
}
