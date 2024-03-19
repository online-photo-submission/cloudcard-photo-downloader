#!/bin/bash

curlUrl="https://api.onlinephotosubmission.com/api/reports/people"
authToken=""
outputDir=""
photoDir=""

DateTime=$(date '+%Y-%m-%d_%H-%M-%S')

for f in "$photoDir"/*.jpg
do
    echo "${f##*/}" | cut -f 1 -d '.' >> quotesVar.temp
done

commaVar=$(paste -sd, - < quotesVar.temp)

echo "{ \"include\": [$commaVar] }" > request-body.json

curl --location --request POST "$curlUrl" \
 --header "X-Auth-Token: $authToken" \
 --header "Accept: text/csv" \
 --header "Content-Type: application/json" \
 --data "@request-body.json" \
 --output "$outputDir/downloaded-photo-info_$DateTime.csv"

rm quotesVar.temp
rm request-body.json
# rm "$photoDir/*.jpg"
