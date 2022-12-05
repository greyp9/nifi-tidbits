package org.apache.nifi.greyp9.kafka.api.common;

import java.util.Iterator;

public interface Headers extends Iterable<Header> {

    Headers add(Header var1) throws IllegalStateException;

    Headers add(String var1, byte[] var2) throws IllegalStateException;

    Headers remove(String var1) throws IllegalStateException;

    Header lastHeader(String var1);

    Iterable<Header> headers(String var1);

    Header[] toArray();
}
