# Scripts

---

## download-people-report.bat

### Summary

This is a batch script that can be called by the cloudcard photo downloader to retrieve user info from the cloudcard API for downloaded photos. 

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
  - note: we recommend using a temporary directory, rather than the primary folder that the downloader is saving photos to. The downloader can save photos to multiple directories using the downloader.photoDirectories property and specifying multiple destination folders (separated by commas). This way, this script clears out the temporary directory each time it runs (so it is only retrieving info for the photos that were just downloaded). 


Once these variables are configured, the cloudcard photo downloader needs to be set up to run this script once photos have been sucessfully downloaded (see the Shell/Batch Script Hook Settings in the downloader readme).

---

## sftp-script.sh

### Summary

The sftp-script sends downloaded photos over SFTP to a remote location. This script should be stored in the main Cloudcard-Photo-Downloader folder if being used.

### Requirements

- OS: Mac OSX or Linux
- OS/Security Roles: Access to remote storage destination, and a public/private key pair for sending files over SFTP


#### Configuration ([Video](https://video.drift.com/v/abYGbYsT875/))

This script has 4 variables that you must set:

- USER
  - description: The user of the remote machine that photos are being sent to
  
- HOST
  - description: The host name of the remote machine that photos are being sent to

- DESTINATION_DIRECTORY
  - description: The desired output path on your remote machine where you want photos stored

- KEY
  - description: The location of your pem file that contains the private key


Once these variables are configured, the cloudcard photo downloader needs to be set up to run this script once photos have been sucessfully downloaded. The recommended configuration is: `ShellCommandService.postDownloadCommand=./sftp-script.sh`

#### Encrypting application properties ([Video])(https://video.drift.com/v/abfYgZ7R05O/)
-This method allows for encrypting the run.sh file and any application properties that it contains, utilizing AES-256 encryption
  -In order to use this, you'll also need to download the `encode-run-sh.sh` script as well as the `decrypt-and-execute-run-sh.sh` script
  - note: this method only works on linux servers at this point
  1. Add any properties you want to encrypt to the run.sh as command line variables using Java -D syntax
  2. Run `encode-run-sh.sh` and enter a password
  3. Delete your `run.sh` file (or back it up in a secure location)
  4. Run `decrypt-and-execute-run-sh.sh` (will request your password)
