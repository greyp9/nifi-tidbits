package org.apache.nifi.greyp9.kafka.api.common;

public interface Header {
    String key();

    byte[] value();
}
