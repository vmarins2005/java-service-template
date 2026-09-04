package io.github.vmarins2005.violacoes;

import org.springframework.beans.factory.annotation.Autowired;

/** Violação deliberada: dependência injetada por campo, invisível para quem lê o construtor. */
public class ServicoComInjecaoPorCampo {

    @Autowired
    private String dependenciaEscondida;

    public String dependenciaEscondida() {
        return dependenciaEscondida;
    }
}
