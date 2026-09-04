package io.github.vmarins2005.tarifas.aplicacao;

import io.github.vmarins2005.tarifas.dominio.Tarifa;
import io.github.vmarins2005.tarifas.dominio.TarifaJaCadastradaException;
import io.github.vmarins2005.tarifas.dominio.TarifaNaoEncontradaException;
import io.github.vmarins2005.tarifas.dominio.Tarifas;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CadastroDeTarifas {

    private final Tarifas tarifas;

    /** Injeção por construtor. O teste de arquitetura recusa injeção por campo — ADR 0002. */
    public CadastroDeTarifas(Tarifas tarifas) {
        this.tarifas = tarifas;
    }

    @Transactional
    public Tarifa cadastrar(Tarifa nova) {
        if (tarifas.existe(nova.codigo())) {
            throw new TarifaJaCadastradaException(nova.codigo());
        }
        return tarifas.salvar(nova);
    }

    @Transactional(readOnly = true)
    public Tarifa buscar(String codigo) {
        return tarifas.porCodigo(codigo).orElseThrow(() -> new TarifaNaoEncontradaException(codigo));
    }

    @Transactional(readOnly = true)
    public List<Tarifa> listar() {
        return tarifas.todas();
    }
}
