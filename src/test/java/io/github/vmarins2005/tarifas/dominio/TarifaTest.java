package io.github.vmarins2005.tarifas.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TarifaTest {

    @Test
    @DisplayName("o código é normalizado na construção")
    void oCodigoENormalizadoNaConstrucao() {
        assertThat(new Tarifa("  tar-01 ", "Tarifa básica", 1_990).codigo()).isEqualTo("TAR-01");
    }

    @Test
    void recusaCodigoVazio() {
        assertThatThrownBy(() -> new Tarifa("  ", "Tarifa básica", 1_990))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sem código");
    }

    @Test
    void recusaDescricaoVazia() {
        assertThatThrownBy(() -> new Tarifa("TAR-01", "", 1_990))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sem descrição");
    }

    @Test
    void recusaValorNaoPositivo() {
        assertThatThrownBy(() -> new Tarifa("TAR-01", "Tarifa básica", 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positivo");
    }
}
