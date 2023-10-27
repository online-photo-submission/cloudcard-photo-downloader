# CloudCard Photo Downloader

(cloudcard-photo-downloader)

---

## Summary

This project automatically downloads photos from [CloudCard Online Photo Submission](http://onlinephotosubmission.com/).

## Tutorial Videos

[![Tutorial Videos](https://online-photo-submission.github.io/cloudcard-photo-downloader/video-preview.png)](https://www.youtube.com/playlist?list=PLBUntFa_QZcw6mOV96Fy4NyGVpTR6_6pA)

## Requirements

- Java 17 
    - Amazon Corretto 17 (recommended)
        - [Download](https://docs.aws.amazon.com/corretto/latest/corretto-17-ug/downloads-list.html)
        - [Windows Installation Instructions](https://docs.aws.amazon.com/corretto/latest/corretto-17-ug/windows-7-install.html)
        - [Linux Installation Instructions](https://docs.aws.amazon.com/corretto/latest/corretto-17-ug/generic-linux-install.html)
    - Any other full-featured Java should work, but we only test the downloader on Corretto.
- 512MB RAM
- Storage: 1GB
- OS: Any
- Processor: Any
- Storage Location - OS or Data: Any
- OS/Security Roles: Access to photo storage destination
- [Service account with office level access](https://sharptop.atlassian.net/wiki/spaces/CCD/pages/1226440705/User+Roles)
  to CloudCard Online Photo Submission
- Outbound network access to the following servers/ports if your organization requires all outbound traffic to be
  whitelisted
    - api.onlinephotosubmission.com:443
    - api.cloudcard.ca:443 (only necessary if using CloudCard's Canada specific instance)
    - onlinephoto-api.transactcampus.net:443 (only necessary if using Online Photo Submission through Transact)
    - s3-ca-central-1.amazonaws.com:443
    - test-api.onlinephotosubmission.com:443 (only necessary if using CloudCard's test instance)
    - s3.amazonaws.com:443 (if using CloudCard's test instance)

To test your system, run `java -version`. The output should look like the following. The exact version isn't important
as long as it starts with `1.8`.
> openjdk version "1.8.0_232" <br/>
> OpenJDK Runtime Environment Corretto-8.232.09.2 (build 1.8.0_232-b09) <br/>
> OpenJDK 64-Bit Server VM Corretto-8.232.09.2 (build 25.232-b09, mixed mode)

## Network Diagram

[![Network Diaram](https://online-photo-submission.github.io/cloudcard-photo-downloader/downloader-diagram.png)](https://online-photo-submission.github.io/cloudcard-photo-downloader/downloader-diagram.png)

## Installation and Configuration ([Video](https://youtu.be/a9P57lKKo2Q))

1. Create a separate [service account](https://sharptop.atlassian.net/wiki/spaces/CCD/pages/1226440705/User+Roles) for
   CloudCard Photo Downloader to use. ([Instructions](https://youtu.be/_J9WKAMZOdY))
1. Download
   the [zip file](https://github.com/online-photo-submission/cloudcard-photo-downloader/raw/master/cloudcard-photo-downloader.zip).
1. Get your access token (Instructions are included in the [service account video](https://youtu.be/_J9WKAMZOdY).)
1.

Configure `application.properties` ([Instructions](https://github.com/online-photo-submission/cloudcard-photo-downloader#configuration))

1. Open a terminal/command prompt and navigate to the cloudcard-photo-downloader directory.
1. Run `run` (Windows) or `./run.sh` (Linux/Mac).
1. Check the file `downloader.log` for output from the downloader.
1. *Recommended:* Set up the command to run as a service that starts automatically when the server starts. The process
   for doing this is dependent on your operating system and is outside the scope of these instructions.

## Troubleshooting

Immediately upon startup you get the following error:

    com.cloudcard.photoDownloader.ApplicationPropertiesException: The CloudCard API access token must be specified. Please update the 'application.properties' file.

1. Make sure the `application.properties` is in the same directory.
1. Make sure the `application.properties` is not named `application.properties.txt`.
1. As a workaround, You can also specify config values without an `application.properties` file using the following
   syntax
    1. `java -Dconfig.key=config.value -jar cloudcard-photo-downloader.jar`
    1. For example: `java -Dcloudcard.api.accessToken=abc123 -jar cloudcard-photo-downloader.jar`

## Configuration

The simplest way to configure the application is by creating an `application.properties` file, which should be saved in
the same directory as the downloader.
There are, however, many other strategies for configuring the application. For example you may configure the settings
using environment variables, JVM
variables, etc. See
the [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html)
for more information on those options.

Below are descriptions of each option:

#### General Settings ([Video](https://youtu.be/B4xGNDWkk00))

- downloader.photoService
    - default: `SqsPhotoService`
    - description: this determines the strategy used to retrieve the photos to be downloaded
    - options:
        - `SqsPhotoService` - (RECOMMENDED) retrieves the photo data from an AWS SQS queue in near-realtime
        - `CloudCardPhotoService` - retrieves the photo data from the CloudCard API directly no more often than every 10 minutes
- downloader.storageService
    - default: `FileStorageService`
    - description: this setting determines how the downloaded photos will be stored
    - options:
        - `FileStorageService` - stores images as jpeg files on the local or network file system
        - `DatabaseStorageService` - stores the jpeg encoded images as `BLOBs` in a relational database
        - `TouchNetStorageService` - sends images to a TouchNet API.
- downloader.repeat
    - default: `true`
    - description: This setting determines if the downloader will run once and exit, `downloader.repeat=false`, or if
      will run continually, `downloader.repeat=true`
- downloader.delay.milliseconds
    - default: `600000` (Ten Minutes)
    - description: this is the amount of time the downloader will wait between download attempts.
    - note: `downloader.repeat` must be set to `true` for this setting to have any effect.
- downloader.minPhotoIdLength
    - default: `0`
    - description: This setting causes photo IDs to be left padded with zeros (0) until they have at least this many
      digits.

#### SqsPhotoService Settings

- sqsPhotoService.queueUrl
    - default: none
- sqsPhotoService.pollingIntervalSeconds
    - default: 0
    - description: how long to wait between SQS requests for new messages
- sqsPhotoService.pollingDurationSeconds
    - default: 20
    - description: used to configure the duration of [SQS Long Polling](https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-short-and-long-polling.html)

#### CloudCardPhotoService Settings

- cloudcard.api.url
    - default: `https://api.onlinephotosubmission.com/api`
    - Canadian customers should use `https://api.cloudcard.ca/api`
    - Test Instance: `https://test-api.onlinephotosubmission.com/api`
    - Transact Customers: `https://onlinephoto-api.transactcampus.net/api`
    - description: This option allows you to specify the URL of your CloudCard Online Photo Submission API.
- cloudcard.api.accessToken
    - default: none
    - description: this setting holds the API access token for your service account and must be set before the exporter
      to run.
- downloader.fetchStatuses
    - default: `READY_FOR_DOWNLOAD`
    - allowed values: `PENDING`,`APPROVED`,`DENIED`,`READY_FOR_DOWNLOAD`,`DOWNLOADED`,`DISCARDED`,`DONE`
    - description: Photos with these statuses will be downloaded. Separate statuses with a comma.
- downloader.putStatus
    - default: `DOWNLOADED`
    - allowed values: `PENDING`,`APPROVED`,`DENIED`,`READY_FOR_DOWNLOAD`,`DOWNLOADED`,`DISCARDED`,`DONE`
    - description: Downloaded photos will be marked with this status in the CloudCard web application.

#### PhotoService Settings

- downloader.photoService
    - default: `SqsPhotoService`
    - description: Allows downloading photos in near real-time by making use of queueing with AWS SQS. Please contact support@cloudcard.us to have a queue configured for you in AWS.
    - options: `CloudCardPhotoService` - Legacy photo service that allows scheduled polling for photos to download. This option may be helpful if managing the Downloader with the Windows Task Scheduler.

#### SqsPhotoService Settings

- sqsPhotoService.queueUrl
    - default: none
    - description: The URL of the SQS queue. This will be provided by support once configured upon request. 
        - note: This is required if using the SqsPhotoService.
  
- sqsPhotoService.pollingIntervalSeconds
    - default: 0
  
- sqsPhotoService.pollingDurationSeconds
    - default: 20

#### Shell/Batch Script Hook Settings ([Video](https://youtu.be/aJvwVxZtNTQ))

- ShellCommandService.preExecuteCommand
    - default: none
    - description: this shell / batch script will be executed before each time that the downloader executes regardless
      of whether any photos are ready to be downloaded
- ShellCommandService.preDownloadCommand
    - default: none
    - description: this shell / batch script will be executed after finding photos that are ready to be downloaded but
      before downloading them
- ShellCommandService.postDownloadCommand
    - default: none
    - description: this shell / batch script will be executed after each time that the downloader successfully downloads
      photos
    - note: This hook is particularly useful for immediately starting an external process to import downloaded photos
      into a card production system
- ShellCommandService.postExecuteCommand
    - default: none
    - description: this shell / batch script will be executed after each time that the downloader executes regardless of
      whether any photos were downloaded

#### FileStorageService Settings ([Video](https://youtu.be/rfAIyniCDAw))

*Note: `downloader.storageService` must be set to `FileStorageService` for these to have any effect.*

- downloader.photoDirectories
    - default: `downloaded-photos`
    - description: This is the absolute path to the directory(ies) into which the photos will be saved. Separate
      multiple directories with commas. If multiple directories are specified, a copy of each downloaded photo will be
      saved to each directory.

#### DatabaseStorageService Settings ([Video](https://youtu.be/rCdqabrcrJA))

*Note: `downloader.storageService` must be set to `DatabaseStorageService` for these to have any effect.*

- db.mapping.table
    - default: `CLOUDCARD_PHOTOS`
    - description: This is the name of the table into which photos will be stored.
- db.mapping.column.studentId
    - default: `STUDENT_ID`
    - description: This is the name of the `VARCHAR` column into which the cardholder's ID will be stored.
- db.mapping.column.photoId
    - default: `PHOTO`
    - description: This is the name of the `BLOB` column into which the cardholder's jpeg encoded image will be stored.

### Database Connection Settings ([Video](https://youtu.be/JeykYIykI6k))

- db.datasource.enabled
    - default: `false`
- db.datasource.driverClassName
    - default: none
    - options:
        - Oracle: `oracle.jdbc.OracleDriver`
        - MS SQLServer: `com.microsoft.sqlserver.jdbc.SQLServerDriver`
        - MySQL: `com.mysql.cj.jdbc.Driver`
- db.datasource.url
    - default: none
- db.datasource.username
    - default: none
- db.datasource.password
    - default: none
- db.datasource.schema
    - default: none
- spring.jpa.hibernate.dialect
    - default: none
    - options:
        - Oracle: `org.hibernate.dialect.Oracle10gDialect`
        - MS SQLServer: `org.hibernate.dialect.SQLServer2012Dialect`
        - MySQL: `org.hibernate.dialect.MySQL5InnoDBDialect`

#### TouchNet Client Settings

*Note: `downloader.storageService` must be set to `TouchNetStorageService` for these to have any effect.*

- TouchNetClient.apiUrl
- TouchNetClient.developerKey
- TouchNetClient.operatorId
- TouchNetClient.operatorPassword
- TouchNetClient.terminalId
- TouchNetClient.terminalType
    - default: `ThirdParty`
- TouchNetClient.originId

#### CCure Client Settings

*Note: `downloader.storageService` must be set to `CCureStorageService` for these to have any effect.*

- `CCureClient.apiUrl`
  - API URL of the CCure 9000 server that should receive photos
- `CCureClient.operatorId`
  - Username of an operator level account in CCure 9000 that can upload primary portraits.
- `CCureClient.operatorPassword`
  - Password of the operator account defined by `CCureClient.operatorId`
- `CCureClient.clientName`
  - Identifies the integration to CCure. This most likely needs to be the default. 
  - Default: `CloudCard`
- `CCureClient.clientId`
  - The client ID of the cloudcard integration with CCure - this is necessary for CCure to validate the licensing of the cloudcard integration and is usually provided by CloudCard.
- `CCureClient.compatibleCCureVersion`
  - The version of CCure that this integration can use - this is usually going to be provided by CloudCard.

### File Name Resolver Settings

- downloader.fileNameResolver
    - default: `SimpleFileNameResolver`
    - options:
        - `SimpleFileNameResolver` - uses the cardholder's identifier value as the file name
        - `DatabaseFileNameResolver` - executes select query to determine the file name
        - `CustomFieldFileNameResolver` - uses custom field values as the file name

#### DatabaseFileNameResolver Settings

- DatabaseFileNameResolver.baseFileName.query
    - default: none
    - description: Select query to get the base file name to which `.jpg` will be added
    - example:
        - `SELECT TOP 1 student_id FROM my_table WHERE external_id = ? AND other_column LIKE 'abc%' ORDER BY date_created DESC`
        - Note: the cardholder's `identifier` will inserted into the query to replace the `?` symbol

#### CustomFieldFileNameResolver Settings

- CustomFieldFileNameResolver.include
    - default: none
    - description: which custom field values should be used to name downloaded photos.
    - example: `CustomFieldFileNameResolver.include=Full Name`
        - Note: Separate multiple values by commas (no quotes needed).

- CustomFieldFileNameResolver.delimiter
    - default: none
    - description: option to specify a delimiter if you are using multiple custom fields (or at least one custom field
      and the identifier)
    - example: `CustomFieldFileNameResolver.delimiter=_`

### Pre-Processor Settings

Each photo is processed and potentially modified by the specified pre-processor after it is retrieved from CloudCard and
before it is saved by the
storage service

- downloader.preProcessor=DoNothingPreProcessor
    - default: `DoNothingPreProcessor`
    - description: specifies which pre-processor will be used to pre-process each photo
    - options:
        - `DoNothingPreProcessor` - placeholder service that makes no changes to the photo before storing it
        - `BytesLinkPreProcessor` - modifies the external URL from which the binary photo file is retrieved, AKA the
          Bytes Link

#### BytesLinkPreProcessor Settings

- BytesLinkPreprocessor.urlTemplate
    - default: none
    - description: This is the template to use for rewriting the bytes link. The photo's public key with replace the
      token `{publicKey}` if it exists in the template.
    - example: https://api.onlinephotosubmission.com/api/photos/{publicKey}/bytes

### Post-Processor Settings

Each downloaded photo is processed and potentially modified by the specified post-processor after it is saved by the
storage service and before it
marked as downloaded in CloudCard

- downloader.postProcessor
    - default: `DoNothingPostProcessor`
    - description: specifies which post-processor will be used to post-process each photo
    - options:
        - `DoNothingPostProcessor` - placeholder service that performs no actions
        - `DatabasePostProcessor` - executes a database query in response after downloading the photo
            - example: save the file path to the downloaded photo and the current timestamp to a database

#### DatabasePostProcessor Settings

- DatabasePostProcessor.override.photoFilePath
    - default: none
    - description: When saving the metadata about a photo, this file path is saved instead of the actual file path of
      the downloaded photo.
      The file name itself remains unchanged. Useful for network drives that may be mapped/mounted differently on
      different servers/workstations
- DatabasePostProcessor.query
    - default: none
    - description: This is the update/insert query that will update/insert into the DB
    - example: `UPDATE my_table SET date_created = ?, file_location = ? WHERE student_id = ?`
        - Note: use `?` symbols to indicate where parameters should be inserted
- DatabasePostProcessor.query.paramNames
    - default: none
    - description: these are the names of the parameters that will be passed into the update/insert query
    - options:
        - `identifier` - the cardholder's `idenitfier` field within CloudCard
        - `email` - the cardholder's `email` field within CloudCard
        - `fileName` - the full file name, including file path, of the downloaded photo
        - `timestamp` - the current timestamp
        - the exact name of any custom field within CloudCard
        - other options: `aspectRatio`, `publicKey`, `externalURL`, `status`, etc.
    - example: `timestamp,Notes,identifier`
        - Note: Order is important. The order in which the parameter names are listed must match the order in which they
          occur in `DatabasePostProcessor.query`
        - The current timestamp would be inserted in place of the first  `?`. The value of the `Notes` custom field from
          CloudCard will be
          inserted in place of the second `?`. The value of the CloudCard `identifer` field will be inserted in place of
          the third `?`.
- DatabasePostProcessor.query.paramTypes
    - default: none
    - description: these are the sql types of the parameters that will be passed into the update query
    - example: `TIMESTAMP,NVARCHAR,VARCHAR`
        - Note: Order is important. The order in which the parameter types are listed must match the order in which they
          occur in `DatabasePostProcessor.query`

#### AdditionalPhotoPostProcessor Settings

- AdditionalPhotoPostProcessor.include
    - description: which supporting document types should be downloaded.
    - example: `Signature,Government ID`
        - Note: Separate multiple values by commas.

### Summary Service Settings ([Video](https://youtu.be/u33vJqTPPAg))

- downloader.summaryService
    - default: `SimpleSummaryService`
    - description: After successfully downloading photos, the summary service prepares and saves a summary report
    - options:
        - `SimpleSummaryService` - adds a line to the summary file with the following format  
          `Mar-09 10:22 | Attempted: 2 | Succeeded: 2 | Failed: 0`
        - Note: currently there is only one option for Summary service
- SimpleSummaryService.fileName
    - default: none
    - description: this is the file name for the summary file, not including the file path.
    - note: if no file name is specified a new file will be generated each day named  
      `cloudcard-download-summary_yyyy-MM-dd.txt`
- SimpleSummaryService.directory
    - default: `summary`
    - description: this is the full or relative file path to the directory into which the summary file will be saved
    - note: if this directory does not exist, it will be created

### Encrypting application properties ([Video](https://video.drift.com/v/abZMpJLIB5O/))

-This method uses AES-256 to encrypt the run.sh file and any application properties that it contains

- note: this method only works on Linux and Mac

1. Download `encode-run-sh.sh` and `decrypt-and-execute-run-sh.sh` from the `Scripts` folder in this repository and add
   them to your downloader folder
2. Add any properties you want to encrypt (such as the access_token) to `run.sh` as command line parameters instead of
   in application.properties
3. Run `encode-run-sh.sh` and create a password
4. Delete your un-encrypted `run.sh` file
5. To run the downloader, run `decrypt-and-execute-run-sh.sh`. This will ask for your password and then execute the
   encrypted file, which runs the downloader.

## Warranty

THIS PROJECT IS DISTRIBUTED WITH NO WARRANTY. SEE THE [LICENSE](LICENSE) FOR FULL DETAILS.  
If your organization needs fully warrantied CloudCard integration software, consider Cloud Photo Connect
from [Vision Database Systems](http://www.visiondatabase.com/).

## Support

THIS PROJECT IS DISTRIBUTED WITHOUT ANY GUARANTEE OF SUPPORT FROM THE AUTHOR(S). SEE [LICENSE](LICENSE) FOR FULL
DETAILS.  
Separate support agreements are available, contact *info@OnlinePhotoSubmission.com* for more details.
