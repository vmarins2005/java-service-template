# ADR 0005 — Atualizar o Spring Boot 3.4 para o 4.1, e o que quebrou

Status: aceito · 2026-09-04 · supera: —

## Contexto

O projeto nasceu em Spring Boot 3.4.1 — a versão que a maioria dos tutoriais ainda usa. A
primeira execução da varredura de vulnerabilidade sobre o SBOM devolveu isto:

| | Boot 3.4.1 | Boot 4.1.1 | 4.1.1 + Tomcat fixado |
| --- | --- | --- | --- |
| bibliotecas no SBOM | 78 | 97 | 97 |
| **CVEs distintas** | **79** | 3 | **0** |
| críticas | 7 | 3 | 0 |
| altas | 29 | 0 | 0 |
| médias | 29 | 0 | 0 |
| baixas | 14 | 0 | 0 |
| pacotes afetados | 16 | 1 | 0 |

Setenta e nove vulnerabilidades conhecidas num projeto criado do zero, sem uma linha de
código de negócio. A versão tinha 21 meses.

> Não existe "projeto novo, então está seguro". Existe "a versão que eu copiei do tutorial
> tem a idade do tutorial".

## Decisão

Spring Boot **4.1.1**, com `tomcat-embed-*` fixado em **11.0.25**.

## O que a atualização quebrou

Cinco coisas, e nenhuma delas apareceu como erro de compilação no código de produção.

### 1. O enforcer estava banindo o que o framework passou a usar

```
BannedDependencies failed:
  spring-boot-starter-test:4.1.1
    spring-core:7.0.9
      commons-logging:1.3.6 <--- banned via the exclude/include list
```

Banir `commons-logging` era conselho correto para o Spring 5 e 6, que traziam o `spring-jcl`
no lugar dele. O Spring Framework 7 aposentou o `spring-jcl` e voltou a depender do
`commons-logging` de verdade.

A regra ficou registrada, comentada, dentro do `pom.xml`. Uma regra de enforcer sem
explicação vira ruído que alguém desliga em vez de entender.

### 2. As auto-configurações de teste viraram módulos separados

`@AutoConfigureMockMvc` não existe mais em `spring-boot-starter-test`. Ele mudou de artefato
e de pacote:

```
org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc   (Boot 3)
org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc        (Boot 4)
```

Exige a dependência `org.springframework.boot:spring-boot-webmvc-test`.

### 3. O Flyway parou de rodar — em silêncio

Este foi o pior. No Boot 3, `flyway-core` bastava: a auto-configuração vinha dentro do
`spring-boot-autoconfigure`. O Boot 4 moveu cada integração para o seu próprio módulo, e
`flyway-core` sozinho é apenas a biblioteca.

O sintoma não foi "Flyway não configurado". Foi:

```
Schema validation: missing table [tarifas]
```

A aplicação subiu, o Flyway não rodou, e quem reclamou foi o Hibernate — sobre outro assunto.
Um serviço com `ddl-auto: update` teria **subido normalmente**, criado a tabela pelo Hibernate
e continuado funcionando, com o histórico de migração silenciosamente abandonado.

É o argumento do ADR 0001 para `validate` desde o primeiro dia, e ele não era hipotético.

### 4. Duas majors de Testcontainers no mesmo classpath, em silêncio

Este só apareceu porque a atualização me levou a olhar a árvore de dependências:

```
+- org.testcontainers:postgresql:jar:1.21.3:test
+- org.testcontainers:junit-jupiter:jar:1.21.3:test
|  \- org.testcontainers:testcontainers:jar:2.0.5:test
```

O BOM do Spring Boot 4.1.1 gerencia o Testcontainers **2.0.5**, mas declara apenas o módulo
central. O `testcontainers-bom` deste projeto, fixado em 1.21.3, declara os módulos. Entre
BOMs importados **o primeiro declarado ganha**, e o do Spring Boot estava primeiro.

Resultado: módulos 1.21.3 rodando contra um núcleo 2.0.5. Os testes passavam.

E o `dependencyConvergence` do enforcer **não reclama**, porque para ele são artefatos
diferentes — a regra existe para pegar duas versões do *mesmo* artefato.

A correção foi declarar o `testcontainers-bom` **antes** do BOM do Spring Boot. A alternativa
seria migrar para o Testcontainers 2.x, que consolidou os módulos: `org.testcontainers:postgresql`
e `org.testcontainers:junit-jupiter` simplesmente não existem mais no 2.0.5. Isso é uma
migração de verdade, e não cabia dentro desta.

### 5. E uma regra de enforcer que passou por acidente

Aproveitando a atualização, entrou um `<exclude>junit:junit</exclude>` — JUnit 4 de carona
junto com o 5 é a receita para um teste que nunca roda e ninguém percebe.

A regra passou enquanto o `testcontainers` central resolvia para 2.0.5, e reprovou no minuto
em que a família inteira voltou para o 1.21.3: **o Testcontainers 1.x depende de `junit:junit`
em escopo de compilação**. Banir JUnit 4 é banir o Testcontainers.

A regra saiu, e o comentário no `pom.xml` explica por quê. A defesa que sobra é o provider do
surefire ser o `junit-platform`, que ignora teste de JUnit 4 enquanto o `vintage-engine` não
estiver no classpath.

> Uma regra de enforcer que passa hoje pode estar passando por acidente. A que vale é a que
> alguém viu reprovar.

## A armadilha do Tomcat: propriedade que não sobrescreve

O Boot 4.1.1 traz Tomcat 11.0.24, e três CVEs críticas de bypass de autenticação estão
corrigidas no 11.0.25. A correção documentada é declarar a propriedade:

```xml
<tomcat.version>11.0.25</tomcat.version>
```

**Não funcionou.** O SBOM continuou mostrando 11.0.24.

Propriedade sobrescreve versão gerenciada apenas quando o BOM entra como `<parent>`. Este
projeto **importa** o BOM em `dependencyManagement` (ADR 0000), e nesse caso a resolução usa
as propriedades do BOM importado, não as do projeto.

A saída é gerenciar os artefatos diretamente — entradas declaradas no próprio projeto têm
precedência sobre BOM importado:

```xml
<dependency>
  <groupId>org.apache.tomcat.embed</groupId>
  <artifactId>tomcat-embed-core</artifactId>
  <version>${tomcat.version}</version>
</dependency>
```

E os três artefatos, não só o `core`: `tomcat-embed-el` e `tomcat-embed-websocket` também
vêm do starter.

Este é o preço concreto da decisão do ADR 0000 de não usar o parent do Spring Boot. Ele
existe, é chato, e agora está medido em vez de suposto.

## Consequências

- \+ Zero vulnerabilidades conhecidas no SBOM.
- \+ Todos os quatro problemas foram encontrados pelo build, e não em produção.
- − O SBOM cresceu de 78 para 97 bibliotecas, porque a modularização do Boot 4 troca poucos
  jars grandes por muitos jars pequenos. A superfície de dependência não aumentou; a
  contagem, sim — e isso vale saber antes de comparar os dois números.
- − A linha do Tomcat fixado vai envelhecer e precisa sair quando o Boot alcançar o 11.0.25.
  O comentário no `pom.xml` diz isso; nada verifica.
- − Nada aqui garante que a próxima atualização será tão barata. O caminho 3.4 → 4.1 passou
  por uma mudança de major do Spring Framework, e sair ileso com quatro ajustes foi sorte
  tanto quanto cuidado — este projeto tem 12 classes.
