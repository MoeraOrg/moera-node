package org.moera.node.ocrspace;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.moera.node.config.Config;
import org.moera.node.data.MediaFile;
import org.moera.node.media.DirectServeOperations;
import org.moera.node.media.MediaFileContent;
import org.moera.node.media.MediaOperations;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

public class OcrSpaceTest {

    @TempDir
    Path mediaPath;

    @Test
    void directServingSendsUrlWithoutFileContent() throws Exception {
        AtomicReference<Request> request = new AtomicReference<>();
        MediaOperations mediaOperations = new MediaOperations() {
            @Override
            public MediaFileContent openContent(MediaFile mediaFile) {
                throw new AssertionError("Local content should not be opened");
            }
        };
        DirectServeOperations directServeOperations = new DirectServeOperations() {
            @Override
            public String directUrl(MediaFile mediaFile) {
                return "https://node.example/moera/media/stored-name.jpg?signature=value";
            }
        };
        OcrSpace ocrSpace = ocrSpace(mediaOperations, directServeOperations, request);

        Assertions.assertNull(ocrSpace.recognize(mediaFile()));

        String body = requestBody(request.get());
        Assertions.assertTrue(body.contains("name=\"url\""));
        Assertions.assertTrue(body.contains(
            "https://node.example/moera/media/stored-name.jpg?signature=value"
        ));
        Assertions.assertFalse(body.contains("name=\"file\""));
    }

    @Test
    void unavailableDirectServingSendsLocalFileContent() throws Exception {
        AtomicReference<Request> request = new AtomicReference<>();
        Path path = mediaPath.resolve("file.jpg");
        Files.writeString(path, "local content");
        MediaOperations mediaOperations = new MediaOperations() {
            @Override
            public MediaFileContent openContent(MediaFile mediaFile) {
                return new MediaFileContent(path, false);
            }
        };
        DirectServeOperations directServeOperations = new DirectServeOperations() {
            @Override
            public String directUrl(MediaFile mediaFile) {
                return null;
            }
        };
        OcrSpace ocrSpace = ocrSpace(mediaOperations, directServeOperations, request);

        Assertions.assertNull(ocrSpace.recognize(mediaFile()));

        String body = requestBody(request.get());
        Assertions.assertTrue(body.contains("name=\"file\"; filename=\"media-id.jpg\""));
        Assertions.assertTrue(body.contains("local content"));
        Assertions.assertFalse(body.contains("name=\"url\""));
    }

    private static OcrSpace ocrSpace(
        MediaOperations mediaOperations,
        DirectServeOperations directServeOperations,
        AtomicReference<Request> request
    ) {
        Config config = new Config();
        config.setVersion("test");
        config.getMedia().setOcrService("ocrspace");
        config.getMedia().setOcrServiceKey("api-key");

        OkHttpClient client = new OkHttpClient.Builder()
            .addInterceptor(chain -> {
                request.set(chain.request());
                return new Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(ResponseBody.create(
                        "{\"OCRExitCode\":1,\"ParsedResults\":[]}",
                        MediaType.get("application/json")
                    ))
                    .build();
            })
            .build();

        OcrSpace ocrSpace = new OcrSpace();
        ReflectionTestUtils.setField(ocrSpace, "config", config);
        ReflectionTestUtils.setField(ocrSpace, "mediaOperations", mediaOperations);
        ReflectionTestUtils.setField(ocrSpace, "directServeOperations", directServeOperations);
        ReflectionTestUtils.setField(ocrSpace, "objectMapper", new ObjectMapper());
        ReflectionTestUtils.setField(ocrSpace, "client", client);
        return ocrSpace;
    }

    private static MediaFile mediaFile() {
        MediaFile mediaFile = new MediaFile();
        mediaFile.setId("media-id");
        mediaFile.setMimeType("image/jpeg");
        mediaFile.setFileName("stored-name.jpg");
        return mediaFile;
    }

    private static String requestBody(Request request) throws Exception {
        Buffer buffer = new Buffer();
        request.body().writeTo(buffer);
        return buffer.readUtf8();
    }

}
