package io.github.vmarins2005.tarifas.aplicacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.vmarins2005.tarifas.dominio.Tarifa;
import io.github.vmarins2005.tarifas.dominio.TarifaJaCadastradaException;
import io.github.vmarins2005.tarifas.dominio.TarifaNaoEncontradaException;
import io.github.vmarins2005.tarifas.dominio.Tarifas;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Sem Spring e sem banco: o caso de uso é uma classe comum, e o dublê da porta cabe em
 * quinze linhas. É a consequência prática de o domínio não conhecer framework.
 */
class CadastroDeTarifasTest {

    private final TarifasNaMemoria tarifas = new TarifasNaMemoria();
    private final CadastroDeTarifas cadastro = new CadastroDeTarifas(tarifas);

    @Test
    void cadastraEDevolveATarifa() {
        var criada = cadastro.cadastrar(new Tarifa("TAR-01", "Tarifa básica", 1_990));

        assertThat(criada.codigo()).isEqualTo("TAR-01");
        assertThat(cadastro.listar()).containsExactly(criada);
    }

    @Test
    @DisplayName("recusa código repetido")
    void recusaCodigoRepetido() {
        cadastro.cadastrar(new Tarifa("TAR-01", "Tarifa básica", 1_990));

        assertThatThrownBy(() -> cadastro.cadastrar(new Tarifa("tar-01", "Outra", 2_990)))
                .isInstanceOf(TarifaJaCadastradaException.class);
    }

    @Test
    void reclamaDeCodigoInexistente() {
        assertThatThrownBy(() -> cadastro.buscar("TAR-99"))
                .isInstanceOf(TarifaNaoEncontradaException.class)
                .hasMessageContaining("TAR-99");
    }

    @Test
    void buscaPorCodigo() {
        cadastro.cadastrar(new Tarifa("TAR-01", "Tarifa básica", 1_990));

        assertThat(cadastro.buscar("TAR-01").valorEmCentavos()).isEqualTo(1_990);
    }

    private static final class TarifasNaMemoria implements Tarifas {

        private final List<Tarifa> guardadas = new ArrayList<>();

        @Override
        public Tarifa salvar(Tarifa tarifa) {
            guardadas.add(tarifa);
            return tarifa;
        }

        @Override
        public Optional<Tarifa> porCodigo(String codigo) {
            return guardadas.stream().filter(t -> t.codigo().equals(codigo)).findFirst();
        }

        @Override
        public List<Tarifa> todas() {
            return List.copyOf(guardadas);
        }

        @Override
        public boolean existe(String codigo) {
            return porCodigo(codigo).isPresent();
        }
    }
}
