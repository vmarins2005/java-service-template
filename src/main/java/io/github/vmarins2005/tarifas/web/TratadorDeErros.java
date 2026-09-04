package io.github.vmarins2005.tarifas.web;

import io.github.vmarins2005.tarifas.dominio.TarifaJaCadastradaException;
import io.github.vmarins2005.tarifas.dominio.TarifaNaoEncontradaException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Erro de domínio vira {@code application/problem+json} num lugar só.
 *
 * <p>Sem isto, cada controlador inventa o seu formato de erro, e quem consome a API descobre
 * três formatos diferentes em produção.
 */
@RestControllerAdvice
class TratadorDeErros {

    @ExceptionHandler(TarifaNaoEncontradaException.class)
    ProblemDetail naoEncontrada(TarifaNaoEncontradaException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(TarifaJaCadastradaException.class)
    ProblemDetail jaCadastrada(TarifaJaCadastradaException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail invariante(IllegalArgumentException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }
}
