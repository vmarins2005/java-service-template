# O esqueleto que decide o que entra

O pipeline em [`.github/workflows/ci.yaml`](.github/workflows/ci.yaml) está **desarmado de
propósito**: ele roda só por acionamento manual, porque este é um repositório de estudo e os
minutos de execução são compartilhados com projetos reais.

Ele foi executado de verdade uma vez, antes de ser desarmado — **2 min 35 s, verde**, com
Testcontainers subindo Postgres no runner, os seis portões e a construção da imagem. Os
gatilhos de um serviço real estão comentados no topo do arquivo.

Template de serviço Java. O que ele demonstra não é regra de negócio — o domínio inteiro é um
`record` de três campos, de propósito. O que ele demonstra é **o que o build recusa**.

Décimo terceiro de uma série em que cada repositório isola um conceito, e o primeiro de que
os próximos partem.

## Como rodar

```bash
./mvnw test      # 15 testes, 13,7 s, nada de contêiner
./mvnw verify    # 19 testes, 1 min 11 s, Postgres real, cobertura, SBOM
docker compose up --build
```

## Os seis portões

Um CI que só roda `mvn test` não decide nada — repete o que a máquina de quem escreveu já
disse. O que dá valor ao pipeline é a lista do que ele **recusa**:

| portão | o que reprova | medido aqui |
| --- | --- | --- |
| **enforcer** | Java ou Maven abaixo do mínimo, duas versões da mesma biblioteca, dependência banida | pegou uma regra obsoleta na atualização do Boot |
| **surefire** | teste unitário vermelho | 15 testes · 13,7 s |
| **failsafe** | teste de integração vermelho, com Postgres real | 4 testes · 27 s |
| **ArchUnit** | camada violada, domínio acoplado a framework, injeção por campo, `System.out` | 12 classes verificadas |
| **JaCoCo** | cobertura abaixo de 80% de linhas / 70% de ramos | **94,0%** e **83,3%** |
| **Trivy sobre o SBOM** | CVE crítica ou alta | **0** — eram **79** |

Todos os seis rodam localmente com o mesmo comando do CI. Não existe passo mágico que só
funcione no pipeline.

## O número que ninguém espera

**8 dependências declaradas → 97 bibliotecas no SBOM.**

Cada uma roda em produção com os privilégios da aplicação. É a razão de existir o portão de
CVE, e a razão de o [ADR 0001](docs/adr/0001-o-que-entra-no-esqueleto-e-o-que-fica-de-fora.md)
recusar itens do esqueleto com tanto rigor.

## "Projeto novo, então está seguro" é falso

O projeto nasceu em Spring Boot 3.4.1 — a versão que a maioria dos tutoriais ainda usa. A
primeira varredura do SBOM:

| | Boot 3.4.1 | Boot 4.1.1 | 4.1.1 + Tomcat fixado |
| --- | --- | --- | --- |
| bibliotecas | 78 | 97 | 97 |
| **CVEs distintas** | **79** | 3 | **0** |
| críticas | 7 | 3 | 0 |
| pacotes afetados | 16 | 1 | 0 |

Setenta e nove vulnerabilidades conhecidas, sem uma linha de código de negócio. A versão tinha
21 meses.

> Não existe "projeto novo, então está seguro". Existe "a versão que eu copiei do tutorial tem
> a idade do tutorial".

A atualização quebrou cinco coisas, e **nenhuma apareceu como erro de compilação** — está tudo
no [ADR 0005](docs/adr/0005-atualizar-o-spring-boot-3-4-para-4-1.md). A pior:

```
Schema validation: missing table [tarifas]
```

O Boot 4 moveu a auto-configuração do Flyway para um módulo próprio. Com `flyway-core`
sozinho, **o Flyway simplesmente não roda** — e quem reclamou foi o Hibernate, sobre outro
assunto. Um serviço com `ddl-auto: update` teria subido normalmente, criado a tabela pelo
Hibernate, e abandonado o histórico de migração em silêncio.

E a correção do Tomcat expôs outra armadilha: `<tomcat.version>11.0.25</tomcat.version>` **não
funciona** quando o BOM é importado em vez de herdado por `<parent>`. Foi preciso gerenciar os
três artefatos `tomcat-embed-*` diretamente.

A quarta só apareceu porque a atualização me levou a olhar a árvore de dependências:

```
+- org.testcontainers:postgresql:jar:1.21.3:test
+- org.testcontainers:junit-jupiter:jar:1.21.3:test
|  \- org.testcontainers:testcontainers:jar:2.0.5:test
```

Duas majors da mesma biblioteca no mesmo classpath, e os testes passando. **Entre BOMs
importados, o primeiro declarado ganha** — e o `dependencyConvergence` do enforcer não
reclama, porque para ele são artefatos diferentes.

## A regra que protege as outras regras

ArchUnit aprova qualquer regra sobre um conjunto vazio. Um pacote renomeado e a suíte inteira
segue verde sem verificar nada.

`AsRegrasPegamViolacaoTest` roda as mesmas regras contra classes escritas para violá-las, e
passa quando elas **reprovam**:

```
Architecture Violation [Priority: MEDIUM] - Rule 'o domínio não depende de Spring, de JPA
nem de Jackson' was violated (2 times):
Class <...violacoes.dominio.EntidadeDoDominioComJpa> is annotated with
<jakarta.persistence.Entity>
```

> Uma regra de arquitetura que nunca reprovou nada não é uma regra: é uma opinião com sintaxe.

Essas classes de violação moram **fora** do pacote da aplicação, e isso custou um build
quebrado para ser descoberto: dentro, o Hibernate varreu a `@Entity` de mentira junto com as
de verdade. Código de teste dentro do pacote da aplicação é código que o Spring enxerga.

## Duas armadilhas de configuração que nenhum tutorial menciona

**`Unable to find a @SpringBootConfiguration`** no teste de integração. O failsafe roda depois
do `package` e põe no classpath o jar executável, onde as classes estão em `BOOT-INF/classes`.
Solução: `<classesDirectory>${project.build.outputDirectory}</classesDirectory>`.

**Cobertura do teste de integração sumindo.** O surefire pega o agente do JaCoCo sozinho; o
failsafe não. Sem `<argLine>${argLine}</argLine>`, o relatório mostra a camada web descoberta
e o limite reprova um build correto.

## O preço da fronteira do domínio

`Tarifa` não tem `@Entity` nem anotação de validação. Isso obriga duas classes que não
existiriam — `TarifaEntidade` e `TarifaPedida` —, cerca de 40 linhas que só traduzem. É o
argumento honesto contra a regra, e está no
[ADR 0002](docs/adr/0002-a-fronteira-do-dominio-verificada-por-teste.md).

O que se compra: `CadastroDeTarifasTest` roda **sem Spring e sem banco**, com um dublê de
quinze linhas, em 0,4 s.

## Decisões registradas

| ADR | Assunto |
| --- | --- |
| [0000](docs/adr/0000-decisoes-base-do-projeto.md) | Escopo, medição, e por que BOM importado em vez de `parent` |
| [0001](docs/adr/0001-o-que-entra-no-esqueleto-e-o-que-fica-de-fora.md) | O critério: entra o que é caro de acrescentar depois |
| [0002](docs/adr/0002-a-fronteira-do-dominio-verificada-por-teste.md) | A fronteira do domínio, e a regra que protege as regras |
| [0003](docs/adr/0003-teste-unitario-e-de-integracao-em-fases-separadas.md) | 13,7 s contra 1 min 11 s: por que duas fases |
| [0004](docs/adr/0004-o-que-o-ci-reprova.md) | Os seis portões, e o que a cobertura **não** mede |
| [0005](docs/adr/0005-atualizar-o-spring-boot-3-4-para-4-1.md) | A atualização: 79 CVEs, três quebras e uma propriedade que não sobrescreve |

## O que fica de fora, de propósito

Formatador de código, biblioteca de mapeamento, autenticação, cache, mensageria e cliente
HTTP. O critério e o motivo de cada recusa estão no ADR 0001 — em resumo: **entra o que é caro
de acrescentar depois; fica de fora o que cada time decide sozinho**.

A imagem de contêiner deste template tem **160 MB**
(`docker image inspect --format '{{.Size}}'`). Ela é um baseline correto e não otimizado —
encolhê-la é o assunto do projeto `java-docker-image-optimization` desta série.

> Este número já esteve errado aqui: dizia 565 MB, lido da coluna `DISK USAGE` do
> `docker images`, que soma camadas compartilhadas e cache de build de outro jeito. Duas
> formas de medir a mesma coisa devolvendo números com três vezes de diferença é exatamente
> o tipo de erro que um README com números convida — e a correção é dizer **qual comando**
> produziu o número, não só o número.

## Exercícios

1. **Anote `Tarifa` com `@Entity`** e apague `TarifaEntidade`. O build reprova em ArchUnit
   antes de qualquer teste rodar. Agora meça quanto código isso economizaria, e decida se
   valeria.
2. **Troque `ddl-auto` para `update`** e remova o `spring-boot-starter-flyway`. A aplicação
   sobe, os testes passam, e o histórico de migração deixou de existir. É o modo de falha
   silencioso do ADR 0005 reproduzido de propósito.
3. **Baixe o Spring Boot para 3.4.1** e rode a varredura. Conte as 79.
4. **Suba o limite de cobertura para 95%** e veja qual portão reprova primeiro. Depois
   escreva um teste sem asserção alguma e observe a cobertura subir.
5. **Remova `<argLine>${argLine}</argLine>`** do failsafe e rode `mvn verify`. O build reprova
   por cobertura, e a mensagem não diz nada sobre o failsafe.

## Regras de trabalho neste repositório

- Todo portão do CI roda localmente, com o mesmo comando.
- Limite de cobertura é propriedade no topo do `pom.xml`: mudá-lo é uma linha visível no diff.
- Regra de arquitetura nova vem acompanhada da classe que a viola.
- Versão fixada fora do BOM vem com comentário dizendo quando pode sair.

## O que eu faria diferente

_A preencher depois de usar._
