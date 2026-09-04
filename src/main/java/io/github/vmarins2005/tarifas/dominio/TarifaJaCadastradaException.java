package io.github.vmarins2005.tarifas.dominio;

public class TarifaJaCadastradaException extends RuntimeException {

    public TarifaJaCadastradaException(String codigo) {
        super("já existe tarifa com o código " + codigo);
    }
}
