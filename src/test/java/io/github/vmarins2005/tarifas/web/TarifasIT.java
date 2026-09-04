package io.github.vmarins2005.tarifas.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * O caminho inteiro: HTTP, validação, caso de uso, JPA, Flyway e Postgres de verdade.
 *
 * <p>Termina em {@code IT} de propósito — o failsafe roda esta classe na fase de
 * {@code verify}, e não junto com os testes unitários. Ver ADR 0003: quem quer resposta em
 * segundos roda {@code mvn test}; o CI roda {@code mvn verify} e paga os contêineres.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TarifasIT extends BancoDeTestes {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("cadastra, devolve 201 e passa a listar")
    void cadastraEPassaAListar() throws Exception {
        mockMvc.perform(post("/tarifas")
                        .contentType("application/json")
                        .content("{\"codigo\":\"tar-01\",\"descricao\":\"Tarifa básica\",\"valorEmCentavos\":1990}"))
                .andExpect(status().isCreated())
                // O código foi normalizado pelo domínio antes de chegar ao banco.
                .andExpect(jsonPath("$.codigo").value("TAR-01"));

        mockMvc.perform(get("/tarifas/TAR-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valorEmCentavos").value(1990));
    }

    @Test
    @DisplayName("código repetido vira 409, e não 500")
    void codigoRepetidoVira409() throws Exception {
        cadastra("TAR-02");

        mockMvc.perform(post("/tarifas")
                        .contentType("application/json")
                        .content("{\"codigo\":\"TAR-02\",\"descricao\":\"Outra\",\"valorEmCentavos\":2990}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("já existe tarifa com o código TAR-02"));
    }

    @Test
    @DisplayName("código inexistente vira 404 com problem+json")
    void codigoInexistenteVira404() throws Exception {
        mockMvc.perform(get("/tarifas/NAO-EXISTE"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("não existe tarifa com o código NAO-EXISTE"));
    }

    @Test
    @DisplayName("a validação do pedido barra antes de chegar ao domínio")
    void aValidacaoBarraAntesDoDominio() throws Exception {
        mockMvc.perform(post("/tarifas")
                        .contentType("application/json")
                        .content("{\"codigo\":\"\",\"descricao\":\"Tarifa\",\"valorEmCentavos\":-5}"))
                .andExpect(status().isBadRequest());
    }

    private void cadastra(String codigo) throws Exception {
        mockMvc.perform(post("/tarifas")
                        .contentType("application/json")
                        .content("{\"codigo\":\"%s\",\"descricao\":\"Tarifa\",\"valorEmCentavos\":1990}"
                                .formatted(codigo)))
                .andExpect(status().isCreated());
    }
}
