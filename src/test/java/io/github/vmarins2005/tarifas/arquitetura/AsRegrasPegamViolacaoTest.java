package io.github.vmarins2005.tarifas.arquitetura;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * O teste que faltava.
 *
 * <p>Uma suíte de arquitetura verde não prova nada sozinha: ela fica verde igual quando as
 * regras funcionam e quando elas não enxergam classe nenhuma — um pacote renomeado, uma
 * opção de importação errada, e a verificação inteira vira decoração silenciosa.
 *
 * <p>Aqui as mesmas regras rodam contra classes escritas para violá-las, e o teste passa
 * quando elas <b>reprovam</b>. A mensagem de erro real vai para a saída, porque é ela que
 * alguém vai ler no dia em que o build quebrar.
 */
class AsRegrasPegamViolacaoTest {

    private static JavaClasses violacoes;

    /**
     * As classes de violação moram <b>fora</b> do pacote da aplicação, e isso custou um build
     * quebrado para ser descoberto.
     *
     * <p>Elas começaram em {@code ...tarifas.arquitetura.violacoes}, e a que existe para
     * violar a regra do domínio está anotada com {@code @Entity}. O Hibernate a varreu junto
     * com as entidades de verdade e o teste de integração morreu com
     * {@code Schema-validation: missing table [entidade_do_dominio_com_jpa]}.
     *
     * <p>Dois aprendizados ficaram: código de teste dentro do pacote da aplicação é código
     * que o Spring enxerga; e {@code ddl-auto: validate} pegou o problema — com
     * {@code update}, o Hibernate teria criado a tabela em silêncio.
     */
    private static final String PACOTE_DAS_VIOLACOES = "io.github.vmarins2005.violacoes";

    @BeforeAll
    static void importa() {
        violacoes = new ClassFileImporter().importPackages(PACOTE_DAS_VIOLACOES);
    }

    @Test
    @DisplayName("a regra do domínio reprova uma classe de domínio anotada com JPA")
    void aRegraDoDominioReprovaEntidadeComJpa() {
        assertThatThrownBy(() -> RegrasDeArquitetura.DOMINIO_SEM_FRAMEWORK.check(violacoes))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("EntidadeDoDominioComJpa")
                .hasMessageContaining("jakarta.persistence")
                .satisfies(erro -> System.out.println("--- regra do domínio ---\n" + erro.getMessage()));
    }

    @Test
    @DisplayName("a regra de injeção reprova um campo anotado com @Autowired")
    void aRegraDeInjecaoReprovaCampoAutowired() {
        assertThatThrownBy(() -> RegrasDeArquitetura.SEM_INJECAO_POR_CAMPO.check(violacoes))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("dependenciaEscondida")
                .satisfies(erro -> System.out.println("--- regra de injeção ---\n" + erro.getMessage()));
    }
}
