#!/bin/sh

cd app
cp ../client_secrets.json ./backend/client_secrets.json
docker-compose -f docker-compose.yml -f prod.yml up --remove-orphans --build -d