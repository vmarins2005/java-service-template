# ADR 0003 — Teste unitário e de integração em fases separadas

Status: aceito · 2026-09-04 · supera: —

## Contexto

Medido neste repositório:

| comando | o que roda | tempo |
| --- | --- | --- |
| `mvn clean test` | 15 testes, sem contêiner | **13,7 s** |
| `mvn clean verify` | 19 testes, com Postgres real | **1 min 11 s** |

A diferença é quase toda o contêiner do Postgres: 27 s só para os quatro testes de
integração, contra 0,4 s para os quatro testes do caso de uso.

Se tudo rodasse junto, **todo** ciclo de feedback custaria mais de um minuto — inclusive
mudar uma mensagem de erro.

## Decisão

- `*Test` → surefire, fase `test`. Sem Spring, sem banco, sem rede.
- `*IT` → failsafe, fase `integration-test`. Contexto do Spring, Postgres via Testcontainers.

Quem está escrevendo código roda `mvn test` em treze segundos. O CI roda `mvn verify` e paga
o minuto inteiro.

## O que quebrou nessa configuração

**1. `Unable to find a @SpringBootConfiguration`.**

O failsafe roda depois do `package`, e por padrão põe no classpath o artefato do projeto — que
a essa altura é o jar executável do Spring Boot, com as classes debaixo de `BOOT-INF/classes`,
onde a varredura do Spring não as enxerga.

```xml
<classesDirectory>${project.build.outputDirectory}</classesDirectory>
```

**2. Cobertura do teste de integração sumindo.**

O surefire pega o agente do JaCoCo sozinho, porque o plugin publica a propriedade `argLine` e
o surefire a usa por padrão. O failsafe não. Sem `<argLine>${argLine}</argLine>`, o relatório
mostra a camada web inteira descoberta — e o limite de cobertura reprova um build correto.

Os dois são armadilhas de configuração que não aparecem em nenhum tutorial de "como usar
Testcontainers", e as duas custaram um build vermelho para serem encontradas.

## Um contêiner para a suíte inteira

`BancoDeTestes` sobe o Postgres num bloco `static` e o reaproveita. O isolamento entre testes
vem de `TRUNCATE` no `@BeforeEach`, que leva milissegundos.

A alternativa — contêiner por classe — multiplicaria os 27 s pelo número de classes de
integração sem comprar isolamento melhor.

## Consequências

- \+ Feedback de treze segundos para a maior parte do trabalho.
- \+ O contêiner do Postgres é um custo pago uma vez por execução, não por classe.
- − Duas fases significa duas configurações de plugin, e a segunda é a que ninguém lembra de
  ajustar. As duas armadilhas acima são exatamente isso.
- − Um teste nomeado `TarifasTest` em vez de `TarifasIT` roda na fase errada, e se ele
  precisar de banco vai falhar com uma mensagem que não explica nada. A convenção de nome é
  frágil e não há nada aqui verificando que ela foi seguida.
- − `TRUNCATE` não isola tudo: sequências, estado em cache da aplicação e o próprio contexto
  do Spring atravessam os testes. Para este projeto basta; num serviço com cache local,
  não bastaria.
