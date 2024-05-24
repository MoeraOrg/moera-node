package org.moera.node.ui;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.moera.commons.util.LogUtil;
import org.moera.node.data.Posting;
import org.moera.node.data.Sitemap;
import org.moera.node.data.SitemapRecord;
import org.moera.node.data.SitemapRecordRepository;
import org.moera.node.domain.Domains;
import org.moera.node.global.PageNotFoundException;
import org.moera.node.global.RequestContext;
import org.moera.node.ui.sitemap.SitemapIndex;
import org.moera.node.ui.sitemap.SitemapIndexItem;
import org.moera.node.ui.sitemap.SitemapUrl;
import org.moera.node.ui.sitemap.SitemapUrlSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.util.Pair;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sitemaps")
public class SitemapController {

    private static final Instant SITEMAPS_UPGRADE_DATE = LocalDateTime
            .of(2022, Month.FEBRUARY, 12, 0, 0)
            .toInstant(ZoneOffset.UTC);
    private static final int MAX_SITEMAP_RECORD = 40000;
    private static final List<Pair<String, String>> STATIC_PAGES = List.of(
            Pair.of("/timeline", "hourly"),
            Pair.of("/profile", "monthly"),
            Pair.of("/people/subscribers", "monthly"),
            Pair.of("/people/subscriptions", "monthly")
    );

    private static final Logger log = LoggerFactory.getLogger(SitemapController.class);

    private int refreshPosition = 0;

    @Inject
    private RequestContext requestContext;

    @Inject
    private Domains domains;

    @Inject
    private SitemapRecordRepository sitemapRecordRepository;

    private Instant applicationStartedAt;

    @GetMapping(produces = MediaType.TEXT_XML_VALUE)
    @Transactional
    @ResponseBody
    public SitemapIndex index() {
        log.info("GET /sitemaps");

        Collection<Sitemap> sitemaps = sitemapRecordRepository.findSitemaps(requestContext.nodeId());
        SitemapIndex sitemapIndex = new SitemapIndex(requestContext.getSiteUrl(), sitemaps, SITEMAPS_UPGRADE_DATE);
        sitemapIndex.getItems().add(
                new SitemapIndexItem(requestContext.getSiteUrl(), "/sitemaps/static", applicationStartedAt));
        return sitemapIndex;
    }

    @GetMapping(path = "/static", produces = MediaType.TEXT_XML_VALUE)
    @ResponseBody
    public SitemapUrlSet sitemapStatic() {
        log.info("GET /sitemaps/static");

        return new SitemapUrlSet(STATIC_PAGES.stream()
                .map(p -> new SitemapUrl(requestContext.getSiteUrl(), p.getFirst(), p.getSecond()))
                .collect(Collectors.toList()));
    }

    @GetMapping(path = "/{id}", produces = MediaType.TEXT_XML_VALUE)
    @Transactional
    @ResponseBody
    public SitemapUrlSet sitemap(@PathVariable UUID id) {
        log.info("GET /sitemaps/{id} (id = {})", LogUtil.format(id));

        Collection<SitemapRecord> records = sitemapRecordRepository.findRecords(requestContext.nodeId(), id);
        if (records.isEmpty()) {
            throw new PageNotFoundException();
        }
        return new SitemapUrlSet(requestContext.getSiteUrl(), records);
    }

    private Sitemap findAvailableSitemap(Collection<Sitemap> sitemaps) {
        return sitemaps.stream()
                .filter(m -> m.getTotal() < MAX_SITEMAP_RECORD)
                .findFirst()
                .orElse(new Sitemap(UUID.randomUUID()));
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startup() {
        applicationStartedAt = Instant.now();
    }

    @Scheduled(fixedDelayString = "PT1H")
    @Transactional
    public void refresh() {
        List<String> names = domains.getWarmDomainNames().stream().sorted().toList();
        int sliceLength = names.size() < 50 ? names.size() : names.size() / 24;
        for (int i = 0; i < sliceLength && refreshPosition + i < names.size(); i++) {
            String domainName = names.get(refreshPosition + i);

            MDC.put("domain", domainName);
            log.debug("Refreshing sitemap of {}", domainName);

            try {
                UUID nodeId = domains.getDomainNodeId(domainName);
                Collection<Posting> postings = sitemapRecordRepository.findUpdated(nodeId);
                log.debug("{} postings to update", postings.size());
                if (postings.isEmpty()) {
                    continue;
                }

                Collection<Sitemap> sitemaps = sitemapRecordRepository.findSitemaps(nodeId);
                Sitemap sitemap = findAvailableSitemap(sitemaps);
                for (Posting posting : postings) {
                    SitemapRecord record = sitemapRecordRepository.findByEntryId(nodeId, posting.getId());
                    if (record != null) {
                        record.update(posting);
                        log.debug("Updated posting {}", posting.getId());
                    } else {
                        record = new SitemapRecord(sitemap.getId(), posting);
                        record = sitemapRecordRepository.save(record);
                        log.debug("Created record {} in sitemap {} for posting {}",
                                record.getId(), record.getSitemapId(), posting.getId());
                    }
                    sitemap.setTotal(sitemap.getTotal() + 1);
                    if (sitemap.getTotal() >= MAX_SITEMAP_RECORD) {
                        sitemap = findAvailableSitemap(sitemaps);
                    }
                }
            } catch (Exception e) {
                log.error("Error refreshing sitemap of {}", domainName, e);
            }
        }
        refreshPosition += sliceLength;
        if (refreshPosition >= names.size()) {
            refreshPosition = 0;
        }
    }

}
