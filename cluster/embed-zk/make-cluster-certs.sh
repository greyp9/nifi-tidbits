#!/bin/bash

REAL_PATH=$(realpath $0)
SCRIPT_DIR=$(dirname $REAL_PATH)
SCRIPT_NAME=$(basename $REAL_PATH)

NIFI_ROOT=$1
if [ "$NIFI_ROOT" = "" ]; then
  echo usage: $SCRIPT_NAME [nifi_root_dir]
  exit 1
fi
NIFI_NODES=$2
if [ "$NIFI_NODES" = "" ]; then
  echo usage: $SCRIPT_NAME [nifi_root_dir] [number_of_nodes 1-9]
  exit 1
fi

# evaluate $NIFI_BIN_DIR so "mkdir $NIFI_CLUSTER_DIR" will work
NIFI_BIN_DIR_GLOB=$NIFI_ROOT"/nifi-assembly/target/nifi*bin"
NIFI_BIN_DIR=$(echo $NIFI_BIN_DIR_GLOB)
NIFI_OPENSSL_DIR=$NIFI_BIN_DIR"/openssl"

OPENSSL="openssl"
$OPENSSL version

rm -rf $NIFI_OPENSSL_DIR
mkdir $NIFI_OPENSSL_DIR

PASSPHRASE=123456

# generate truststore
openssl genrsa -aes256 -passout pass:$PASSPHRASE -out $NIFI_OPENSSL_DIR/ca.key 3072
openssl req -new -x509 -days 1461 -key $NIFI_OPENSSL_DIR/ca.key -passin pass:$PASSPHRASE -sha256 -out $NIFI_OPENSSL_DIR/ca.cer -subj "/CN=nifi-ca/OU=nifi/"
keytool -importcert -noprompt -keystore $NIFI_OPENSSL_DIR/truststore.p12 -storetype PKCS12 -storepass $PASSPHRASE -keypass $PASSPHRASE -file $NIFI_OPENSSL_DIR/ca.cer -alias 1

# generate keystores
for N in `seq 1 $NIFI_NODES`; do
  mkdir $NIFI_OPENSSL_DIR/nifi$N
  # key
  openssl genrsa -aes256 -passout pass:$PASSPHRASE -out $NIFI_OPENSSL_DIR/nifi$N.key 3072
  #openssl rsa -in $NIFI_OPENSSL_DIR/nifi$N.key -passin pass:$PASSPHRASE -check  // optionally verify key

  # certificate chain
  openssl req -new -key $NIFI_OPENSSL_DIR/nifi$N.key -passin pass:$PASSPHRASE -out $NIFI_OPENSSL_DIR/nifi$N.csr -subj "/CN=nifi$N/OU=nifi/" -addext "subjectAltName = DNS:localhost"
  openssl x509 -req -in $NIFI_OPENSSL_DIR/nifi$N.csr -CA $NIFI_OPENSSL_DIR/ca.cer -CAkey $NIFI_OPENSSL_DIR/ca.key -passin pass:$PASSPHRASE -CAcreateserial -out $NIFI_OPENSSL_DIR/nifi$N.cer -days 365 -sha256 -copy_extensions=copyall
  #cat $NIFI_OPENSSL_DIR/ca.cer $NIFI_OPENSSL_DIR/nifi1.cer >$NIFI_OPENSSL_DIR/nifi1.chain.cer  // order?
  cat $NIFI_OPENSSL_DIR/nifi$N.cer $NIFI_OPENSSL_DIR/ca.cer >$NIFI_OPENSSL_DIR/nifi$N.chain.cer

  # keystore
  openssl pkcs12 -export -out $NIFI_OPENSSL_DIR/nifi$N/keystore.p12 -passout pass:$PASSPHRASE -inkey $NIFI_OPENSSL_DIR/nifi$N.key -passin pass:$PASSPHRASE -in $NIFI_OPENSSL_DIR/nifi$N.chain.cer
  #openssl pkcs12 -info -noout -in $NIFI_OPENSSL_DIR/nifi$N.p12 -passin pass:$PASSPHRASE  // optionally verify keystore
  cp $NIFI_OPENSSL_DIR/truststore.p12 $NIFI_OPENSSL_DIR/nifi$N
done
