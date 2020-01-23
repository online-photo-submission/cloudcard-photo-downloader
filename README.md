# CloudCard Photo Downloader 
(cloudcard-photo-downloader)

---

This project automatically downloads photos from [CloudCard Online Photo Submission](http://onlinephotosubmission.com/).

## Video Tutorial
https://youtu.be/8qUS-rLeZlM

[![Tutorial Video](https://img.youtube.com/vi/0KcCnMOf1jA/0.jpg)](https://youtu.be/8qUS-rLeZlM)

## Requirements

- JDK 1.8 
  - Amazon Corretto 8 (recommended)
    - [Download](https://docs.aws.amazon.com/corretto/latest/corretto-8-ug/downloads-list.html)
    - [Windows Installation Instructions](https://docs.aws.amazon.com/corretto/latest/corretto-8-ug/windows-7-install.html)
    - [Linux Installation Instructions](https://docs.aws.amazon.com/corretto/latest/corretto-8-ug/generic-linux-install.html)
  - [Red Hat OpenJDK 8](https://developers.redhat.com/products/openjdk/download)
- 512MB RAM
- Office level access to [CloudCard Online Photo Submission](http://onlinephotosubmission.com/)

To test your system, run `java -version`.  The output should look like the following.  The exact version isn't important as long as it starts with `1.8`.
> openjdk version "1.8.0_232" <br/>
> OpenJDK Runtime Environment Corretto-8.232.09.2 (build 1.8.0_232-b09) <br/>
> OpenJDK 64-Bit Server VM Corretto-8.232.09.2 (build 25.232-b09, mixed mode)


## Installation and Configuration

1. Create a separate service account for CloudCard Photo Downloader to use. ([Instructions](https://www.youtube.com/watch?v=ZfrjFwrkwZQ))
1. Download the [jar file](https://github.com/online-photo-submission/cloudcard-photo-downloader/raw/master/cloudcard-photo-downloader.jar).
1. Download [application.properties](https://raw.githubusercontent.com/online-photo-submission/cloudcard-photo-downloader/master/src/main/resources/application.properties) into the same directory
1. Get your access token (Instructions are included in the [service account video](https://www.youtube.com/watch?v=ZfrjFwrkwZQ).)
1. Configure `application.properties`
1. Run `java -jar cloudcard-photo-downloader.jar` from within the same directory as the JAR and application.properties files.
1. *Recommended:* Set up the command to run as a service that starts automatically when the server starts.  The process for doing this is outside the scope of these instructions.

## Troubleshooting

Immediatly upon startup you get the following error:

    com.cloudcard.photoDownloader.ApplicationPropertiesException: The CloudCard API access token must be specified. Please update the 'application.properties' file.
    
1. Make sure the `application.properties` is in the same directory.
1. Make sure the `application.properties` is not named `application.properties.txt`.
1. As a workaround, You can also specify config values without an `application.properties` file using the following syntax
    1. `java -Dconfig.key=config.value -jar cloudcard-photo-downloader.jar`
    1. For example: `java -Dcloudcard.api.accessToken=abc123 -jar cloudcard-photo-downloader.jar`


## Configuration

The simplest way to configure the application is by creating an `application.properties` file, which should be saved in the same directory as the downloader.  There are, however, many other strategies for configuring the application.  For example you may configure the settings using environment variables, JVM variables, etc.  See the [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html) for more information on those options.

Below are descriptions of each option:

#### API Settings
- cloudcard.api.url  
  - default: `https://api.onlinephotosubmission.com/api`
  - description: This option allows you to specify the URL of your CloudCard Online Photo Submission API.  Most users will not need to change this setting.  Generally, this is only useful if you are testing the integration using the test intance `https://test-api.onlinephotosubmission.com/api`.
- cloudcard.api.accessToken
  - default: none
  - description: this setting holds the API access token for your service account and must be set before the exporter to run. On a Unix/Linux based operating system, you can use `get-token.sh` to get your access token. On Windows systems, you can use `get-token.ps1` in a PowerShell window to get your access token. If you have problems with execution policy, see [this StackExchange question](https://superuser.com/questions/106360/how-to-enable-execution-of-powershell-scripts) for how to resolve the issue.
- downloader.fetchStatuses
  - default: `READY_FOR_DOWNLOAD`
  - allowed values: `PENDING`,`APPROVED`,`DENIED`,`READY_FOR_DOWNLOAD`,`DOWNLOADED`,`DISCARDED`,`DONE`
  - description: Photos with these statuses will be downloaded. Separate statuses with a comma.
- downloader.putStatus
  - default: `DOWNLOADED`
  - allowed values: `PENDING`,`APPROVED`,`DENIED`,`READY_FOR_DOWNLOAD`,`DOWNLOADED`,`DISCARDED`,`DONE`
  - description: Downloaded photos will be marked with this status in the CloudCard web application.

#### General Settings
- downloader.delay.milliseconds
  - default: `600000` (Ten Minutes)
  - this is the amount of time the exporter will wait between exports
- downloader.photoDirectories
  - default: `.`
  - description: this is the absolute path to the directory(ies) into which the photos will be saved. Separate multiple directories with commas.  If multiple directories are specified, a copy of each downloaded photo will be saved to each directory. 
  
#### UDF Settings - Deprecated
- downloader.udfDirectory
  - default: `.`
- downloader.udfFilePrefix
  - default: `CloudCard_Photos_`
  - description: The first part of the UDF filename.  The UDF filename is constructed by concatonating the `udfFilePrefix`, a date formated accordning to `batchIdDateFormat`, and `udfFileExtension`.  Given the defaults the generated filename will look something like `CloudCard_Photos_201805221648.udf`
- downloader.udfFileExtension
  - default: `.udf`
  - description: The extension to use for the UDF filename (see the description for `downloader.udfFilePrefix`)
- downloader.descriptionDateFormat
  - default: `MMM dd 'at' HHmm`
  - description: The format of the date in the UDF description (i.e. `!Description: Photo Import May 22 at 1541`)
- downloader.batchIdDateFormat
  - default: `YYYYMMddHHmm`
  - description: The format of the date in the UDF batch ID field (i.e. `!BatchID: 201805221541`) and in the filename suffix (see the description for `downloader.udfFilePrefix`) 
- downloader.createdDateFormat
  - default: `YYYY-MM-dd`
  - description: The format of the date in the UDF Created field (i.e. `!Created: 2018-05-22`)
- downloader.enableUdf
  - default: `true`
  - description: Enable/Disable UDF file generation
  
#### CSV Settings - Coming Soon
- downloader.csvDirectory
  - **Ignore this setting**
  - default: `.`
  - absolute path to the CSV directory
- downloader.csvFilePrefix
  - **Ignore this setting**
  - default: `CloudCard_Photos_`
  - The first part of the CSV filename
- downloader.csvFileExtension
  - **Ignore this setting**
  - default: `.csv`
  - The extension to use for the CSV filename
- downloader.csvBatchIdDateFormat
  - **Ignore this setting**
  - default: `YYYYMMddHHmm`
  - The format of the date in the CSV batch ID field and in the filename suffix
- downloader.enableCsv
  - **Ignore this setting**
  - default: `true`
  - Enable/Disable CSV file generation

## Support and Warranty
THIS PROJECT IS DISTRIBUTED WITH NO WARRANTY.  SEE THE LICENSE FOR FULL DETAILS.
If your organization needs fully warranteed CloudCard integration software, consider Cloud Photo Connect from [Vision Database Systems](http://www.visiondatabase.com/).
