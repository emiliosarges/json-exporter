package br.com.nog.exporter.service;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

final class CountingOutputStream extends FilterOutputStream {

    private long count;

    CountingOutputStream(OutputStream out) {
        super(out);
    }

    @Override
    public void write(int b) throws IOException {
        out.write(b);
        count++;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
        count += len;
    }

    long getCount() {
        return count;
    }
}
