package io.github.vmarins2005.tarifas.web;

import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Um contêiner para a suíte inteira, iniciado uma vez e reaproveitado.
 *
 * <p>Subir e derrubar Postgres por classe de teste custa dezenas de segundos e não compra
 * nada: o isolamento vem de limpar as tabelas antes de cada teste, que leva milissegundos.
 * O contêiner morre quando a JVM morre — o Testcontainers cuida disso pelo Ryuk.
 */
abstract class BancoDeTestes {

    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16").withReuse(false);

    static {
        POSTGRES.start();
    }

    @Autowired
    private DataSource dataSource;

    @DynamicPropertySource
    static void apontaParaOContainer(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registro.add("spring.datasource.username", POSTGRES::getUsername);
        registro.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @BeforeEach
    void limpaAsTabelas() throws Exception {
        try (var conexao = dataSource.getConnection();
                var comando = conexao.createStatement()) {
            // TRUNCATE e não DELETE: reinicia sequências e é mais rápido. E não apaga a
            // tabela de controle do Flyway, que precisa sobreviver entre os testes.
            comando.execute("TRUNCATE TABLE tarifas");
        }
    }
}
