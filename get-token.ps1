$default_api_url = 'https://api.onlinephotosubmission.com/api'

$api_url = Read-Host -Prompt "CloudCard API URL ['$default_api_url']"

if ($api_url -eq '') {
    $api_url = $default_api_url + '/login'
}

Write-Host $api_url

$username = Read-Host -Prompt "CloudCard Username: "
$password = Read-Host -Prompt "CloudCard Password: "

$rawBody = @{
username=$username
password=$password
}

$json = $rawBody | ConvertTo-Json

Invoke-RestMethod $api_url -Method Post -Body $json -ContentType 'application/json' | Format-Table -Property access_token