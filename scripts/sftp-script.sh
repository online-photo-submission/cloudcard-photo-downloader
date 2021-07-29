HOST=
FILE=put-jpgs.sh
KEY="path to pem file"
DESTINATION_DIRECTORY=photo
PHOTO_DIRECTORY=/photo-test
PORT=
USER=

set -e

if test ! -f $FILE; then
    echo "put """$PHOTO_DIRECTORY"""/*.jpg" > "$FILE"
fi

sftp -i "$KEY" -b "$FILE" sftp://$USER@$HOST/$DESTINATION_DIRECTORY

foldername=$(date +%Y.%m.%d_%H.%M.%S)
mkdir -p  "$foldername"
mv ""$PHOTO_DIRECTORY""/*.jpg "$foldername"
