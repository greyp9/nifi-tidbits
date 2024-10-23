#!/bin/bash

DIST_SOURCE=https://dist.apache.org/repos/dist/dev/nifi/nifi-2.0.0

DIST_TARGET=~/Downloads/NIFI/2.0.0

BINARIES=(
  "minifi-2.0.0-bin.zip"
  "minifi-toolkit-2.0.0-bin.zip"
  "nifi-2.0.0-bin.zip"
  "nifi-2.0.0-source-release.zip"
  "nifi-registry-2.0.0-bin.zip"
  "nifi-registry-toolkit-2.0.0-bin.zip"
  "nifi-stateless-2.0.0-bin.zip"
  "nifi-toolkit-2.0.0-bin.zip"
)

EXTENSIONS=(
  "asc"
  "sha512"
)

mkdir $DIST_TARGET

for BINARY in "${BINARIES[@]}"; do
  echo $BINARY
  SOURCE=$DIST_SOURCE/$BINARY
  TARGET=$DIST_TARGET/$BINARY
  if [[ ! -f $TARGET ]]; then
    wget -O $TARGET $SOURCE
  fi

  for EXTENSION in "${EXTENSIONS[@]}"; do
    SOURCE_X=$SOURCE.$EXTENSION
    TARGET_X=$TARGET.$EXTENSION
    if [[ ! -f $TARGET_X ]]; then
      wget -O $TARGET_X $SOURCE_X
    fi
  done

  echo "$(cat $TARGET.sha512) $TARGET" | sha512sum --check
  gpg --verify $TARGET.asc
done
