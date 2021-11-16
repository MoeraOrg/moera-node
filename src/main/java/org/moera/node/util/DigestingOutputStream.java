package org.moera.node.util;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.io.output.TeeOutputStream;
import org.bouncycastle.crypto.io.DigestOutputStream;
import org.bouncycastle.jcajce.provider.util.DigestFactory;

public class DigestingOutputStream extends OutputStream {

    private final OutputStream stream;
    private final DigestOutputStream hashStream;
    private final DigestOutputStream digestStream;

    public DigestingOutputStream(OutputStream outputStream) {
        hashStream = new DigestOutputStream(DigestFactory.getDigest("SHA-1"));
        digestStream = new DigestOutputStream(DigestFactory.getDigest("SHA-256"));
        stream = new TeeOutputStream(outputStream, new TeeOutputStream(hashStream, digestStream));
    }

    public String getHash() {
        return Util.base64urlencode(hashStream.getDigest());
    }

    public byte[] getDigest() {
        return digestStream.getDigest();
    }

    @Override
    public void write(int i) throws IOException {
        stream.write(i);
    }

    @Override
    public void write(byte[] b) throws IOException {
        stream.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        stream.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        stream.flush();
    }

    @Override
    public void close() throws IOException {
        stream.close();
    }

}
