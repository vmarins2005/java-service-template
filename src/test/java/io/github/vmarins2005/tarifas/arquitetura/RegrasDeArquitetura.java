package io.github.vmarins2005.tarifas.arquitetura;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.GeneralCodingRules;

/**
 * As regras ficam separadas dos testes porque são usadas por dois deles: o que verifica o
 * código de produção, e o que verifica que <b>as regras pegam</b> uma violação de verdade.
 *
 * <p>Uma regra de arquitetura que nunca reprovou nada não é uma regra: é uma opinião com
 * sintaxe. Ver ADR 0002.
 */
final class RegrasDeArquitetura {

    static final String RAIZ = "io.github.vmarins2005.tarifas";

    private RegrasDeArquitetura() {}

    static final ArchRule CAMADAS = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Domínio").definedBy("..dominio..")
            .layer("Aplicação").definedBy("..aplicacao..")
            .layer("Infraestrutura").definedBy("..infraestrutura..")
            .layer("Web").definedBy("..web..")
            .whereLayer("Web").mayNotBeAccessedByAnyLayer()
            .whereLayer("Infraestrutura").mayNotBeAccessedByAnyLayer()
            .whereLayer("Aplicação").mayOnlyBeAccessedByLayers("Web")
            .whereLayer("Domínio").mayOnlyBeAccessedByLayers("Aplicação", "Infraestrutura", "Web")
            .as("as camadas só olham para dentro");

    /**
     * A regra que o ADR 0002 defende: o domínio não conhece framework nenhum.
     *
     * <p>É a que custa código a mais — obriga uma entidade de JPA separada e um tipo de
     * requisição separado — e a que impede que uma troca de ORM vire uma reescrita do
     * domínio.
     */
    static final ArchRule DOMINIO_SEM_FRAMEWORK = noClasses()
            .that().resideInAPackage("..dominio..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..",
                    "jakarta.persistence..",
                    "com.fasterxml.jackson..")
            .as("o domínio não depende de Spring, de JPA nem de Jackson");

    /**
     * Injeção por campo esconde a dependência de quem lê e obriga um contêiner inteiro para
     * instanciar a classe num teste.
     */
    static final ArchRule SEM_INJECAO_POR_CAMPO = fields()
            .should().notBeAnnotatedWith("org.springframework.beans.factory.annotation.Autowired")
            .as("nenhuma injeção por campo");

    /** {@code System.out} num serviço é log que não vai para lugar nenhum. */
    static final ArchRule SEM_SAIDA_PADRAO =
            GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;
}
