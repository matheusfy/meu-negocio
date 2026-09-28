#!/usr/bin/env bash
# Copia os dados do Postgres local (docker compose do meu-negocio) para o banco do Supabase.
#
# Pré-requisitos:
#   - Docker Desktop aberto e o Postgres local de pé (container meu-negocio-postgres-1).
#   - O app já rodou uma vez contra o Supabase (Railway), para o Flyway ter criado as tabelas.
#   - As tabelas de negócio do Supabase estão vazias (o script confere e aborta se não estiverem).
#
# Uso (Git Bash), com a URI do "Session pooler" do Supabase (Connect → Session pooler):
#   SUPABASE_DB_URL='postgresql://postgres.<ref>:<senha>@aws-1-sa-east-1.pooler.supabase.com:5432/postgres?sslmode=require' \
#     ./scripts/migrar-dados-supabase.sh
#
# A senha fica só na variável de ambiente desta execução; o script não grava nada em disco.
set -euo pipefail

: "${SUPABASE_DB_URL:?defina SUPABASE_DB_URL com a URI do Session pooler do Supabase}"
LOCAL_CONTAINER="${LOCAL_CONTAINER:-meu-negocio-postgres-1}"
PG_IMAGE="${PG_IMAGE:-postgres:18}"

# Tabelas que não são dados de negócio: histórico do Flyway e sessões de login.
EXCLUIDAS="'flyway_schema_history','spring_session','spring_session_attributes'"

local_sql() {
    docker exec -i "$LOCAL_CONTAINER" sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1 -tA "$@"' -- "$@"
}
remoto_sql() {
    docker run --rm -i -e SUPABASE_DB_URL "$PG_IMAGE" sh -c 'psql "$SUPABASE_DB_URL" -v ON_ERROR_STOP=1 -tA "$@"' -- "$@"
}
contagens_sql() {
    # Gera "tabela|linhas" para cada tabela de negócio.
    echo "SELECT string_agg(format('SELECT %L || ''|'' || count(*) FROM public.%I', tablename, tablename), ' UNION ALL ' ORDER BY tablename)
          FROM pg_tables WHERE schemaname = 'public' AND tablename NOT IN ($EXCLUIDAS)"
}

echo "== Conferindo versões do schema (Flyway)"
v_local=$(local_sql -c "SELECT max(version::int) FROM flyway_schema_history WHERE success")
v_remoto=$(remoto_sql -c "SELECT max(version::int) FROM flyway_schema_history WHERE success")
echo "   local: V$v_local | Supabase: V$v_remoto"
if [ "$v_remoto" -lt "$v_local" ]; then
    echo "!! O Supabase está atrás do banco local. Publique a versão atual do app antes." >&2
    exit 1
fi

echo "== Conferindo se o Supabase está vazio"
ocupadas=$(remoto_sql -c "$(contagens_sql)" | remoto_sql | awk -F'|' '$2 > 0')
if [ -n "$ocupadas" ]; then
    echo "!! Estas tabelas do Supabase já têm dados (abortando para não duplicar):" >&2
    echo "$ocupadas" >&2
    exit 1
fi

echo "== Copiando dados (uma transação só: ou vai tudo, ou nada)"
docker exec "$LOCAL_CONTAINER" sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" --data-only --no-owner --no-privileges \
        --exclude-table=flyway_schema_history --exclude-table="spring_session*"' \
    | docker run --rm -i -e SUPABASE_DB_URL "$PG_IMAGE" sh -c 'psql "$SUPABASE_DB_URL" -v ON_ERROR_STOP=1 --single-transaction -q' \
    > /dev/null

echo "== Linhas por tabela (local | Supabase)"
join -t'|' <(local_sql -c "$(contagens_sql)" | local_sql | sort) <(remoto_sql -c "$(contagens_sql)" | remoto_sql | sort) \
    | awk -F'|' '{ status = ($2 == $3) ? "ok" : "DIFERENTE"; printf "   %-20s %6s | %-6s %s\n", $1, $2, $3, status }'
echo "== Pronto. As sequences (ids) foram copiadas junto pelo pg_dump."
