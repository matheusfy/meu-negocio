-- O Supabase publica o schema public pela Data API (REST) com a chave anon, que é pública.
-- Com RLS ligado e nenhuma policy, essa API não enxerga nenhuma linha.
-- O app conecta como dono das tabelas, e o dono ignora RLS: nada muda para ele (nem no
-- Postgres local). Tabelas criadas em migrations futuras precisam repetir o ENABLE.
-- A flyway_schema_history fica de fora: o Flyway segura um lock nela (em outra conexão)
-- enquanto migra, e o ALTER TABLE aqui esperaria para sempre. Ela só tem metadados das
-- migrations, e a Data API do Supabase também fica desligada.
DO $$
DECLARE
    tabela record;
BEGIN
    FOR tabela IN
        SELECT tablename FROM pg_tables
        WHERE schemaname = 'public' AND tablename <> 'flyway_schema_history'
    LOOP
        EXECUTE format('ALTER TABLE public.%I ENABLE ROW LEVEL SECURITY', tabela.tablename);
    END LOOP;
END $$;
