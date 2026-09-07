# Baseline correto, não otimizado — encolher esta imagem é o assunto do projeto
# `java-docker-image-optimization` desta série, que parte exatamente daqui.

FROM eclipse-temurin:21-jdk AS build
WORKDIR /fonte

# O pom vai sozinho primeiro, e as dependências são baixadas numa camada separada.
# Sem isso, cada mudança em uma linha de código refaz o download inteiro do Maven.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B -q dependency:go-offline

COPY src/ src/
# Os testes já rodaram no CI, com contêineres. Repeti-los aqui exigiria Docker dentro do
# Docker e dobraria o tempo de build sem descobrir nada novo.
RUN ./mvnw -B -q package -DskipTests

FROM eclipse-temurin:21-jre AS runtime

# Usuário sem privilégio. O padrão é root, e um processo Java não precisa disso para
# escutar na 8080.
RUN useradd --system --uid 10001 --create-home servico
USER 10001

WORKDIR /app
COPY --from=build --chown=10001 /fonte/target/*.jar app.jar

EXPOSE 8080

# MaxRAMPercentage e não -Xmx: a JVM lê o limite do contêiner e calcula a partir dele.
# Um -Xmx fixo ignora o limite e é como se descobre o OOMKilled em produção — assunto do
# projeto `java-oomkilled-k8s` desta série.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

# Forma "exec" com sh -c para expandir JAVA_OPTS mantendo o java como PID 1 pelo exec.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
