package org.apache.nifi.greyp9.kafka.api.common;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class RecordHeaders implements Headers {
    private final List<Header> headers;

    public RecordHeaders() {
        this.headers = new ArrayList<>();
    }

    public RecordHeaders(final List<Header> headers) {
        this.headers = headers;
    }

    @Override
    public Headers add(Header header) throws IllegalStateException {
        headers.add(header);
        return this;
    }

    @Override
    public Headers add(String key, byte[] value) throws IllegalStateException {
        headers.add(new RecordHeader(key, value));
        return this;
    }

    @Override
    public Headers remove(String key) throws IllegalStateException {
        throw new IllegalStateException(new UnsupportedOperationException());
    }

    @Override
    public Header lastHeader(String key) {
        throw new IllegalStateException(new UnsupportedOperationException());
    }

    @Override
    public Iterable<Header> headers(String var1) {
        throw new IllegalStateException(new UnsupportedOperationException());
    }

    @Override
    public Header[] toArray() {
        throw new IllegalStateException(new UnsupportedOperationException());
    }

    @Override
    public Iterator<Header> iterator() {
        return headers.iterator();
    }
}
