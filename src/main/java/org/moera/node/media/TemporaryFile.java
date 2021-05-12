package org.moera.node.media;

import java.io.OutputStream;
import java.nio.file.Path;

public class TemporaryFile {

    private final Path path;
    private final OutputStream outputStream;

    public TemporaryFile(Path path, OutputStream outputStream) {
        this.path = path;
        this.outputStream = outputStream;
    }

    public Path getPath() {
        return path;
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }

}
