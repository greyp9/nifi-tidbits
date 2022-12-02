package org.apache.nifi.greyp9.kafka.api;

public interface KafkaController {

    void publishOne(String value);

    public String consumeOne();
}
