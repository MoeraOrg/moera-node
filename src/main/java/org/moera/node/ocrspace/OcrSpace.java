package org.moera.node.ocrspace;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import jakarta.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.moera.node.config.Config;
import org.moera.node.data.MediaFile;
import org.moera.node.media.MediaOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

@Service
public class OcrSpace {

    private static final Logger log = LoggerFactory.getLogger(OcrSpace.class);

    private static final String API_ENDPOINT = "https://api.ocr.space/parse/image";

    private final OkHttpClient client = new OkHttpClient();

    @Inject
    private Config config;

    @Inject
    private MediaOperations mediaOperations;

    @Inject
    private ObjectMapper objectMapper;

    public String recognize(MediaFile mediaFile) {
        if (
            !Objects.equals(config.getMedia().getOcrService(), "ocrspace")
            || ObjectUtils.isEmpty(config.getMedia().getOcrServiceKey())
        ) {
            log.warn("OCR service is not configured, skipping recognition");
            return null;
        }

        Path filePath = mediaOperations.getPath(mediaFile);
        RequestBody fileBody = RequestBody.create(filePath.toFile(), MediaType.parse(mediaFile.getMimeType()));
        MultipartBody multipartBody = new MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("apikey", config.getMedia().getOcrServiceKey())
            .addFormDataPart("file", mediaFile.getFileName(), fileBody)
            .addFormDataPart("language", "auto")
            .addFormDataPart("OCREngine", "2")
            .build();
        Request request = new Request.Builder()
            .method("POST", multipartBody)
            .addHeader("Accept", "application/json")
            .url(API_ENDPOINT)
            .build();

        try {
            try (Response response = this.client.newCall(request).execute()) {
                try {
                    ResponseBody body = response.body();
                    if (body == null) {
                        throw new OcrSpaceInvalidResponseException();
                    }
                    OcrResult result = objectMapper.readValue(body.string(), OcrResult.class);
                    if (result.getOcrExitCode() == 1) {
                        if (ObjectUtils.isEmpty(result.getParsedResults())) {
                            return null;
                        }
                        ParsedResult parsedResult = result.getParsedResults().get(0);
                        if (parsedResult.getFileParseExitCode() == 1) {
                            return !ObjectUtils.isEmpty(parsedResult.getParsedText())
                                ? parsedResult.getParsedText()
                                : null;
                        }
                    }
                    throw new OcrSpaceRecognitionException(result);
                } catch (IOException e) {
                    throw new OcrSpaceInvalidResponseException(e);
                }
            }
        } catch (IOException e) {
            throw new OcrSpaceConnectionException(e);
        }
    }

}
