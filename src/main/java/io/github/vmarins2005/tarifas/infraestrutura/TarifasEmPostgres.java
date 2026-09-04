package io.github.vmarins2005.tarifas.infraestrutura;

import io.github.vmarins2005.tarifas.dominio.Tarifa;
import io.github.vmarins2005.tarifas.dominio.Tarifas;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** O adaptador: a única classe que conhece ao mesmo tempo o domínio e o JPA. */
@Repository
class TarifasEmPostgres implements Tarifas {

    private final TarifasJpa jpa;

    TarifasEmPostgres(TarifasJpa jpa) {
        this.jpa = jpa;
    }

    @Override
    public Tarifa salvar(Tarifa tarifa) {
        return jpa.save(TarifaEntidade.de(tarifa)).paraDominio();
    }

    @Override
    public Optional<Tarifa> porCodigo(String codigo) {
        return jpa.findById(codigo).map(TarifaEntidade::paraDominio);
    }

    @Override
    public List<Tarifa> todas() {
        return jpa.findAll().stream().map(TarifaEntidade::paraDominio).toList();
    }

    @Override
    public boolean existe(String codigo) {
        return jpa.existsById(codigo);
    }
}
