package meu.negocio.com.br.exception;

import java.time.LocalDateTime;
import java.util.List;

public record ErroResposta(
    LocalDateTime timestamp,
    int status,
    String error,
    String message,
    String path,
    List<String> detalhes
) {

    public ErroResposta(LocalDateTime timestamp, int status, String error, String message, String path) {
        this(timestamp, status, error, message, path, List.of());
    }
}
