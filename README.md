# cloudcard-photo-downloader

This project automatically downloads photos from [CloudCard Online Photo Submission](http://onlinephotosubmission.com/).

## Usage

1. Download the jar file.
1. Download [application.properties](https://raw.githubusercontent.com/online-photo-submission/cloudcard-photo-downloader/master/src/main/resources/application.properties) into the same directory
1. Configure `application.properties`
1. Run `java -jar cloudcard-photo-downloader.jar`

## Configuring `application.properties`

`application.properties` is the main configuration file for the exporter.  Below are descriptions of each option:

- cloudcard.api.url  
  - default: `https://api.onlinephotosubmission.com/api`
  - description: This option allows you to specify the URL of your CloudCard Online Photo Submission API.  Most users will not need to change this setting.  Generally, this is only useful if you are testing the integration using the test intance `https://test-api.onlinephotosubmission.com/api`.
- cloudcard.api.accessToken
  - default: none
  - description: this setting holds the API access token for your service account and must be set before the exporter to run. On a Unix/Linux based operating system, you can use `get-token.sh` to get your access token.
- downloader.delay.milliseconds
  - default: `600000` (Ten Minutes)
  - this is the amount of time the exporter will wait between exports
- downloader.photoDirectory
  - default: none
  - description: this is the absolute path to the directory into which the photos will be saved
- downloader.udfDirectory
  - default: 
# The first part of the UDF filename
downloader.udfFilePrefix=CloudCard_Photos_
# The extension to use for the UDF filename
downloader.udfFileExtension=.udf
# The format of the date in the UDF description
downloader.descriptionDateFormat=MMM dd 'at' HHmm
# The format of the date in the UDF batch ID field and in the filename suffix
downloader.batchIdDateFormat=YYYYMMddHHmm
# The format of the date in the UDF Created field
downloader.createdDateFormat=YYYY-MM-dd
# Enable/Disable UDF file generation
downloader.enableUdf=true
