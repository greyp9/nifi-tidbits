package org.apache.nifi.greyp9.kafka.api;

import java.util.Properties;

public class TestUtils {

    /**
     * The default Kafka endpoint.
     */
    private static final String BOOTSTRAP_SERVER = "localhost:9092";

    /**
     * Ensure fresh data for each test run.
     */
    private static final long TIMESTAMP = System.currentTimeMillis();

    /**
     * The name of the test kafka topic to be created.
     */
    private static final String TOPIC = "nifi-topic-" + TIMESTAMP;

    /**
     * The name of the test kafka group to use.
     */
    private static final String GROUP = "nifi-group-" + TIMESTAMP;

    public static Properties getTestProducerProperties() {
        final Properties properties = new Properties();
        properties.put("bootstrap.servers", BOOTSTRAP_SERVER);
        properties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        properties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        return properties;
    }

    public static Properties getTestConsumerProperties() {
        final Properties properties = new Properties();
        properties.put("bootstrap.servers", BOOTSTRAP_SERVER);
        properties.put("group.id", GROUP);
        properties.put("auto.offset.reset", "earliest");
        properties.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        properties.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        return properties;
    }

    public static String getTestTopic() {
        return TOPIC;
    }
}
