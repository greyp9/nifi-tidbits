#!/bin/bash

DIST_SOURCE=https://dist.apache.org/repos/dist/dev/nifi/nifi-1.16.2
DIST_TARGET=~/Downloads/NIFI/1.16.2

BINARIES=(
  "minifi-1.16.2-bin.tar.gz"
  "minifi-1.16.2-bin.zip"
  "minifi-c2-1.16.2-bin.tar.gz"
  "minifi-c2-1.16.2-bin.zip"
  "minifi-toolkit-1.16.2-bin.tar.gz"
  "minifi-toolkit-1.16.2-bin.zip"
  "nifi-1.16.2-bin.tar.gz"
  "nifi-1.16.2-bin.zip"
  "nifi-1.16.2-source-release.zip"
  "nifi-registry-1.16.2-bin.tar.gz"
  "nifi-registry-1.16.2-bin.zip"
  "nifi-registry-toolkit-1.16.2-bin.tar.gz"
  "nifi-registry-toolkit-1.16.2-bin.zip"
  "nifi-stateless-1.16.2-bin.tar.gz"
  "nifi-toolkit-1.16.2-bin.tar.gz"
  "nifi-toolkit-1.16.2-bin.zip"
)

EXTENSIONS=(
  "asc"
  "sha256"
  "sha512"
)

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

  echo "$(cat $TARGET.sha256) $TARGET" | sha256sum --check
  echo "$(cat $TARGET.sha512) $TARGET" | sha512sum --check
  gpg --verify $TARGET.asc
done
