#!/bin/bash

DIR=docker
MANIFEST=${DIR}/docker-compose-full.yml

# start the containers
docker compose -f ${MANIFEST} up

# remove the allocated resources
docker compose -f ${MANIFEST} down