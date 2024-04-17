#!/bin/bash

curlUrl="https://api.onlinephotosubmission.com"
persistentAccessToken=""
outputDir=""
photoDir=""

response=$(curl -X POST ${curlUrl}/authentication-token --header 'Content-Type: application/json' --data '{"persistentAccessToken": "'$persistentAccessToken'"}')

authToken=$(echo $response | grep -o '"tokenValue":"[^"]*' | sed 's/"tokenValue":"//')
DateTime=$(date '+%Y-%m-%d_%H-%M-%S')

for f in "$photoDir"/*.jpg
do
    echo "${f##*/}" | cut -f 1 -d '.' >> quotesVar.temp
done

commaVar=$(paste -sd, - < quotesVar.temp)

echo "{ \"include\": [$commaVar] }" > request-body.json

curl --location --request POST "${curlUrl}/api/reports/people" \
 --header "X-Auth-Token: $authToken" \
 --header "Accept: text/csv" \
 --header "Content-Type: application/json" \
 --data "@request-body.json" \
 --output "$outputDir/downloaded-photo-info_$DateTime.csv"

rm quotesVar.temp
rm request-body.json
# rm "$photoDir/*.jpg"

curl POST ${curlUrl}person/me/logout --header 'X-Auth-Token: '$authToken'' --header 'Accept: application/json' --header 'Content-Type: application/json' --data '{"authenticationToken": "'$authToken'"}'
