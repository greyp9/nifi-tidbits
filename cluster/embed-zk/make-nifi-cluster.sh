#!/bin/bash

REAL_PATH=$(realpath $0)
SCRIPT_DIR=$(dirname $REAL_PATH)
SCRIPT_NAME=$(basename $REAL_PATH)

#echo REAL_PATH=$REAL_PATH
#echo SCRIPT_DIR=$SCRIPT_DIR
#echo SCRIPT_NAME=$SCRIPT_NAME
#echo SHELL=$SHELL

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

SAXON_FOLDER_GLOB=$HOME"/.m2/repository/net/sf/saxon/Saxon-HE/10.6*"
SAXON_FOLDER_LIST=($SAXON_FOLDER_GLOB)
SAXON_LIST_LENGTH=${#SAXON_FOLDER_LIST[@]}
SAXON_FOLDER_LATEST=${SAXON_FOLDER_LIST[$SAXON_LIST_LENGTH - 1]}
SAXON_JAR=$SAXON_FOLDER_LATEST"/Saxon-HE-10.*.jar"

if [ $SHELL = "/bin/sh" ]
then
  SAXON="java -jar $SAXON_JAR"
  SED="sed -i"
elif [ $SHELL = "/bin/bash" ]
then
  SAXON="java -jar $SAXON_JAR"
  SED="sed -i"
elif [ $SHELL = "/bin/zsh" ]
then
  SAXON="java -jar $SAXON_JAR"
  # https://unix.stackexchange.com/questions/401905/bsd-sed-vs-gnu-sed-and-i
  SED="sed -i ''"
else
  echo unknown environment - bin/sh - bin/bash - bin/zsh
  exit 1
fi

NIFI_IMAGE_DIR=$NIFI_ROOT"/nifi-assembly/target/nifi*bin/nifi*"

# evaluate $NIFI_BIN_DIR so "mkdir $NIFI_CLUSTER_DIR" will work
NIFI_BIN_DIR_GLOB=$NIFI_ROOT"/nifi-assembly/target/nifi*bin"
NIFI_BIN_DIR=$(echo $NIFI_BIN_DIR_GLOB)
NIFI_CLUSTER_DIR=$NIFI_BIN_DIR"/cluster"
NIFI_OPENSSL_DIR=$NIFI_BIN_DIR"/openssl"
NIFI_OPENSSL_PASSPHRASE=123456

#echo NIFI_IMAGE_DIR=$NIFI_IMAGE_DIR
#echo NIFI_BIN_DIR=$NIFI_BIN_DIR
#echo NIFI_CLUSTER_DIR=$NIFI_CLUSTER_DIR

rm -rf $NIFI_CLUSTER_DIR
mkdir $NIFI_CLUSTER_DIR
for n in `seq 1 $NIFI_NODES`; do

  echo NIFI_CLUSTER_DIR_IT=$NIFI_CLUSTER_DIR/nifi$n

  cp -R $NIFI_IMAGE_DIR $NIFI_CLUSTER_DIR/nifi$n
  cp -R $NIFI_OPENSSL_DIR/nifi$n/*.* $NIFI_CLUSTER_DIR/nifi$n/conf

  $SED 's/nifi.cluster.flow.election.max.candidates=/nifi.cluster.flow.election.max.candidates='"$NIFI_NODES"'/g' $NIFI_CLUSTER_DIR/nifi$n/conf/nifi.properties
  $SED "s/nifi.cluster.flow.election.max.wait.time=5 mins/nifi.cluster.flow.election.max.wait.time=2 min/g" $NIFI_CLUSTER_DIR/nifi$n/conf/nifi.properties
  $SED "s/nifi.cluster.is.node=false/nifi.cluster.is.node=true/g" $NIFI_CLUSTER_DIR/nifi$n/conf/nifi.properties
  $SED "s/nifi.cluster.protocol.is.secure=false/nifi.cluster.protocol.is.secure=true/g" $NIFI_CLUSTER_DIR/nifi$n/conf/nifi.properties
  $SED "s/nifi.cluster.load.balance.host=/nifi.cluster.load.balance.host=localhost/g" $NIFI_CLUSTER_DIR/nifi$n/conf/nifi.properties
  $SED "s/nifi.cluster.load.balance.port=6342/nifi.cluster.load.balance.port=634${n}/g" $NIFI_CLUSTER_DIR/nifi$n/conf/nifi.properties
  $SED "s/nifi.cluster.node.address=/nifi.cluster.node.protocol.address=localhost/g" $NIFI_CLUSTER_DIR/nifi$n/conf/nifi.properties
  $SED "s/nifi.cluster.node.protocol.port=/nifi.cluster.node.protocol.port=1144${n}/g" $NIFI_CLUSTER_DIR/nifi$n/conf/nifi.properties
  $SED "s/nifi.remote.input.host=/nifi.remote.input.host=localhost/g" $NIFI_CLUSTER_DIR/nifi$n/conf/nifi.properties
  $SED "s/nifi.remote.input.socket.port=/nifi.remote.input.socket.port=1044${n}/g" $NIFI_CLUSTER_DIR/nifi$n/conf/nifi.properties
  $SED "s/nifi.sensitive.props.key=/nifi.sensitive.props.key=this-is-my-sensitive-props-key/g" $NIFI_CLUSTER_DIR/nifi$n/conf/nifi.properties
  $SED "s/nifi.state.management.embedded.zookeeper.start=false/nifi.state.management.embedded.zookeeper.start=true/g" $NIFI_CLUSTER_DIR/nifi$n/conf/nifi.properties
  $SED "s/nifi.web.https.host=127.0.0.1/nifi.web.https.host=localhost/g" $NIFI_CLUSTER_DIR/nifi$n/conf/nifi.properties
  $SED "s/nifi.web.https.port=8443/nifi.web.https.port=944${n}/g" $NIFI_CLUSTER_DIR/nifi$n/conf/nifi.properties
  $SED "s/nifi.security.truststorePasswd=/nifi.security.truststorePasswd=$NIFI_OPENSSL_PASSPHRASE/g" $NIFI_CLUSTER_DIR/nifi$n/conf/nifi.properties
  $SED "s/nifi.security.keystorePasswd=/nifi.security.keystorePasswd=$NIFI_OPENSSL_PASSPHRASE/g" $NIFI_CLUSTER_DIR/nifi$n/conf/nifi.properties
  $SED "s/nifi.security.keyPasswd=/nifi.security.keyPasswd=$NIFI_OPENSSL_PASSPHRASE/g" $NIFI_CLUSTER_DIR/nifi$n/conf/nifi.properties

  # nifi properties

  ZK_CONNECT_STRING_VALUE=
  for m in `seq 1 $NIFI_NODES`; do
    if [ "$ZK_CONNECT_STRING_VALUE" != "" ]
    then
      ZK_CONNECT_STRING_VALUE+=","
    fi
    # standard port is 2181; use a non-standard port to avoid conflicts with another zookeeper
    ZK_CONNECT_STRING_VALUE+="localhost:219$m"
  done
  $SED 's/nifi.zookeeper.connect.string=/nifi.zookeeper.connect.string='"$ZK_CONNECT_STRING_VALUE"'/g' $NIFI_CLUSTER_DIR/nifi$n/conf/nifi.properties

  # zookeeper config

  # add lines for each node
  for m in `seq $NIFI_NODES -1 2`; do
    REPLACE_ADD_SERVER_LINE='server.1=\'$'\n''server.'$m'='
    $SED 's/^server.1=/'"$REPLACE_ADD_SERVER_LINE"'/g' $NIFI_CLUSTER_DIR/nifi$n/conf/zookeeper.properties
  done
  # set the values for each node
  for m in `seq 1 $NIFI_NODES`; do
    $SED 's/^server.'"$m"'=/server.'"$m"'=localhost:288'"$m"':388'"$m"';219'"$m"'/g' $NIFI_CLUSTER_DIR/nifi$n/conf/zookeeper.properties
  done

  mkdir -p $NIFI_CLUSTER_DIR/nifi$n/state/zookeeper
  echo $n >> $NIFI_CLUSTER_DIR/nifi$n/state/zookeeper/myid
  mkdir -p $NIFI_CLUSTER_DIR/nifi$n/state/zookeeper/version-2

  ZK_STATE_PROVIDER_VALUE=""
  for m in `seq 1 $NIFI_NODES`; do
    if [ "$ZK_STATE_PROVIDER_VALUE" != "" ]
    then
      ZK_STATE_PROVIDER_VALUE+=","
    fi
    ZK_STATE_PROVIDER_VALUE+="localhost:219$m"
  done
  $SED 's/></>'"$ZK_STATE_PROVIDER_VALUE"'</g' $NIFI_CLUSTER_DIR/nifi$n/conf/state-management.xml

  # https://formulae.brew.sh/formula/saxon
  $SAXON n=$n -xsl:"$SCRIPT_DIR/authorizers.xslt" -s:"$NIFI_CLUSTER_DIR/nifi$n/conf/authorizers.xml" -o:"$NIFI_CLUSTER_DIR/nifi$n/conf/authorizers.xml"
done

# cluster control scripts
echo "#!/bin/bash" > $NIFI_CLUSTER_DIR/start.sh
echo "#!/bin/bash" > $NIFI_CLUSTER_DIR/stop.sh
for m in `seq 1 $NIFI_NODES`; do
  echo "$NIFI_CLUSTER_DIR/nifi$m/bin/nifi.sh start" >> $NIFI_CLUSTER_DIR/start.sh
  echo "$NIFI_CLUSTER_DIR/nifi$m/bin/nifi.sh stop" >> $NIFI_CLUSTER_DIR/stop.sh
done
chmod +x $NIFI_CLUSTER_DIR/start.sh
chmod +x $NIFI_CLUSTER_DIR/stop.sh
