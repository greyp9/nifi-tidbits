# Creating a Development NiFi Cluster

### NiFi project
- [nifi (github.com)](https://github.com/apache/nifi)

### Prerequisites
- Java Development Kit (8 or 11)
- Maven (3.6+)
- (for MacOS) Saxon XSLT utility
  - [Brew](https://formulae.brew.sh/formula/saxon)


### Usage (MacOS, Linux)
- Build NiFi project (Maven); one of the following:
  - *run from directory `$NIFI_ROOT`*
  - `mvn clean install`
    - (build entire project)
  - `mvn clean install -DskipTests`
    - (do not run unit tests)
  - `mvn clean install -DskipTests -pl :nifi-assembly -pl :nifi-toolkit-assembly -am`
    - (only needed assemblies)


- (optional) Configure single user credentials:
  - *run from directory `$NIFI_ROOT`*
  - `./nifi-assembly/target/nifi-1.16.0-SNAPSHOT-bin/nifi-1.16.0-SNAPSHOT/bin/nifi.sh set-single-user-credentials myuser my12charpassword`


- Run the cluster generation script:
  - *run from directory `$NIFI_TIDBITS_ROOT`*
  - `./cluster/embed-zk/make-nifi-cluster.sh $NIFI_ROOT n`
    - `$NIFI_ROOT` full filesystem path of nifi project
    - `n` number of nodes desired (2-9)


- Start the cluster:
  - *run from directory `$NIFI_ROOT`*
  - `./nifi-assembly/target/nifi-1.16.0-SNAPSHOT-bin/cluster/start.sh`


- Access the cluster web UI:
  - [NiFi web UI](https://localhost:9443/nifi/)


- Stop the cluster:
  - *run from directory `$NIFI_ROOT`*
  - `./nifi-assembly/target/nifi-1.16.0-SNAPSHOT-bin/cluster/stop.sh`
