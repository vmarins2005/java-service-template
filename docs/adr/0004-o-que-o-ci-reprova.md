# ADR 0004 — O que o CI reprova

Status: aceito · 2026-09-04 · supera: —

## Contexto

Um CI que só roda `mvn test` e mostra um selo verde não decide nada — ele repete o que a
máquina de quem escreveu o código já disse.

O que dá valor a um pipeline é a lista do que ele **recusa**.

## Decisão: seis portões

| portão | o que reprova | número medido aqui |
| --- | --- | --- |
| **enforcer** | versão de Java ou Maven abaixo do mínimo, duas versões da mesma biblioteca no classpath, dependência banida | pegou uma regra obsoleta na atualização do Boot — ADR 0005 |
| **surefire** | teste unitário vermelho | 15 testes, 13,7 s |
| **failsafe** | teste de integração vermelho, com Postgres real | 4 testes, 27 s |
| **ArchUnit** | violação de camada, domínio acoplado a framework, injeção por campo, `System.out` | 12 classes verificadas |
| **JaCoCo** | cobertura abaixo de 80% de linhas ou 70% de ramos | está em **94,0%** e **83,3%** |
| **Trivy sobre o SBOM** | CVE crítica ou alta em qualquer dependência | **0** hoje; eram **79** antes da atualização |

## Por que a varredura é do SBOM, e não da imagem

Varrer a imagem também encontraria problemas do sistema de arquivos base, o que é útil — e é
tarde. O SBOM sai do `package`, antes de existir imagem: o build reprova sem gastar tempo
construindo um contêiner que não vai subir.

E o SBOM é o que registra **as versões que o Maven realmente resolveu**, que não são
necessariamente as que estão escritas no `pom.xml`.

## O número que ninguém espera

**8 dependências declaradas → 97 bibliotecas no SBOM.**

Cada uma delas é uma linha de código que roda em produção com os privilégios da aplicação, e
uma CVE em potencial. É a razão de o portão de vulnerabilidade existir, e a razão de o ADR
0001 recusar itens do esqueleto com tanto rigor.

## O limite de cobertura, e o que ele não faz

80% de linhas e 70% de ramos, escritos como propriedade no topo do `pom.xml` para que mudar um
limite seja uma linha visível no diff, e não uma configuração enterrada.

A única exclusão é a classe de bootstrap, e ela está à vista no plugin.

**O que o limite não faz**: cobertura mede o que foi *executado*, não o que foi *verificado*.
Um teste sem asserção alguma cobre tudo. É por isso que a série tem um projeto separado de
mutation testing — `java-rateio-fatura` — e por que o número aqui é um piso, não uma meta.

## Consequências

- \+ Seis motivos distintos para o build reprovar, e cada um com uma mensagem que diz o que
  fazer.
- \+ Todos os seis rodam localmente com o mesmo comando do CI. Não há passo que só exista no
  pipeline.
- − Cada portão é um ponto de atrito, e atrito acumulado vira `[skip ci]`. Os limites estão
  folgados de propósito.
- − O Trivy baixa a base de vulnerabilidades a cada execução do CI. É rede e tempo, e num dia
  em que o serviço deles estiver fora, o build reprova por um motivo que não é do projeto.
- − Um portão de CVE que reprova **hoje** vai reprovar amanhã por uma CVE publicada durante a
  noite, num build cujo código não mudou. Isso é correto e é desconfortável, e o time precisa
  combinar antes o que fazer quando não há correção disponível.
