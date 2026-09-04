# ADR 0002 — A fronteira do domínio, verificada por teste

Status: aceito · 2026-09-04 · supera: —

## Contexto

"O domínio não deve conhecer o framework" é consenso em toda discussão de arquitetura e
sobrevive a cerca de três sprints, porque anotar o `record` com `@Entity` economiza uma classe
e ninguém percebe no diff.

Uma convenção que depende de alguém lembrar não é uma convenção: é uma esperança.

## Decisão

Quatro regras verificadas por teste, em `RegrasDeArquitetura`:

1. **Camadas só olham para dentro.** Web e Infraestrutura não são acessadas por ninguém;
   Aplicação só pela Web; Domínio por todas.
2. **O domínio não depende de Spring, JPA nem Jackson.**
3. **Nenhuma injeção por campo.**
4. **Nenhum acesso a `System.out` / `System.err`.**

## O preço da regra 2, cobrado à vista

Ela obriga duas classes que não existiriam:

- `TarifaEntidade`, porque `Tarifa` não pode ter `@Entity`;
- `TarifasController.TarifaPedida`, porque `Tarifa` não pode ter anotação de validação.

São ~40 linhas de código que não fazem nada além de traduzir. É o argumento honesto contra a
regra, e é caro justamente porque é aí que a maioria dos projetos desiste.

O que se compra: `CadastroDeTarifasTest` roda **sem Spring e sem banco**, com um dublê da
porta de quinze linhas, em 0,4 s. E a forma do domínio é decidida pelo domínio, não pelo
mapeamento do ORM.

## A regra que protege as outras

`ArchUnit` aprova qualquer regra sobre um conjunto vazio de classes. Um pacote renomeado, uma
opção de importação errada, e a suíte inteira continua verde sem verificar nada.

Duas defesas:

**1. Contar as classes.** `haClassesParaVerificar` afirma que a importação trouxe mais de oito
classes. Custa uma linha e pega o modo de falha mais silencioso.

**2. Rodar as regras contra violações reais.** `AsRegrasPegamViolacaoTest` executa as mesmas
regras contra classes escritas para violá-las, e passa quando elas **reprovam**. A mensagem
real vai para a saída do build:

```
Architecture Violation [Priority: MEDIUM] - Rule 'o domínio não depende de Spring, de JPA
nem de Jackson' was violated (2 times):
Class <...violacoes.dominio.EntidadeDoDominioComJpa> is annotated with
<jakarta.persistence.Entity>
```

> Uma regra de arquitetura que nunca reprovou nada não é uma regra: é uma opinião com sintaxe.

## Onde as violações moram, e por quê

Fora do pacote da aplicação, em `io.github.vmarins2005.violacoes`.

Elas começaram dentro, em `...tarifas.arquitetura.violacoes`, e o build quebrou: a classe que
existe para violar a regra do domínio está anotada com `@Entity`, o Hibernate a varreu junto
com as entidades de verdade, e o teste de integração morreu com
`Schema-validation: missing table [entidade_do_dominio_com_jpa]`.

Dois aprendizados, e os dois valem além deste repositório:

- **Código de teste dentro do pacote da aplicação é código que o Spring enxerga.**
- **`ddl-auto: validate` pegou.** Com `update`, o Hibernate teria criado a tabela em silêncio,
  e o repositório teria uma tabela fantasma que ninguém sabe de onde veio.

## Consequências

- \+ A fronteira é verificada a cada build, e a verificação é verificada.
- \+ O teste do caso de uso não precisa de contêiner de injeção.
- − ~40 linhas de tradução por agregado, e mais uma classe para manter a cada campo novo.
- − As regras estão em português no `.as(...)`, o que ajuda quem lê o build e atrapalha quem
  procura a mensagem no Google.
- − `consideringOnlyDependenciesInLayers()` ignora dependências para fora das camadas
  declaradas. É o que evita centenas de falsos positivos vindos do JDK, e é também o que
  esconderia uma camada nova que alguém criasse sem declarar aqui.
