set curlUrl=https://api.onlinephotosubmission.com/api/reports/people
set authToken=
set outputDir=
set photoDir=

(for %%f in ("%photoDir%*.jpg") do @echo "%%~nf") >> quotesVar.temp

@echo off
setlocal EnableDelayedExpansion

set "line="
(  for /F "delims=" %%a in (quotesVar.temp) do (
      if defined line echo !line!,
      set "line=%%a"
   )
   echo !line!
) > commaVar.temp

echo { "include": [> request-body.json
type commaVar.temp>> request-body.json
echo ] }>> request-body.json

curl --location --request POST "%curlUrl%" ^
 --header "X-Auth-Token: %authToken%" ^
 --header "Accept: text/csv" ^
 --header "Content-Type: application/json" ^
 --data "@request-body.json" ^
 --output "%outputDir%"\downloaded-photo-info_%date:~10,4%-%date:~4,2%-%date:~7,2%_%time:~0,2%%time:~3,2%.csv 2>>&1

del quotesVar.temp
del commaVar.temp
del request-body.json