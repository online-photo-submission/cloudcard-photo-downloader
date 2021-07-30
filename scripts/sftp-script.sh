USER=
HOST=
DESTINATION_DIRECTORY=photo
KEY="path to pem file"
FILE=put-jpgs.sh
PHOTO_DIRECTORY=downloaded-photos

set -e

if test ! -f $FILE; then
    echo "put """$PHOTO_DIRECTORY"""/*.jpg" > "$FILE"
fi

sftp -i "$KEY" -b "$FILE" sftp://$USER@$HOST/$DESTINATION_DIRECTORY

foldername=$(date +%Y.%m.%d_%H.%M.%S)
mkdir -p  "$foldername"
mv ""$PHOTO_DIRECTORY""/*.jpg "$foldername"
