package org.moera.node.rest;

import java.net.URI;
import java.util.UUID;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;

import org.moera.commons.util.LogUtil;
import org.moera.node.auth.Admin;
import org.moera.node.data.BlockedInstant;
import org.moera.node.data.BlockedInstantRepository;
import org.moera.node.data.Entry;
import org.moera.node.data.EntryRepository;
import org.moera.node.global.ApiController;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.liberin.model.BlockedInstantAddedLiberin;
import org.moera.node.liberin.model.BlockedInstantDeletedLiberin;
import org.moera.node.model.BlockedInstantAttributes;
import org.moera.node.model.BlockedInstantInfo;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.Result;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiController
@RequestMapping("/moera/api/blocked-instants")
@NoCache
public class BlockedInstantController {

    private static final Logger log = LoggerFactory.getLogger(BlockedInstantController.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private EntryRepository entryRepository;

    @Inject
    private BlockedInstantRepository blockedInstantRepository;

    @PostMapping
    @Admin
    @Transactional
    public ResponseEntity<BlockedInstantInfo> post(
            @Valid @RequestBody BlockedInstantAttributes blockedInstantAttributes) {
        log.info("POST /blocked-instants (storyType = {}, entryId = {}",
                LogUtil.format(blockedInstantAttributes.getStoryType().toString()),
                LogUtil.format(blockedInstantAttributes.getEntryId()));

        Entry entry = null;
        if (blockedInstantAttributes.getEntryId() != null) {
            entry = entryRepository.findByNodeIdAndId(requestContext.nodeId(), blockedInstantAttributes.getEntryId())
                    .orElseThrow(() -> new ObjectNotFoundFailure("entry.not-found"));
        }

        BlockedInstant blockedInstant = new BlockedInstant();
        blockedInstant.setId(UUID.randomUUID());
        blockedInstant.setNodeId(requestContext.nodeId());
        blockedInstant.setStoryType(blockedInstantAttributes.getStoryType());
        blockedInstant.setEntry(entry);
        blockedInstant.setCreatedAt(Util.now());
        blockedInstant = blockedInstantRepository.save(blockedInstant);

        requestContext.send(new BlockedInstantAddedLiberin(blockedInstant));

        return ResponseEntity.created(URI.create("/blocked-instants/" + blockedInstant.getId()))
                .body(new BlockedInstantInfo(blockedInstant));
    }

    @GetMapping("/{id}")
    @Admin
    @Transactional
    public BlockedInstantInfo get(@PathVariable UUID id) {
        log.info("GET /blocked-instants/{id}, (id = {})", LogUtil.format(id));

        BlockedInstant blockedInstant = blockedInstantRepository.findByNodeIdAndId(requestContext.nodeId(), id)
                .orElseThrow(() -> new ObjectNotFoundFailure("blocked-instant.not-found"));

        return new BlockedInstantInfo(blockedInstant);
    }

    @DeleteMapping("/{id}")
    @Admin
    @Transactional
    public Result delete(@PathVariable UUID id) {
        log.info("DELETE /blocked-instants/{id}, (id = {})", LogUtil.format(id));

        BlockedInstant blockedInstant = blockedInstantRepository.findByNodeIdAndId(requestContext.nodeId(), id)
                .orElseThrow(() -> new ObjectNotFoundFailure("blocked-instant.not-found"));
        blockedInstantRepository.delete(blockedInstant);

        requestContext.send(new BlockedInstantDeletedLiberin(blockedInstant));

        return Result.OK;
    }

}
