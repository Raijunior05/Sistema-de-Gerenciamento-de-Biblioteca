#!/usr/bin/env sh
# Carrega os dados de demonstracao no banco do docker compose.
# Uso (na raiz do projeto): sh scripts/seed-demo.sh
set -e
CONTAINER=bibliotech-db
DIR=$(cd "$(dirname "$0")" && pwd)

docker cp "$DIR/seed-demo.sql" "$CONTAINER:/tmp/seed-demo.sql"
docker exec "$CONTAINER" psql -U bibliotech -d bibliotech -v ON_ERROR_STOP=1 -f /tmp/seed-demo.sql

echo "Dados de demonstracao carregados. Login extra: coordenacao / demo123"
