# ADR 0001 — O que entra no esqueleto, e o que fica de fora

Status: aceito · 2026-09-04 · supera: —

## Contexto

Todo template cresce até virar um framework interno que ninguém entende e todo mundo
contorna. O critério para incluir algo precisa ser mais duro do que "é útil".

## O critério

> Entra o que é **caro de acrescentar depois**. Fica de fora o que cada time decide sozinho.

## O que entra, e por que é caro depois

| item | por que não dá para deixar para depois |
| --- | --- |
| **Log em JSON** | trocar o formato depois de haver dashboard, alerta e consulta salva em cima do formato antigo significa refazer todos eles |
| **Migração com Flyway, `ddl-auto: validate`** | um banco que nasceu com `ddl-auto: update` não tem histórico de esquema, e reconstruí-lo depois é arqueologia |
| **Health e readiness** | sem eles o orquestrador não sabe se o serviço subiu, e a descoberta disso costuma acontecer durante o primeiro deploy |
| **Limite de cobertura** | ligar um limite num projeto com 30% de cobertura significa escrever 200 testes antes do próximo commit passar |
| **Regras de arquitetura** | idem: a primeira execução do ArchUnit num projeto de dois anos devolve centenas de violações, e a reação é desligar a regra |
| **SBOM e varredura de CVE** | tecnicamente dá para acrescentar depois; na prática nunca acontece, porque não há um dia em que ele seja urgente até haver um incidente |
| **`open-in-view: false`** | o padrão do Spring Boot é `true`, e desligar depois quebra código que passou a depender de sessão aberta na camada web sem ninguém notar |
| **Encerramento suave** | um serviço que morre no meio de uma requisição só mostra o problema em produção, sob carga, durante um deploy |

## O que fica de fora, e por quê

**Formatador de código.** É decisão de time e a primeira coisa que alguém desliga quando
chega imposta. Fica o `.editorconfig`, que descreve a convenção sem reprovar build.

**Biblioteca de mapeamento (MapStruct e afins).** A conversão entre entidade e domínio deste
projeto tem seis linhas. Introduzir geração de código para isso é trocar seis linhas visíveis
por uma dependência e um passo de build.

**Autenticação.** É o assunto de um projeto inteiro desta série, e um template com Spring
Security mal configurado é pior que um sem — dá a impressão de que o assunto está resolvido.

**Cache, mensageria, cliente HTTP.** Nem todo serviço tem. Um template que traz tudo obriga
cada projeto a remover o que não usa, e ninguém remove: fica lá, aparece no SBOM, e um dia
vira CVE de uma biblioteca que o serviço nunca chamou.

**Métrica de negócio.** O Actuator com Micrometer está no esqueleto; qual métrica publicar é
decisão de quem conhece o domínio.

## Consequências

- \+ O SBOM tem 97 bibliotecas para 8 dependências declaradas. Cada item recusado acima seria
  mais um punhado de bibliotecas em todo serviço que nascer daqui.
- \+ Quem começa um serviço a partir daqui gasta o primeiro dia escrevendo domínio, e não
  configurando Flyway e Actuator pela décima vez.
- − Alguns dos itens recusados vão ser copiados de outro repositório em vez de padronizados,
  e vão ficar ligeiramente diferentes em cada serviço.
- − "É caro depois" é um julgamento, não um critério objetivo. A lista acima vai envelhecer,
  e a hora de revisitá-la é quando o terceiro serviço acrescentar a mesma coisa.
