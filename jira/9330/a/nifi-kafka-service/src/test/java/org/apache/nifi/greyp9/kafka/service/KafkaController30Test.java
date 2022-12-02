package org.apache.nifi.greyp9.kafka.service;

import org.apache.nifi.greyp9.kafka.api.KafkaController;
import org.apache.nifi.greyp9.kafka30.KafkaController30;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@TestMethodOrder(MethodOrderer.MethodName.class)
public class KafkaController30Test {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Test
    public void test_1_InstantiateController30() {
        final KafkaController kafkaController = new KafkaController30();
        logger.info(kafkaController.getClass().getName());
    }

    @Test
    public void test_2_InstantiateController30Reflect() throws ReflectiveOperationException {
        final String className = "org.apache.nifi.greyp9.kafka30.KafkaController30";
        final KafkaController kafkaController = Class.forName(className).asSubclass(KafkaController.class).newInstance();
        logger.info(kafkaController.getClass().getName());
        kafkaController.publishOne(className);
        final String value = kafkaController.consumeOne();
        Assertions.assertEquals(className, value);
    }
}
