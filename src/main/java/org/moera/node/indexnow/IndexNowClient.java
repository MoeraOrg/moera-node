package org.moera.node.indexnow;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import jakarta.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.moera.node.config.Config;
import org.moera.node.data.Entry;
import org.moera.node.data.EntryRepository;
import org.moera.node.data.EntryType;
import org.moera.node.domain.Domains;
import org.moera.node.global.RequestCounter;
import org.moera.node.util.Transaction;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class IndexNowClient {

    private static final Logger log = LoggerFactory.getLogger(IndexNowClient.class);

    private static final int BATCH_SIZE = 5000;

    @Inject
    private Config config;

    @Inject
    private RequestCounter requestCounter;

    @Inject
    private Domains domains;

    @Inject
    private EntryRepository entryRepository;

    @Inject
    private Transaction tx;

    @Inject
    private ObjectMapper objectMapper;

    private final OkHttpClient client = new OkHttpClient();

    private void sendRequest(String host, List<String> urlList) throws IndexNowException {
        IndexNowRequest body = new IndexNowRequest(host, config.getIndexNow().getKey(), urlList);
        try {
            Request request = new Request.Builder()
                .post(RequestBody.create(objectMapper.writeValueAsString(body), MediaType.parse("application/json")))
                .addHeader("Accept", "application/json")
                .addHeader("User-Agent", config.getUserAgent())
                .url(config.getIndexNow().getEndpoint())
                .build();
            try (Response response = this.client.newCall(request).execute()) {
                HttpStatus status = HttpStatus.valueOf(response.code());
                switch (status) {
                    case OK:
                    case ACCEPTED:
                        log.info("Sent {} URLs from domain {} to IndexNow", urlList.size(), host);
                        break;
                    case BAD_REQUEST:
                        throw new IndexNowException("Invalid format of IndexNow request", host);
                    case FORBIDDEN:
                        throw new IndexNowException("Invalid IndexNow key", host);
                    case UNPROCESSABLE_ENTITY:
                        log.error("Unprocessable IndexNow entity: {}", response.body());
                        break;
                    case TOO_MANY_REQUESTS:
                        log.debug(
                            "Too many requests to IndexNow endpoint: {}", response.header(HttpHeaders.RETRY_AFTER)
                        );
                        throw new IndexNowException("Too many requests to IndexNow endpoint", host);
                    default:
                        log.debug("Unexpected response from IndexNow endpoint: {}", response.body());
                        throw new IndexNowException("Unexpected response from IndexNow endpoint: " + status, host);
                }
            }
        } catch (JsonProcessingException e) {
            throw new IndexNowException("Cannot serialize IndexNowRequest", host, e);
        } catch (IOException e) {
            throw new IndexNowException("Error connecting to IndexNow endpoint", host, e);
        }

    }

    private void sendRequest(List<Entry> entries) {
        entries.sort(Comparator.comparing(Entry::getNodeId));

        try {
            UUID nodeId = null;
            List<Entry> entryList = new ArrayList<>();
            Set<UUID> ids = new HashSet<>();
            for (Entry entry : entries) {
                if (nodeId == null || !nodeId.equals(entry.getNodeId())) {
                    if (!entryList.isEmpty()) {
                        String domainName = domains.getDomain(nodeId).getName();
                        sendRequest(domainName, entryList.stream().map(e -> getEntryUrl(e, domainName)).toList());
                        tx.executeWriteQuietly(() -> entryRepository.indexedNow(ids, Util.now()));
                    }
                    entryList.clear();
                    ids.clear();
                    nodeId = entry.getNodeId();
                }
                entryList.add(entry);
                ids.add(entry.getId());
            }
        } catch (IndexNowException e) {
            log.error(e.getMessage(), e.getCause());
        }
    }

    private static String getEntryUrl(Entry entry, String domainName) {
        return entry.getEntryType() == EntryType.COMMENT
            ? String.format("https://%s/post/%s?comment=%s", domainName, entry.getParent().getId(), entry.getId())
            : String.format("https://%s/post/%s", domainName, entry.getId());
    }

    @Scheduled(fixedDelayString = "PT1H")
    public void indexNow() {
        if (!domains.isReady() || config.getIndexNow().getKey() == null) {
            return;
        }

        try (var ignored = requestCounter.allot()) {
            log.info("Sending IndexNow requests");

            List<Entry> entries = tx.executeRead(() ->
                entryRepository.findByIndexNow(PageRequest.of(0, BATCH_SIZE, Sort.by(Sort.Direction.DESC, "editedAt")))
            );
            if (entries.isEmpty()) {
                return;
            }

            Set<UUID> extraIds = new HashSet<>();
            List<Entry> indexed = new ArrayList<>();
            for (Entry entry : entries) {
                if (
                    !entry.isOriginal()
                    || !entry.getViewE().isPublic()
                    || entry.getParent() != null && !entry.getParent().getViewE().isPublic()
                ) {
                    extraIds.add(entry.getId());
                } else {
                    indexed.add(entry);
                }
            }
            if (!extraIds.isEmpty()) {
                tx.executeWrite(() -> entryRepository.indexedNow(extraIds, Util.now()));
            }
            if (!indexed.isEmpty()) {
                sendRequest(indexed);
            }
        }
    }

}
