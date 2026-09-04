package io.github.vmarins2005.tarifas.infraestrutura;

import org.springframework.data.jpa.repository.JpaRepository;

interface TarifasJpa extends JpaRepository<TarifaEntidade, String> {}
