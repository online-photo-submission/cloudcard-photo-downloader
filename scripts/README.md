# Scripts

---

## download-people-report

### Summary

The csv-generator is a batch script that can be called by the cloudcard photo downloader to retrieve user info from the cloudcard API for downloaded photos. 

### Requirements

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



## sftp-script

### Summary

The sftp-script is a shell script sends downloaded photos over SFTP to a remote location. This script should be stored in the main Cloudcard-Photo-Downloader folder if being used.

### Requirements

- OS: Mac OSX or Linux
- OS/Security Roles: Access to remote storage destination, and a public/private key pair for sending files over SFTP


#### Configuration
This script has 4 variables:

- USER
  - description: The user of the remote machine that photos are being sent to
  
- HOST
  - description: The host name of the remote machine that photos are being sent to

- DESTINATION_DIRECTORY
  - description: The desired output path on your remote machine where you want photos stored

- KEY
  - description: The location of your pem file that contains the private key


Once these variables are configured, the cloudcard photo downloader needs to be set up to run this script once photos have been sucessfully downloaded. The recommended configuration is: `ShellCommandService.postDownloadCommand=./sftp-script.sh`
