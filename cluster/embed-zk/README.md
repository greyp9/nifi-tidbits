# Creating a Development NiFi Cluster

### NiFi project
- [nifi (github.com)](https://github.com/apache/nifi)

### Prerequisites
- Java Development Kit (8 or 11)
- Maven (3.6+)
- Saxon XSLT utility
  - MacOS: [Brew](https://formulae.brew.sh/formula/saxon)
  - Linux: `apt-get install saxon`


### Usage (MacOS, Linux)
- Build NiFi project (Maven):
    ```
    nifi $ mvn clean install -DskipTests
    ```


- (optional) Configure single user credentials:
    ```
    nifi $ ./nifi-assembly/target/nifi-1.16.0-SNAPSHOT-bin/nifi-1.16.0-SNAPSHOT/bin/nifi.sh set-single-user-credentials myuser my12charpassword
    ```


- Run the cluster generation script:
    ```
    nifi-tidbits $ ./cluster/embed-zk/make-nifi-cluster.sh $NIFI_ROOT n
    ```
  - `$NIFI_ROOT` full filesystem path of nifi project
  - `n` number of nodes desired (2-9)


- Start the cluster:
    ```
    nifi $ ./nifi-assembly/target/nifi-1.16.0-SNAPSHOT-bin/cluster/start.sh
    ```


- Access the cluster web UI:
  - [https://localhost:9443/nifi/](nifi)


- Stop the cluster:
    ```
    nifi $ ./nifi-assembly/target/nifi-1.16.0-SNAPSHOT-bin/cluster/stop.sh
    ```
