package io.github.vmarins2005.tarifas.arquitetura;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** As regras aplicadas ao código de produção. */
class ArquiteturaTest {

    private static JavaClasses producao;

    @BeforeAll
    static void importa() {
        producao = new ClassFileImporter()
                // Sem esta opção, as próprias classes de violação deste pacote de teste
                // entrariam na verificação e o build reprovaria a si mesmo.
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(RegrasDeArquitetura.RAIZ);
    }

    @Test
    @DisplayName("há classes para verificar — a regra que protege as outras")
    void haClassesParaVerificar() {
        // ArchUnit aprova qualquer regra sobre um conjunto vazio. Sem esta asserção, um erro
        // de pacote transformaria a suíte inteira de arquitetura em decoração silenciosa.
        System.out.printf("arquitetura: %d classes de produção verificadas%n", producao.size());
        assertThat(producao.size()).isGreaterThan(8);
    }

    @Test
    void asCamadasSoOlhamParaDentro() {
        RegrasDeArquitetura.CAMADAS.check(producao);
    }

    @Test
    void oDominioNaoDependeDeFramework() {
        RegrasDeArquitetura.DOMINIO_SEM_FRAMEWORK.check(producao);
    }

    @Test
    void nenhumaInjecaoPorCampo() {
        RegrasDeArquitetura.SEM_INJECAO_POR_CAMPO.check(producao);
    }

    @Test
    void nenhumaSaidaPadrao() {
        RegrasDeArquitetura.SEM_SAIDA_PADRAO.check(producao);
    }
}
