#!/bin/bash

# make sure it is not running or stopped
docker compose -f docker/docker-compose.yml down 2>/dev/null

# start the infra in background
docker compose -f docker/docker-compose.yml up -d

# wait some seconds
sleep 1

# execute the test coverage
sbt ";project integrationTests; clean; IntegrationTest/test;"

# clean up containers
docker compose -f docker/docker-compose.yml down 2>/dev/null


