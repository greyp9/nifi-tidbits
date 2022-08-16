#!/bin/bash

PATTERN_URL_SOURCE="https://archive.apache.org/dist/nifi/VERSION/nifi-VERSION-bin.zip"
FOLDER_TARGET="."

NIFI_VERSIONS=(
  "1.13.2"
  "1.14.0"
  "1.15.3"
  "1.16.3"
  "1.17.0"
)

for NIFI_VERSION in "${NIFI_VERSIONS[@]}"; do
  echo $NIFI_VERSION
  URL="${PATTERN_URL_SOURCE//VERSION/$NIFI_VERSION}"
  FILENAME=$(basename $URL)

  # download archive (if not already downloaded)
  if [[ ! -f $FILENAME ]]; then
    wget $URL
  fi

  # unzip (if not already unzipped)
  if [[ ! -d $VERSION ]]; then
    unzip $FILENAME 'nifi-'$NIFI_VERSION'/docs/*'
  fi

done
