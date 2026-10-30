#!/bin/sh
# Writes the SPA runtime config from the container env, so one image serves every environment.
set -e
[ -z "$API_URL" ] && exit 0
cat > /usr/share/nginx/html/config.json <<JSON
{
  "apiUrl": "${API_URL}",
  "authUrl": "${AUTH_URL}",
  "clientId": "${AUTH_CLIENT_ID:-restauco-app}",
  "resource": "${AUTH_RESOURCE}",
  "tenant": "${TENANT}"
}
JSON
