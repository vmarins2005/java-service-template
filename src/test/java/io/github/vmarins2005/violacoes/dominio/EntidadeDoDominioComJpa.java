package io.github.vmarins2005.violacoes.dominio;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * Violação deliberada: uma classe de domínio anotada com JPA.
 *
 * <p>Mora num pacote {@code ..dominio..} dentro dos testes justamente para que a regra a
 * enxergue. Existe para provar que {@code DOMINIO_SEM_FRAMEWORK} reprova de verdade.
 */
@Entity
public class EntidadeDoDominioComJpa {

    @Id
    private String id;

    public String id() {
        return id;
    }
}
