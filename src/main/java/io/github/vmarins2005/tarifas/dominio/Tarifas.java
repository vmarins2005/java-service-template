package io.github.vmarins2005.tarifas.dominio;

import java.util.List;
import java.util.Optional;

/** A porta. Quem implementa está em {@code infraestrutura}, e o domínio não sabe disso. */
public interface Tarifas {

    Tarifa salvar(Tarifa tarifa);

    Optional<Tarifa> porCodigo(String codigo);

    List<Tarifa> todas();

    boolean existe(String codigo);
}
