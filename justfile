set shell := ["sh", "-c"]
set allow-duplicate-recipes := true
set positional-arguments := true
set dotenv-load := true
set export := true

# default: compile

docker_data_dir := env("PROJECT_DIRECTORY") + "/" + env("DATA_BASE_PATH")
kafka_data_dir := docker_data_dir + "/.kafka/"
prometheus_data_dir := docker_data_dir + "/.prometheus/"
grafana_data_dir := docker_data_dir + "/.grafana/"


run-migrations:
    docker logs cassandra-temp -f
    liquibase update --defaults-file=support/storage/postgres/liquibase.properties
    liquibase update --defaults-file=support/storage/postgres/liquibase-test.properties
    liquibase update --defaults-file=support/storage/cassandra/liquibase.properties

truncate-all:
    liquibase execute-sql \
              --sql-file=support/storage/cassandra/ddls/truncate-all.cql \
              --defaults-file=support/storage/cassandra/liquibase.properties
    liquibase execute-sql \
              --sql-file=support/storage/postgres/ddls/truncate-all.sql \
              --defaults-file=support/storage/postgres/liquibase.properties

# drop-all:
#     liquibase drop-all \
#               --defaults-file=support/storage/postgres/liquibase.properties

[macos]
[private]
docker-compose-up:
    docker compose \
      --project-directory $PROJECT_DIRECTORY \
      -f support/docker-compose-storage-macos.yml \
      -f support/docker-compose-messaging.yml \
      -f support/docker-compose-observability.yml \
      up -d

#      -f support/docker-compose-messaging.yml \
#      -f support/docker-compose-observability.yml \
[linux]
[private]
docker-compose-up:
    docker compose \
      --project-directory $PROJECT_DIRECTORY \
      -f support/docker-compose-storage-linux.yml \
      up -d

infrastructure-up:
    #!/usr/bin/env bash
    set -euxo pipefail

    if [[ ! -d "{{ kafka_data_dir }}" ]]; then
      mkdir -p "{{ kafka_data_dir }}"
      mkdir -p "{{ prometheus_data_dir }}"
      mkdir -p "{{ grafana_data_dir }}"
      sudo chmod -R 777 "{{ docker_data_dir }}"

      mkdir -p logs/var/vector
      cp -R "$PROJECT_DIRECTORY/support/observability/grafana/extra/." "{{ grafana_data_dir }}"

    fi

    just docker-compose-up

[macos]
[private]
docker-compose-down:
    docker compose \
      --project-directory $PROJECT_DIRECTORY \
      -f support/docker-compose-storage-macos.yml \
      -f support/docker-compose-messaging.yml \
      -f support/docker-compose-observability.yml \
      down

[linux]
[private]
docker-compose-down:
    docker compose \
      --project-directory $PROJECT_DIRECTORY \
      -f support/docker-compose-storage-linux.yml \
      -f support/docker-compose-messaging.yml \
      -f support/docker-compose-observability.yml \
      down

infrastructure-down:
    just docker-compose-down

# [confirm]
clean-infrastructure-data: infrastructure-down
    #!/usr/bin/env bash

    sudo rm -Rf support/observability/data/grafana/alerting || true
    sudo rm -Rf "{{ docker_data_dir }}"
    rm -rf logs
    echo "All infrastructure data cleaned"

[confirm]
clean-kafka-data: infrastructure-down
    sudo rm -Rf "{{ kafka_data_dir }}"
    mkdir -p "{{ kafka_data_dir }}"
    sudo chmod -R 777 "{{ kafka_data_dir }}"

[confirm]
clean-logs-data:
    rm -rf logs
    mkdir -p logs/var/vector

#lstart:
#    #!/usr/bin/env bash
#    set -euxo pipefail
#
#    loki -config.file=support/observability/loki/loki-local-config.yaml &
#    echo $! > .loki.pid
#
#    vector -c support/observability/vector/vector.toml &
#    echo $! > .vector.pid
#
#lstop:
#    #!/usr/bin/env bash
#    set -euxo pipefail
#
#    kill -9 $(cat .loki.pid)
#    kill -9 $(cat .vector.pid)
#    rm .loki.pid
#    rm .vector.pid
