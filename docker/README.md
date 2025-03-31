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
docker compose -f ./postgres/docker-compose.yaml down -v
```
```bash
docker compose -f ./kafka/docker-compose.yaml down -v
```
```bash
docker compose -f ./mocks/docker-compose.yaml down -v
```
