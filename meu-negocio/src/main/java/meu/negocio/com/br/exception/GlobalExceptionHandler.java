package meu.negocio.com.br.exception;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResposta> handleTipoInvalido(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String tipoEsperado = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "outro tipo";
        ErroResposta erro = new ErroResposta(
            LocalDateTime.now(),
            HttpStatus.BAD_REQUEST.value(),
            "Parâmetro inválido",
            "O parâmetro '" + ex.getName() + "' deve ser do tipo " + tipoEsperado,
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erro);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResposta> handleValidacao(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<String> detalhes = ex.getBindingResult().getFieldErrors().stream()
            .map(erro -> erro.getField() + ": " + erro.getDefaultMessage())
            .toList();
        ErroResposta erro = new ErroResposta(
            LocalDateTime.now(),
            HttpStatus.BAD_REQUEST.value(),
            "Dados inválidos",
            "Um ou mais campos estão inválidos",
            request.getRequestURI(),
            detalhes
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erro);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErroResposta> handleRegraDeNegocio(IllegalArgumentException ex, HttpServletRequest request) {
        ErroResposta erro = new ErroResposta(
            LocalDateTime.now(),
            HttpStatus.BAD_REQUEST.value(),
            "Requisição inválida",
            ex.getMessage(),
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erro);
    }

    @ExceptionHandler({
        ProdutoNaoEncontradoException.class,
        CustoInsumoNaoEncontradoException.class,
        DecanteNaoEncontradoException.class,
        MarcaNaoEncontradaException.class,
        LoteCompraNaoEncontradoException.class,
        FrascoAbertoNaoEncontradoException.class
    })
    public ResponseEntity<ErroResposta> handleRecursoNaoEncontrado(RuntimeException ex, HttpServletRequest request) {
        ErroResposta erro = new ErroResposta(
            LocalDateTime.now(),
            HttpStatus.NOT_FOUND.value(),
            "Recurso não encontrado",
            ex.getMessage(),
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(erro);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResposta> handleIntegridade(DataIntegrityViolationException ex, HttpServletRequest request) {
        ErroResposta erro = new ErroResposta(
            LocalDateTime.now(),
            HttpStatus.CONFLICT.value(),
            "Conflito de dados",
            "O registro viola uma restrição de integridade, como um SKU já cadastrado para este usuário.",
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(erro);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResposta> handleErroInesperado(Exception ex, HttpServletRequest request) {
        ErroResposta erro = new ErroResposta(
            LocalDateTime.now(),
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Erro interno",
            "Ocorreu um erro inesperado. Tente novamente mais tarde.",
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(erro);
    }
}
