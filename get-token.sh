#!/bin/bash

default_api_url="https://api.onlinephotosubmission.com/api"

read -p "CloudCard API URL [$default_api_url]: " api_url
if [ -z "$api_url" ]; then
    api_url="$default_api_url"
fi

read -p "CloudCard Username: " username
read -p "CloudCard Password: " password

curl -s -X POST -H 'Content-Type: application/json' -d "{ \"username\": \"$username\", \"password\": \"$password\" }" "$api_url/login" | \
    python -c "import sys, json; print json.load(sys.stdin)['access_token']"