package io.github.vmarins2005.tarifas.infraestrutura;

import io.github.vmarins2005.tarifas.dominio.Tarifa;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A entidade existe separada do {@link Tarifa} para que o mapeamento do banco não decida a
 * forma do domínio.
 *
 * <p>É código a mais, e é o preço de a regra do ADR 0002 valer de verdade: sem esta classe,
 * o domínio teria anotação de JPA e a fronteira seria só uma intenção.
 */
@Entity
@Table(name = "tarifas")
class TarifaEntidade {

    @Id
    @Column(name = "codigo", nullable = false, length = 32)
    private String codigo;

    @Column(name = "descricao", nullable = false)
    private String descricao;

    @Column(name = "valor_em_centavos", nullable = false)
    private long valorEmCentavos;

    protected TarifaEntidade() {
        // exigido pelo JPA
    }

    private TarifaEntidade(String codigo, String descricao, long valorEmCentavos) {
        this.codigo = codigo;
        this.descricao = descricao;
        this.valorEmCentavos = valorEmCentavos;
    }

    static TarifaEntidade de(Tarifa tarifa) {
        return new TarifaEntidade(tarifa.codigo(), tarifa.descricao(), tarifa.valorEmCentavos());
    }

    Tarifa paraDominio() {
        return new Tarifa(codigo, descricao, valorEmCentavos);
    }
}
