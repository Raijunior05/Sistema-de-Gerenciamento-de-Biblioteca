# Carrega os dados de demonstracao no banco do docker compose.
# Uso (na raiz do projeto): powershell -ExecutionPolicy Bypass -File scripts\seed-demo.ps1
$ErrorActionPreference = 'Stop'
$container = 'bibliotech-db'
$script = Join-Path $PSScriptRoot 'seed-demo.sql'

docker cp $script "${container}:/tmp/seed-demo.sql"
if ($LASTEXITCODE -ne 0) { throw "Container $container nao encontrado. Rode 'docker compose up -d'." }

docker exec $container psql -U bibliotech -d bibliotech -v ON_ERROR_STOP=1 -f /tmp/seed-demo.sql
if ($LASTEXITCODE -ne 0) { throw 'Falha ao carregar os dados de demonstracao.' }

Write-Host 'Dados de demonstracao carregados. Login extra: coordenacao / demo123'
