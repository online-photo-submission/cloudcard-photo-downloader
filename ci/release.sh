#!/usr/bin/env bash

set -e

version="$1"

if [ -z "$1" ]; then
  version="$(date +"%y.%m.%d.%H%M")"
fi

echo "Setting version to $version"

if [[ -n $(git status -s) ]]; then
  echo "Please make sure git is in a clean state before running this script."
  exit
fi

GRADLE_CONF="build.gradle"
PROP_CONF="src/main/groovy/com/cloudcard/photoDownloader/ApplicationPropertiesValidator.java"

BACKUP=".tmp"
sed -i"$BACKUP" -e "s/String version = .*$/String version = \"$version\";/" $PROP_CONF
sed -i"$BACKUP" -e "s/version = .*$/version = \'$version\'/" $GRADLE_CONF

rm "${PROP_CONF}$BACKUP"
rm "${GRADLE_CONF}$BACKUP"

# build
rm cloudcard-photo-downloader.*
./gradlew clean test build
./clean.sh

# package
mkdir cloudcard-photo-downloader
cp "build/libs/cloudcard-photo-downloader-$version.jar" "cloudcard-photo-downloader/cloudcard-photo-downloader.jar"
echo cloudcard.api.accessToken= >cloudcard-photo-downloader/application.properties
echo "java -jar cloudcard-photo-downloader.jar >> downloader.log" >> cloudcard-photo-downloader/run.bat
echo "java -jar cloudcard-photo-downloader.jar >> downloader.log" >cloudcard-photo-downloader/run.sh
echo "CloudCard Photo Downloader version $version" > cloudcard-photo-downloader/version.txt
chmod +x cloudcard-photo-downloader/run.sh
zip -r cloudcard-photo-downloader.zip cloudcard-photo-downloader
rm -rf cloudcard-photo-downloader

# commit
git add cloudcard-photo-downloader.zip
git add $PROP_CONF
git add $GRADLE_CONF

git commit -m "$version"
git tag "$version"
git push origin master
