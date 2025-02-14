package org.moera.node.rest;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;

import org.moera.lib.node.types.BlockedInstantInfo;
import org.moera.lib.node.types.Result;
import org.moera.lib.node.types.Scope;
import org.moera.lib.util.LogUtil;
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
import org.moera.node.model.BlockedInstantFilter;
import org.moera.node.model.BlockedInstantInfoUtil;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.StoryTypeUtil;
import org.moera.node.model.ValidationFailure;
import org.moera.node.operations.BlockedInstantOperations;
import org.moera.node.operations.ReminderOperations;
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

    @Inject
    private BlockedInstantOperations blockedInstantOperations;

    @Inject
    private ReminderOperations reminderOperations;

    @PostMapping
    @Admin(Scope.OTHER)
    @Transactional
    public ResponseEntity<BlockedInstantInfo> post(
            @Valid @RequestBody BlockedInstantAttributes blockedInstantAttributes) {
        log.info("POST /blocked-instants (storyType = {}, entryId = {}, remoteNodeName = {}, remotePostingId = {})",
                LogUtil.format(blockedInstantAttributes.getStoryType().toString()),
                LogUtil.format(blockedInstantAttributes.getEntryId()),
                LogUtil.format(blockedInstantAttributes.getRemoteNodeName()),
                LogUtil.format(blockedInstantAttributes.getRemotePostingId()));

        if (blockedInstantAttributes.getStoryType() == null) {
            throw new ValidationFailure("blockedInstantAttributes.storyType.blank");
        }

        Entry entry = null;
        if (blockedInstantAttributes.getEntryId() != null) {
            entry = entryRepository.findByNodeIdAndId(requestContext.nodeId(), blockedInstantAttributes.getEntryId())
                    .orElseThrow(() -> new ObjectNotFoundFailure("entry.not-found"));
        }

        blockedInstantOperations.findExact(requestContext.nodeId(), blockedInstantAttributes.getStoryType(),
                blockedInstantAttributes.getEntryId(), blockedInstantAttributes.getRemoteNodeName(),
                blockedInstantAttributes.getRemotePostingId(), blockedInstantAttributes.getRemoteOwnerName())
                .forEach(blockedInstantRepository::delete);

        BlockedInstant blockedInstant = new BlockedInstant();
        blockedInstant.setId(UUID.randomUUID());
        blockedInstant.setNodeId(requestContext.nodeId());
        blockedInstant.setEntry(entry);
        blockedInstant.setCreatedAt(Util.now());
        blockedInstantAttributes.toBlockedInstant(blockedInstant);
        blockedInstant = blockedInstantRepository.save(blockedInstant);

        requestContext.send(new BlockedInstantAddedLiberin(blockedInstant));

        if (StoryTypeUtil.isReminder(blockedInstantAttributes.getStoryType())) {
            reminderOperations.unpublishAndDelete(blockedInstantAttributes.getStoryType());
        }

        return ResponseEntity.created(URI.create("/blocked-instants/" + blockedInstant.getId()))
                .body(BlockedInstantInfoUtil.build(blockedInstant));
    }

    @GetMapping("/{id}")
    @Admin(Scope.OTHER)
    @Transactional
    public BlockedInstantInfo get(@PathVariable UUID id) {
        log.info("GET /blocked-instants/{id}, (id = {})", LogUtil.format(id));

        BlockedInstant blockedInstant = blockedInstantRepository.findByNodeIdAndId(requestContext.nodeId(), id)
                .orElseThrow(() -> new ObjectNotFoundFailure("blocked-instant.not-found"));

        return BlockedInstantInfoUtil.build(blockedInstant);
    }

    @DeleteMapping("/{id}")
    @Admin(Scope.OTHER)
    @Transactional
    public Result delete(@PathVariable UUID id) {
        log.info("DELETE /blocked-instants/{id}, (id = {})", LogUtil.format(id));

        BlockedInstant blockedInstant = blockedInstantRepository.findByNodeIdAndId(requestContext.nodeId(), id)
                .orElseThrow(() -> new ObjectNotFoundFailure("blocked-instant.not-found"));
        blockedInstantRepository.delete(blockedInstant);

        requestContext.send(new BlockedInstantDeletedLiberin(blockedInstant));

        return Result.OK;
    }

    @PostMapping("/search")
    @Admin(Scope.OTHER)
    @Transactional
    public List<BlockedInstantInfo> post(@Valid @RequestBody BlockedInstantFilter blockedInstantFilter) {
        log.info("POST /blocked-instants/search (storyType = {})",
                LogUtil.format(blockedInstantFilter.getStoryType().toString()));

        return blockedInstantOperations.search(requestContext.nodeId(), blockedInstantFilter.getStoryType(),
                        blockedInstantFilter.getEntryId(), blockedInstantFilter.getRemoteNodeName(),
                        blockedInstantFilter.getRemotePostingId(), blockedInstantFilter.getRemoteOwnerName())
                .map(BlockedInstantInfoUtil::build)
                .collect(Collectors.toList());
    }

}
