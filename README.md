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
- Outbound network access to the following domains/ports if your organization requires all outbound traffic to be
  whitelisted
    - api.onlinephotosubmission.com:443
    - s3-us-east-2.amazonaws.com:443
         #### Alternate Domains
        
        - api.cloudcard.ca:443 (CloudCard's Canada instance)
        - s3-ca-central-1.amazonaws.com:443 (CloudCard's Canada instance)
        - test-api.onlinephotosubmission.com:443 (CloudCard's test instance)
        - onlinephoto-api.transactcampus.net:443 (Online Photo Submission through Transact)
    
To test your system, run `java -version`. The output should look like the following. The exact version isn't important
as long as it starts with `17`.
```
openjdk version "17.0.6" 2023-01-17 LTS
OpenJDK Runtime Environment Corretto-17.0.6.10.1 (build 17.0.6+10-LTS)
OpenJDK 64-Bit Server VM Corretto-17.0.6.10.1 (build 17.0.6+10-LTS, mixed mode, sharing)
```

## Network Diagram

[![Network Diaram](https://online-photo-submission.github.io/cloudcard-photo-downloader/downloader-diagram.png)](https://online-photo-submission.github.io/cloudcard-photo-downloader/downloader-diagram.png)

## Installation and Configuration

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
        - `SqsPhotoService` - (RECOMMENDED) retrieves the photo data from an AWS SQS queue in near-realtime. Please contact support@cloudcard.us to have a queue configured in AWS.
        - `CloudCardPhotoService` - Legacy option that retrieves the photo data from the CloudCard API directly no more often than every 10 minutes. This option may be helpful if managing the Downloader with the Windows Task Scheduler.
- downloader.storageService
    - default: `FileStorageService`
    - description: this setting determines how the downloaded photos will be stored
    - options:
        - `FileStorageService` - stores images as jpeg files on the local or network file system
        - `DatabaseStorageService` - stores the jpeg encoded images as `BLOBs` in a relational database
        - `IntegrationStorageService` - sends images to an internet-accessible API. Currently only supports Genetec ClearID.
        - `TouchNetStorageService` - sends images to a TouchNet API.
        - `WorkdayStorageService` - sends images to a Workday instance.
- downloader.repeat
    - default: `true`
    - description: This setting determines if the downloader will run once and exit, `downloader.repeat=false`, or if
      will run continually, `downloader.repeat=true`
- downloader.scheduling.type
    - default: `fixedDelay` 
        - the downloader runs intermittently by default on a fixed delay, which can be modified with `downloader.delay.milliseconds`
    - other options:
        - `cron` allows precise scheduling using cron expressions
        -  note: If scheduling type is `cron`, you must also specify a cron expression with `downloader.cron.schedule` 
- downloader.delay.milliseconds
    - default: `600000` (Ten Minutes)
    - description: this is the amount of time the downloader will wait between download attempts.
    - note: `downloader.repeat` must be set to `true` and scheduling type to `fixedDelay` for this setting to have any effect.
- downloader.cron.schedule
    - description: specifies the cron expression that the downloader will run on if `downloader.scheduling.type` is set to `cron`. 
    - example: `downloader.cron.schedule=0 0 12 * * *` (This configuration runs the downloader once a day at 12:00pm)
- downloader.minPhotoIdLength
    - default: `0`
    - description: This setting causes photo IDs to be left padded with zeros (0) until they have at least this many
      digits.

#### SqsPhotoService Settings

- sqsPhotoService.queueUrl
    - default: none
    - note: This will be provided by support once configured upon request. 
- aws.sqs.region
    - default: `ca-central-1`
    - description: the AWS region in which the SQS queue is located. This will be provided by support once configured upon request.
- sqsPhotoService.pollingIntervalSeconds
    - default: 0
    - description: how long to wait between SQS requests for new messages
- sqsPhotoService.pollingDurationSeconds
    - default: 20
    - description: used to configure the duration of [SQS Long Polling](https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-short-and-long-polling.html)
- sqsPhotoService.putStatus
    - default: none
    - description: determines what status the photo is set to after downloading.
    - note: if this is not set, the photo status will not be updated in CloudCard after downloading the photo with the SQS integration.
    - note: this option requires the CloudCardClient settings to be configured.

#### CloudCardPhotoService Settings

*Note: CloudCardPhotoService also requires CloudCardClient settings to be configured*

- downloader.fetchStatuses
    - default: `READY_FOR_DOWNLOAD`
    - allowed values: `PENDING`,`APPROVED`,`DENIED`,`READY_FOR_DOWNLOAD`,`DOWNLOADED`,`DISCARDED`,`DONE`
    - description: Photos with these statuses will be downloaded. Separate statuses with a comma.
- downloader.putStatus
    - default: `DOWNLOADED`
    - allowed values: `PENDING`,`APPROVED`,`DENIED`,`READY_FOR_DOWNLOAD`,`DOWNLOADED`,`DISCARDED`,`DONE`
    - description: Downloaded photos will be marked with this status in the CloudCard web application.

#### CloudCardClient Settings

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
 
#### Proxy Settings
The downloader supports going through a proxy when using the CloudCardPhotoService to download photos to the file system.
- proxy.host
    - default: `null`
- proxy.port
    - default: `0`  

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
- db.photoUpdates.enabled
    - default: `true`
    - description: This boolean determines if downloaded photos will update existing cardholder photos in the database. If a cardholder doesn't yet exist in the database, this will default to an `INSERT` statement and write a new row to the database.
    - note: If this is set to `false`, downloaded photos will **always** be written to a new row.

##### Database Connection Settings ([Video](https://youtu.be/JeykYIykI6k))

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

#### ClearId Integration Settings

*Note: `downloader.storageService` must be set to `IntegrationStorageService`, and `IntegrationStorageService.client` must be set to `ClearIdClient` for these to have any effect.*

Note you will need to create an API integration in the Administration section of ClearID.

- `ClearIdClient.apiUrl`
    - The versioned identity api url for ClearID.
    - example: `https://api.demo.clearid.io/identity/api/v4`
- `ClearIdClient.stsUrl`
    - The STS service url for ClearID.
    - example: `https://sts-demo.clearid.io`
- `ClearIdClient.accountId`
    - Your organization's accountID in ClearID. 
    - Note: You can find this in the url of your ClearID instance, e.g. `https://demo.clearid.io/<account_id>/administration/service-principals`
- `ClearIdClient.clientId`
    - The ClientID for the ClearID API Integration Key.
    - Note: This information is only available one time, when you generate a key for an API integration in ClearID 
- `ClearIdClient.clientSecret`
    - The ClientSecret for the ClearID API Integration Key.
    - Note: This information is only available one time, when you generate a key for an API integration in ClearID

#### TouchNet Storage Service Settings

*Note: `downloader.storageService` must be set to `TouchNetStorageService` for these to have any effect.*

- `TouchNetClient.apiUrl`
    - Internet accessible URL where the target OneCard API is located. 
- `TouchNetClient.operatorId`
    - username of an Operator level account that the Downloader can use when uploading photos into TouchNet.
- `TouchNetClient.operatorPassword`
    - password of the account identified by `TouchNetClient.operatorId`
- `TouchNetClient.terminalId`
    - Terminal ID that should be assigned to this CloudCard downloader.
- `TouchNetClient.terminalType`
    - default: `ThirdParty`
- `TouchNetClient.developerKey`
    - provided by CloudCard
- `TouchNetClient.originId`
    - provided by CloudCard

#### Workday Storage Service Settings

*Note: `downloader.storageService` must be set to `WorkdayStorageService` for these to have any effect.*

- `WorkdayClient.apiUrl`
    - URL, including TLD, but no other path, where the Workday API is located.
    - Obtain by running the Public Web Services report. From related actions of Human Resources (public), select Web Service -> View WSDL.
      - At the bottom of the WSDL, you will see an xml tag that looks like this: 
        ```xml
        <soapbind:address location="https://impl-services1.wd12.myworkday.com/ccx/service/tenant_name/Human_Resources/v45.0"/>
        ```
    - Example: `https://impl-services1.wd12.myworkday.com`
- `WorkdayClient.tenantName`
    - tenant name, which is the value after `service/` in the WSDL URL.
- `WorkdayClient.isu.username`
    - username of an Integration System User account that has, at minimum, `Put` permissions on the `Personal Data: Personal Photo` domain.
- `WorkdayClient.isu.password`
    - password of the account identified by `WorkdayClient.isu.username`

### File Name Resolver Settings

- downloader.fileNameResolver
    - default: `SimpleFileNameResolver`
    - options:
        - `SimpleFileNameResolver` - uses the cardholder's identifier value as the file name
        - `DatabaseFileNameResolver` - executes select query to determine the file name
        - `CustomFieldFileNameResolver` - uses custom field values as the file name
        > **⚠️ DEPRECATED:** This resolver is deprecated. Use `DynamicFileNameResolver` instead, which supports more options.
        - `DynamicFileNameResolver` - uses fields such as custom fields and identifier to create the file name

#### DatabaseFileNameResolver Settings

- DatabaseFileNameResolver.baseFileName.query
    - default: none
    - description: Select query to get the base file name.
        - If using the FileStorageService, this will have `.jpg` added to it.
        - If using the DatabaseStorageService, this is the value that will be written to the `studentId` column in your database. This column can be specified in the DatabaseStorageService settings.
    - example:
        - `SELECT TOP 1 student_id FROM my_table WHERE external_id = ? AND other_column LIKE 'abc%' ORDER BY date_created DESC`
        - Note: the cardholder's `identifier` will inserted into the query to replace the `?` symbol

#### DynamicFileNameResolver Settings

- DynamicFileNameResolver.include
    - default: none
    - description: which field values should be used to name downloaded photos.
    - example: `DynamicFileNameResolver.include=identifier,Full Name`
        - Note: Separate multiple values by commas (no quotes needed).
        - Options:
            - any required custom field name in the user's organization (i.e. `First Name`, `Last Name`, etc.)
            - `identifier` - the cardholder's identifier field within CloudCard
            - `dateCreated` - the timestamp of when the photo was submitted

- DynamicFileNameResolver.delimiter
    - default: none
    - description: option to specify a delimiter if you are using multiple fields
    - example: `DynamicFileNameResolver.delimiter=_`

- DynamicFileNameResolver.dateFormat
    - default: `yyyy-MM-dd_HH-mm-ss`
    - description: Specifies the format to use for the `dateCreated` field when it is included in the file name.
    - example: `DynamicFileNameResolver.dateFormat=yyyy-MM-dd_HH-mm-ss`
        - Uses [Java DateTimeFormatter patterns](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/format/DateTimeFormatter.html).

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
        - `dateCreatedTimestamp` - the timestamp of when the photo was submitted
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

*Note: this post processor requires the CloudCardClient to be configured.*

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

### Remote Logging Settings

If you would like to send logs to CloudCard for remote support, you can specify the following config properties. This behavior is optional and off by default.

- `logging.appender.papertrail.level`
    - default: `OFF`
    - options: `TRACE`, `DEBUG`, `ERROR`, `WARN`, `INFO`, `OFF`
- `cloudcard.logging.identity`
    - CloudCard Support will provide you with a value for this configuration
- `cloudcard.logging.host`
    - CloudCard Support will provide you with a value for this configuration
- `cloudcard.logging.port`
    - CloudCard Support will provide you with a value for this configuration

### ManifestFileService
The ManifestFileService allows you to generate a file with information about each photo that has been downloaded. This service runs each time the downloader successfully downloads photos.

- `ManifestFileService`
    - default: `DoNothingManifestFileService`
    - description: Does nothing
    - Other options:
        - `ManifestFileService=CSVManifestFileService`
        - description: Generates a CSV file.

### CSVManifestFileService Settings

The `CSVManifestFileService` is responsible for generating a CSV file containing information about each photo that has been downloaded. This service runs each time the downloader successfully downloads photos. Below are the settings you can configure for `CSVManifestFileService`:

- `fileName`
    - Description: Specifies the base name for the generated CSV manifest file. If not set, a default name will be used.
    - Default Value: `manifest`
- `fileNameDateFormat`
    - Description: If specified, this defines the date format to append to the `fileName` for timestamping. Uses Java's [SimpleDateFormat](https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html).
    - Default Value: `null`
- `directory`
    - Description: The directory where the CSV manifest file will be saved.
    - Default Value: `downloaded-photos`
- `delimiter`
    - Description: The delimiter character used to separate values in the CSV file.
    - Default Value: `,`
- `quoteMode`
    - Description: The quoting mode used when generating the CSV file. Corresponds to [Apache Commons CSV QuoteMode enum](https://commons.apache.org/proper/commons-csv/apidocs/org/apache/commons/csv/QuoteMode.html).
    - Default Value: `ALL_NON_NULL`
- `quoteCharacter`
    - Description: The character used for quoting in the CSV file. Only effective if `doubleQuoteValues` is `true`.
    - Default Value: `"`
- `escapeCharacter`
    - Description: The escape character used in the CSV file.
    - Default Value: `null`
- `dateFormat`
    - Description: The format used for date values in the CSV file. Uses Java's [SimpleDateFormat](https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html).
    - Default Value: `null`
- `headerAndColumnMap`
    - Description: A map defining the CSV file headers and the corresponding object properties to use for their values. Values available are derived from the [Photo class](https://github.com/online-photo-submission/cloudcard-photo-downloader/blob/master/src/main/groovy/com/cloudcard/photoDownloader/Photo.groovy)
    - Default Value: `{'Cardholder_ID':'person.identifier','Photo_Status': 'status','Photo_Date_Submitted':'dateCreated'}`
        - In order to pass custom field values in, use `'person.customFields.full_custom_field_name'` (i.e. `'person.customFields.First Name'`)
        - Optional: You can specify a static field in this map by prefixing the value with `static_`.
        - Example: `'Source':'static_CloudCard'` 
        - Optional: You can specify the photo file name with the value `photo_fileName` OR the full photo file path with `photo_fullFilePath`.
        - Examples: `'Photo':'photo_fileName`, `'Photo Path':'photo_fullFilePath'`
        - NOTE: If downloading photos to multiple directories , the fullFilePath will always reference the first directory listed in `downloader.photoDirectories`.

### Encrypting application properties

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

