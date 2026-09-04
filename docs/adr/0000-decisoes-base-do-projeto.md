# ADR 0000 — Decisões base do projeto

Status: aceito · 2026-09-04 · supera: —

## Contexto

Este repositório é o esqueleto de onde os próximos serviços desta série partem. O que ele
demonstra não é regra de negócio: é **o que o build recusa**.

O domínio inteiro cabe em um `record` de três campos, e isso é proposital. Um template com
domínio interessante desvia a atenção do que ele existe para mostrar.

## Decisões

### 1. Um serviço que roda de verdade, e não um gerador de esqueleto

Tem HTTP, banco com migração, health, log estruturado, contêiner e Compose. Um template que
não sobe é uma lista de dependências com pretensão.

### 2. Toda verificação roda na máquina de quem desenvolve, com o mesmo comando do CI

Não há passo de CI que use uma ação mágica sem equivalente local. A varredura de
vulnerabilidade é a mesma linha de `docker run` nos dois lugares.

Isso importa porque a alternativa — descobrir a regra só quando o CI reprova — é o que faz
as pessoas tratarem o pipeline como obstáculo em vez de rede.

### 3. As regras de arquitetura são testadas contra violações reais

Uma suíte de ArchUnit verde não prova nada sozinha: ela fica verde igual quando as regras
funcionam e quando elas não enxergam classe nenhuma. `AsRegrasPegamViolacaoTest` roda as
mesmas regras contra classes escritas para violá-las. Ver ADR 0002.

### 4. Números medidos, e não configurados

O README não diz "temos boa cobertura": diz 94,0% de linhas e 83,3% de ramos, medidos pelo
mesmo relatório que o build reprova. Não diz "SBOM gerado": diz que 8 dependências
declaradas viram 97 bibliotecas.

### 5. BOM importado, e não `spring-boot-starter-parent`

Usar o parent do Spring Boot herda plugins, perfis e configurações que ninguém leu, e
sequestra o `<parent>` — que numa empresa costuma ser o POM corporativo.

O preço está medido e documentado no ADR 0005: **sobrescrever versão por propriedade deixa de
funcionar**, e isso custou uma rodada de medição para ser descoberto.

## Consequências

- O build completo leva ~1 min 11 s e sobe um Postgres real. `mvn test` sozinho leva 13,7 s
  e não sobe nada — a separação está no ADR 0003.
- Há classes de violação deliberada em `src/test`, fora do pacote da aplicação. O motivo
  dessa localização custou um build quebrado e está registrado em `AsRegrasPegamViolacaoTest`.
- O template nasce em Spring Boot 4.1.1 com **zero vulnerabilidades conhecidas**, e o ADR 0005
  registra o que a atualização quebrou para chegar lá.
