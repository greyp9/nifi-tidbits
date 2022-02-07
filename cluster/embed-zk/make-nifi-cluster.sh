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

if [ $SHELL = "/bin/sh" ]
then
  SAXON="java -jar ~/.m2/repository/net/sf/saxon/Saxon-HE/10.6/Saxon-HE-10.6.jar"
  SED="sed -i"
elif [ $SHELL = "/bin/bash" ]
then
  SAXON="java -jar ~/.m2/repository/net/sf/saxon/Saxon-HE/10.6/Saxon-HE-10.6.jar"
  SED="sed -i"
elif [ $SHELL = "/bin/zsh" ]
then
  # https://formulae.brew.sh/formula/saxon
  SAXON="saxon"
  # https://unix.stackexchange.com/questions/401905/bsd-sed-vs-gnu-sed-and-i
  SED="sed -i ''"
else
  echo unknown environment - bin/sh - bin/bash - bin/zsh
  exit 1
fi

NIFI_TOOLKIT_DIR=$NIFI_ROOT"/nifi-toolkit/nifi-toolkit-assembly/target/nifi-toolkit*bin/nifi-toolkit*"
NIFI_IMAGE_DIR=$NIFI_ROOT"/nifi-assembly/target/nifi*bin/nifi*"

# evaluate $NIFI_BIN_DIR so "mkdir $NIFI_CLUSTER_DIR" will work
NIFI_BIN_DIR_GLOB=$NIFI_ROOT"/nifi-assembly/target/nifi*bin"
NIFI_BIN_DIR=$(echo $NIFI_BIN_DIR_GLOB)
NIFI_CLUSTER_DIR=$NIFI_BIN_DIR"/cluster"

#cd $NIFI_TOOLKIT_DIR && ./bin/tls-toolkit.sh standalone -n 'nifi[1-3]' -C 'CN=nifi' -c 'ca.nifi' -O
cd $NIFI_TOOLKIT_DIR && ./bin/tls-toolkit.sh standalone -n 'localhost' -C 'CN=nifi' -c 'ca.nifi' -O

rm -rf $NIFI_CLUSTER_DIR
mkdir $NIFI_CLUSTER_DIR
for n in `seq 1 $NIFI_NODES`; do

  cp -R $NIFI_IMAGE_DIR $NIFI_CLUSTER_DIR/nifi$n
  # cp -R $NIFI_TOOLKIT_DIR/nifi$n/*.* $NIFI_CLUSTER_DIR/nifi$n/conf
  cp -R $NIFI_TOOLKIT_DIR/localhost/*.* $NIFI_CLUSTER_DIR/nifi$n/conf

  sed -i '' 's/nifi.cluster.flow.election.max.candidates=/nifi.cluster.flow.election.max.candidates='"$NIFI_NODES"'/g' $NIFI_CLUSTER_DIR/nifi$n/conf/nifi.properties
  sed -i '' "s/nifi.cluster.flow.election.max.wait.time=5 mins/nifi.cluster.flow.election.max.wait.time=2 min/g" $NIFI_CLUSTER_DIR/nifi$n/conf/nifi.properties
  sed -i '' "s/nifi.cluster.is.node=false/nifi.cluster.is.node=true/g" $NIFI_CLUSTER_DIR/nifi$n/conf/nifi.properties
  sed -i '' "s/nifi.cluster.load.balance.host=/nifi.cluster.load.balance.host=localhost/g" $NIFI_CLUSTER_DIR/nifi$n/conf/nifi.properties
  sed -i '' "s/nifi.cluster.load.balance.port=6342/nifi.cluster.load.balance.port=634${n}/g" $NIFI_CLUSTER_DIR/nifi$n/conf/nifi.properties
  sed -i '' "s/nifi.cluster.node.address=nifi${n}/nifi.cluster.node.protocol.address=localhost/g" $NIFI_CLUSTER_DIR/nifi$n/conf/nifi.properties
  sed -i '' "s/nifi.cluster.node.protocol.port=11443/nifi.cluster.node.protocol.port=1144${n}/g" $NIFI_CLUSTER_DIR/nifi$n/conf/nifi.properties
  sed -i '' "s/nifi.remote.input.socket.port=10443/nifi.remote.input.socket.port=1044${n}/g" $NIFI_CLUSTER_DIR/nifi$n/conf/nifi.properties
  sed -i '' "s/nifi.sensitive.props.key=/nifi.sensitive.props.key=this-is-my-sensitive-props-key/g" $NIFI_CLUSTER_DIR/nifi$n/conf/nifi.properties
  sed -i '' "s/nifi.state.management.embedded.zookeeper.start=false/nifi.state.management.embedded.zookeeper.start=true/g" $NIFI_CLUSTER_DIR/nifi$n/conf/nifi.properties
  sed -i '' "s/nifi.web.https.host=nifi${n}/nifi.web.https.host=localhost/g" $NIFI_CLUSTER_DIR/nifi$n/conf/nifi.properties
  sed -i '' "s/nifi.web.https.port=9443/nifi.web.https.port=944${n}/g" $NIFI_CLUSTER_DIR/nifi$n/conf/nifi.properties

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
  sed -i '' 's/nifi.zookeeper.connect.string=/nifi.zookeeper.connect.string='"$ZK_CONNECT_STRING_VALUE"'/g' $NIFI_CLUSTER_DIR/nifi$n/conf/nifi.properties

  # zookeeper config

  # add lines for each node
  for m in `seq $NIFI_NODES -1 2`; do
    REPLACE_ADD_SERVER_LINE='server.1=\'$'\n''server.'$m'='
    sed -i '' 's/^server.1=/'"$REPLACE_ADD_SERVER_LINE"'/g' $NIFI_CLUSTER_DIR/nifi$n/conf/zookeeper.properties
  done
  # set the values for each node
  for m in `seq 1 $NIFI_NODES`; do
    sed -i '' 's/^server.'"$m"'=/server.'"$m"'=localhost:288'"$m"':388'"$m"';219'"$m"'/g' $NIFI_CLUSTER_DIR/nifi$n/conf/zookeeper.properties
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
  sed -i '' 's/\>\</\>'"$ZK_STATE_PROVIDER_VALUE"'\</g' $NIFI_CLUSTER_DIR/nifi$n/conf/state-management.xml

  # https://formulae.brew.sh/formula/saxon
  saxon n=$n -xsl:"$SCRIPT_DIR/authorizers.xslt" -s:"$NIFI_CLUSTER_DIR/nifi$n/conf/authorizers.xml" -o:"$NIFI_CLUSTER_DIR/nifi$n/conf/authorizers.xml"
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
