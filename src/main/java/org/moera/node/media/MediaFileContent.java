package org.moera.node.media;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public record MediaFileContent(Path path, boolean temporary) implements AutoCloseable {

    @Override
    public void close() throws IOException {
        if (temporary) {
            Files.deleteIfExists(path);
        }
    }

}
