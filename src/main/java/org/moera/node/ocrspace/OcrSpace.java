package org.moera.node.ocrspace;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import jakarta.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pemistahl.lingua.api.Language;
import com.github.pemistahl.lingua.api.LanguageDetector;
import com.github.pemistahl.lingua.api.LanguageDetectorBuilder;
import com.ibm.icu.text.UnicodeSet;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.languagetool.JLanguageTool;
import org.languagetool.rules.Rule;
import org.languagetool.rules.RuleMatch;
import org.moera.node.config.Config;
import org.moera.node.data.MediaFile;
import org.moera.node.media.MediaOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

@Service
public class OcrSpace {

    public static final long MAX_FILE_SIZE = 1024 * 1024;

    private static final Logger log = LoggerFactory.getLogger(OcrSpace.class);

    private static final String API_ENDPOINT = "https://api.ocr.space/parse/image";
    private static final Language[] SUPPORTED_LANGUAGES = { Language.ENGLISH, Language.RUSSIAN, Language.UKRAINIAN };
    private static final UnicodeSet EMOJIS = new UnicodeSet("[[:Emoji:]]").freeze();

    private final OkHttpClient client = new OkHttpClient();

    private final LanguageDetector detector;
    private final Map<Language, JLanguageTool> languageTools;

    @Inject
    private Config config;

    @Inject
    private MediaOperations mediaOperations;

    @Inject
    private ObjectMapper objectMapper;

    public OcrSpace() {
        detector = LanguageDetectorBuilder.fromLanguages(SUPPORTED_LANGUAGES).build();
        languageTools = Map.of(
            Language.ENGLISH, createLanguageTool("en-US"),
            Language.RUSSIAN, createLanguageTool("ru-RU"),
            Language.UKRAINIAN, createLanguageTool("uk-UA")
        );
    }

    private static JLanguageTool createLanguageTool(String lang) {
        JLanguageTool langTool = new JLanguageTool(org.languagetool.Languages.getLanguageForShortCode(lang));
        langTool.getAllRules().stream()
            .filter(rule -> !rule.isDictionaryBasedSpellingRule())
            .map(Rule::getId)
            .forEach(langTool::disableRule);
        return langTool;
    }

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
            .addHeader("User-Agent", config.getUserAgent())
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
                            String text = parsedResult.getParsedText();
                            if (ObjectUtils.isEmpty(text)) {
                                return null;
                            }
                            return filterGibberish(text);
                        }
                    }
                    throw new OcrSpaceRecognitionException(result);
                } catch (IOException e) {
                    throw new OcrSpaceInvalidResponseException(e);
                }
            }
        } catch (IOException e) {
            log.debug("OCR service connection error", e);
            throw new OcrSpaceConnectionException(e);
        }
    }

    private String filterGibberish(String text) throws IOException{
        text = text.trim().replaceAll("\\s+", " ");
        log.debug("Recognized text: {}", text);

        if (!isGoodScript(text)) {
            log.debug("Bad script, skipping");
            return null;
        }

        Language language = detector.detectLanguageOf(text);
        if (!Arrays.asList(SUPPORTED_LANGUAGES).contains(language)) {
            log.debug("Unknown language, skipping");
            return null;
        }

        List<RuleMatch> matches = languageTools.get(language).check(text);
        int count = 0;
        if (!matches.isEmpty()) {
            for (RuleMatch match : matches) {
                count += match.getToPos() - match.getFromPos();
            }
        }
        double ratio = (double) count / text.length();
        log.debug("Error ratio = {}, the text is {}", ratio, ratio >= 0.2 ? "gibberish" : "normal");

        return ratio < 0.2 ? text : null;
    }

    private boolean isGoodScript(String text) {
        for (int i = 0; i < text.length();) {
            final int cp = text.codePointAt(i);
            i += Character.charCount(cp);

            if (Character.isDigit(cp)) {
                continue;
            }

            Character.UnicodeScript script = Character.UnicodeScript.of(cp);
            if (script == Character.UnicodeScript.LATIN || script == Character.UnicodeScript.CYRILLIC) {
                continue;
            }

            switch (Character.getType(cp)) {
                case Character.CONNECTOR_PUNCTUATION:
                case Character.DASH_PUNCTUATION:
                case Character.START_PUNCTUATION:
                case Character.END_PUNCTUATION:
                case Character.INITIAL_QUOTE_PUNCTUATION:
                case Character.FINAL_QUOTE_PUNCTUATION:
                case Character.OTHER_PUNCTUATION:
                case Character.MATH_SYMBOL:
                case Character.OTHER_NUMBER:
                    continue;
            }

            if (Character.isWhitespace(cp)) {
                continue;
            }

            if (EMOJIS.contains(cp)) {
                continue;
            }

            return false;
        }

        return true;
    }

}
