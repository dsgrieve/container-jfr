#!/bin/sh

set -x
set -e

if [ -z "$CONTAINER_JFR_IMAGE" ]; then
    CONTAINER_JFR_IMAGE="quay.io/rh-jmc-team/container-jfr:latest"
fi

echo -e "\n\nRunning $CONTAINER_JFR_IMAGE ...\n\n"

if [ -z "$CONTAINER_JFR_LOG_LEVEL" ]; then
    CONTAINER_JFR_LOG_LEVEL=ALL
fi

if [ -z "$CONTAINER_JFR_WEB_HOST" ]; then
    CONTAINER_JFR_WEB_HOST="0.0.0.0" # listens on all interfaces and hostnames for testing purposes
fi

if [ -z "$CONTAINER_JFR_WEB_PORT" ]; then
    CONTAINER_JFR_WEB_PORT=8181
fi

if [ -z "$CONTAINER_JFR_EXT_WEB_PORT" ]; then
    CONTAINER_JFR_EXT_WEB_PORT="$CONTAINER_JFR_WEB_PORT"
fi

if [ -z "$CONTAINER_JFR_LISTEN_HOST" ]; then
    CONTAINER_JFR_LISTEN_HOST="$CONTAINER_JFR_WEB_HOST"
fi

if [ -z "$CONTAINER_JFR_LISTEN_PORT" ]; then
    CONTAINER_JFR_LISTEN_PORT=9090;
fi

if [ -z "$CONTAINER_JFR_EXT_LISTEN_PORT" ]; then
    CONTAINER_JFR_EXT_LISTEN_PORT="$CONTAINER_JFR_LISTEN_PORT"
fi

if [ -z "$CONTAINER_JFR_AUTH_MANAGER" ]; then
    CONTAINER_JFR_AUTH_MANAGER="com.redhat.rhjmc.containerjfr.net.NoopAuthManager"
fi

podman run \
    --hostname container-jfr \
    --name container-jfr \
    --mount type=tmpfs,target=/flightrecordings \
    -p 9091:9091 \
    -p $CONTAINER_JFR_EXT_LISTEN_PORT:$CONTAINER_JFR_LISTEN_PORT \
    -p $CONTAINER_JFR_EXT_WEB_PORT:$CONTAINER_JFR_WEB_PORT \
    -e CONTAINER_JFR_ENABLE_CORS="$CONTAINER_JFR_ENABLE_CORS" \
    -e CONTAINER_JFR_LOG_LEVEL=$CONTAINER_JFR_LOG_LEVEL \
    -e CONTAINER_JFR_WEB_HOST=$CONTAINER_JFR_WEB_HOST \
    -e CONTAINER_JFR_WEB_PORT=$CONTAINER_JFR_WEB_PORT \
    -e CONTAINER_JFR_EXT_WEB_PORT=$CONTAINER_JFR_EXT_WEB_PORT \
    -e CONTAINER_JFR_LISTEN_HOST=$CONTAINER_JFR_LISTEN_HOST \
    -e CONTAINER_JFR_LISTEN_PORT=$CONTAINER_JFR_LISTEN_PORT \
    -e CONTAINER_JFR_EXT_LISTEN_PORT=$CONTAINER_JFR_EXT_LISTEN_PORT \
    -e CONTAINER_JFR_AUTH_MANAGER=$CONTAINER_JFR_AUTH_MANAGER \
    -e GRAFANA_DATASOURCE_URL=$GRAFANA_DATASOURCE_URL \
    -e GRAFANA_DASHBOARD_URL=$GRAFANA_DASHBOARD_URL \
    -e USE_LOW_MEM_PRESSURE_STREAMING=$USE_LOW_MEM_PRESSURE_STREAMING \
    -e KEYSTORE_PATH=$KEYSTORE_PATH \
    -e KEYSTORE_PASS=$KEYSTORE_PASS \
    -e KEY_PATH=$KEY_PATH \
    -e CERT_PATH=$CERT_PATH \
    --rm -it "$CONTAINER_JFR_IMAGE" "$@"
