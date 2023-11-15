# NiFi Development Cluster - Create Keystores / Truststores Using OpenSSL

### OpenSSL Version that Supports "subjectAltName"
`OpenSSL 3.0.7 1 Nov 2022 (Library: OpenSSL 3.0.7 1 Nov 2022)`

### OpenSSL Version that Does Not Support "subjectAltName"
`LibreSSL 2.8.3`

```
- certificate authority
openssl genrsa -aes256 -out ca.key 3072
- one of these for each cluster node
openssl genrsa -aes256 -out nifi1.key 3072

- certificate authority
openssl req -new -x509 -days 1461 -key ca.key -sha256  -out ca.cer -subj "/CN=nifi-ca/OU=nifi/"
- one of these for each cluster node
/usr/local/opt/openssl/bin/openssl req -new -key nifi1.key -out nifi1.csr -subj "/CN=nifi1/OU=nifi/" -addext "subjectAltName = IP:192.168.1.1,DNS:localhost"

- one of these for each cluster node
openssl x509 -req -in nifi1.csr -CA ca.cer -CAkey ca.key -CAcreateserial -out nifi1.cer -days 365 -sha256

- one of these for each cluster node
cat ca.cer nifi1.cer >nifi1.chain.cer

- certificate authority (truststore, JKS or PKCS12
# openssl pkcs12 -export -out trust.pkcs12 -in ca.cer -nokeys
# keytool -importcert -keystore trust.jks -file ca.cer -alias 1
keytool -importcert -keystore trust.p12 -storetype PKCS12 -file ca.cer -alias 1

- one of these for each cluster node
openssl pkcs12 -export -out nifi1.p12 -inkey nifi1.key -in nifi1.chain.cer
```

### Notes
- https://stackoverflow.com/questions/52524948/created-java-truststore-p12-using-only-openssl
- https://angelborroy.wordpress.com/2022/08/12/building-a-custom-pkcs12-truststore-for-java/
- https://github.com/openssl/openssl/pull/19025
- https://github.com/openssl/openssl/issues/21784
- https://github.com/openssl/openssl/discussions/21791
