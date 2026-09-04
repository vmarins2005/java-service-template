CREATE TABLE tarifas (
    codigo            VARCHAR(32)  PRIMARY KEY,
    descricao         TEXT         NOT NULL,
    valor_em_centavos BIGINT       NOT NULL CHECK (valor_em_centavos > 0)
);

-- O CHECK acima repete a invariante que o record Tarifa já garante. Repetir é proposital:
-- o banco recebe carga de outras origens alem desta aplicacao, e a invariante que so existe
-- em Java protege apenas o caminho que passa por Java.
