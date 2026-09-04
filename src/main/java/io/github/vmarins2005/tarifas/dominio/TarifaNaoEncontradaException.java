package io.github.vmarins2005.tarifas.dominio;

public class TarifaNaoEncontradaException extends RuntimeException {

    public TarifaNaoEncontradaException(String codigo) {
        super("não existe tarifa com o código " + codigo);
    }
}
