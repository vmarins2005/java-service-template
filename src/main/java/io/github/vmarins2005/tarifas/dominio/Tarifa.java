package io.github.vmarins2005.tarifas.dominio;

/**
 * O domínio inteiro deste template cabe aqui, e é de propósito: o que o repositório
 * demonstra é o esqueleto em volta, não a regra de negócio.
 *
 * <p>Repare no que <b>não</b> tem: nenhuma anotação de JPA, nenhuma de Jackson, nenhuma de
 * Spring. Isso não é purismo — é uma regra verificada por teste em
 * {@code ArquiteturaTest}, e o motivo está no ADR 0002.
 */
public record Tarifa(String codigo, String descricao, long valorEmCentavos) {

    public Tarifa {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("tarifa sem código não pode ser cadastrada");
        }
        if (descricao == null || descricao.isBlank()) {
            throw new IllegalArgumentException("tarifa sem descrição não pode ser cadastrada");
        }
        if (valorEmCentavos <= 0) {
            throw new IllegalArgumentException("valor deve ser positivo: " + valorEmCentavos);
        }
        codigo = codigo.trim().toUpperCase();
    }
}
