# Docker

## Start containere
```bash
docker compose -f ./postgres/docker-compose.yaml up -d
```
```bash
docker compose -f ./kafka/docker-compose.yaml up -d
```
```bash
docker compose -f ./mocks/docker-compose.yaml up -d
```

## Stopp containere
```bash
docker compose -f ./postgres/docker-compose.yaml stop
```
```bash
docker compose -f ./kafka/docker-compose.yaml stop
```
```bash
docker compose -f ./mocks/docker-compose.yaml stop
```

## Slett containere
```bash
docker compose -f ./postgres/docker-compose.yaml rm -s -v -f
```
```bash
docker compose -f ./kafka/docker-compose.yaml rm -s -v -f
```
```bash
docker compose -f ./mocks/docker-compose.yaml rm -s -v -f
```

## Slette volumer
```bash
docker volume rm postgres
```
```bash
docker volume rm kafka-data kafka-secrets schema-registry-secrets
```
```bash
docker compose -f ./mocks/docker-compose.yaml rm -s -v -f
```
