package meu.negocio.com.br.exception;

public class LoteCompraNaoEncontradoException extends RuntimeException {

    public LoteCompraNaoEncontradoException(Long id) {
        super("Lote de compra com id " + id + " não encontrado");
    }
}
