# csv-generator

---

## Summary

The csv-generator is a batch script that can be called by the cloudcard photo downloader to retrieve user info from the cloudcard API for downloaded photos. 

## Requirements

- OS: Windows with curl installed (now comes packaged with Windows 10)
  - [Download](https://curl.se/windows/)
- OS/Security Roles: Access to photo storage destination 
- [Service account with office level access](https://sharptop.atlassian.net/wiki/spaces/CCD/pages/1226440705/User+Roles) to CloudCard Online Photo Submission


#### Configuration
This script is designed to be configurable with 4 variables:

- curlUrl
  - description: This is pre-configured to pull from the production api endpoint. Most users will not need to change this setting.

- authToken 
  - description: An authentication token from a cloudcard service account is necessary to retrieve info from the api via a curl command. This can be the same token used for running the downloader itself. 

- outputDir
  - description: The desired output path (absolute directory) for where the csv file should be saved to.

- photoDir
  - description: The absolute directory for where the cloudcard photo downloader is saving photos. 


Once these variables are configured, the cloudcard photo downloader needs to be set up to run this script once photos have been sucessfully downloaded (see the Shell/Batch Script Hook Settings in the downloader readme).
