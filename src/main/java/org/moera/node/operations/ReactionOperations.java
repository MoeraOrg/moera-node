package org.moera.node.operations;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.inject.Inject;

import org.moera.commons.crypto.CryptoUtil;
import org.moera.commons.crypto.Fingerprint;
import org.moera.commons.util.LogUtil;
import org.moera.node.auth.AuthenticationException;
import org.moera.node.auth.IncorrectSignatureException;
import org.moera.node.data.Comment;
import org.moera.node.data.Entry;
import org.moera.node.data.Posting;
import org.moera.node.data.Reaction;
import org.moera.node.data.ReactionRepository;
import org.moera.node.fingerprint.Fingerprints;
import org.moera.node.global.RequestContext;
import org.moera.node.liberin.LiberinManager;
import org.moera.node.liberin.model.CommentReactionTotalsUpdatedLiberin;
import org.moera.node.liberin.model.PostingReactionTotalsUpdatedLiberin;
import org.moera.node.media.MediaOperations;
import org.moera.node.model.ReactionDescription;
import org.moera.node.model.ReactionInfo;
import org.moera.node.model.ReactionsSliceInfo;
import org.moera.node.model.ValidationFailure;
import org.moera.node.naming.NamingCache;
import org.moera.node.util.EmojiList;
import org.moera.node.util.ExtendedDuration;
import org.moera.node.util.MomentFinder;
import org.moera.node.util.SafeInteger;
import org.moera.node.util.Transaction;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.ObjectUtils;

@Component
public class ReactionOperations {

    public static final Duration UNSIGNED_TTL = Duration.of(15, ChronoUnit.MINUTES);
    public static final int MAX_REACTIONS_PER_REQUEST = 200;

    private static final Logger log = LoggerFactory.getLogger(ReactionOperations.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private ReactionRepository reactionRepository;

    @Inject
    private NamingCache namingCache;

    @Inject
    private LiberinManager liberinManager;

    @Inject
    private ReactionTotalOperations reactionTotalOperations;

    @Inject
    private MediaOperations mediaOperations;

    @Inject
    private PlatformTransactionManager txManager;

    private final MomentFinder momentFinder = new MomentFinder();

    public void validate(ReactionDescription reactionDescription, Entry entry) {
        mediaOperations.validateAvatar(
                reactionDescription.getOwnerAvatar(),
                reactionDescription::setOwnerAvatarMediaFile,
                () -> new ValidationFailure("reactionDescription.ownerAvatar.mediaId.not-found"));

        if (reactionDescription.getSignature() == null) {
            String ownerName = requestContext.getClientName();
            if (ObjectUtils.isEmpty(ownerName)) {
                throw new AuthenticationException();
            }
            if (!ObjectUtils.isEmpty(reactionDescription.getOwnerName())
                    && !reactionDescription.getOwnerName().equals(ownerName)) {
                throw new AuthenticationException();
            }
            reactionDescription.setOwnerName(ownerName);
        } else {
            byte[] signingKey = namingCache.get(reactionDescription.getOwnerName()).getSigningKey();
            Fingerprint fingerprint = Fingerprints.reaction(reactionDescription.getSignatureVersion())
                    .create(reactionDescription, entry.getCurrentRevision().getDigest());
            if (!CryptoUtil.verify(fingerprint, reactionDescription.getSignature(), signingKey)) {
                throw new IncorrectSignatureException();
            }
            requestContext.authenticatedWithSignature(reactionDescription.getOwnerName());
        }

        if (entry.isOriginal()) {
            if (!requestContext.isPrincipal(entry.getAddReactionE())) {
                throw new AuthenticationException();
            }
            if (reactionDescription.isNegative()
                    && !requestContext.isPrincipal(entry.getAddNegativeReactionE())) {
                throw new AuthenticationException();
            }
        }

        EmojiList accepted = new EmojiList(!reactionDescription.isNegative()
                ? entry.getAcceptedReactionsPositive()
                : entry.getAcceptedReactionsNegative());
        if (!accepted.isAccepted(reactionDescription.getEmoji())) {
            throw new ValidationFailure("reaction.not-accepted");
        }
    }

    public Reaction post(ReactionDescription reactionDescription, Entry entry, Consumer<Reaction> reactionDeleted,
                         Consumer<Reaction> reactionAdded) {
        Reaction reaction = reactionRepository.findByEntryIdAndOwner(entry.getId(), reactionDescription.getOwnerName());
        if (reaction != null) {
            log.debug("Found previous reaction {}, deadline {}",
                    LogUtil.format(reaction.getId()), LogUtil.format(reaction.getDeadline()));
        } else {
            log.debug("Previous reaction not found");
        }
        if (reaction == null || reaction.getDeadline() == null
                || reaction.isNegative() != reactionDescription.isNegative()
                || reaction.getEmoji() != reactionDescription.getEmoji()
                || reaction.getSignature() == null && reactionDescription.getSignature() != null) {

            if (reaction != null) {
                log.debug("Deleting reaction {}", LogUtil.format(reaction.getId()));
                reactionTotalOperations.changeTotals(entry, reaction, -1);
                reaction.setDeletedAt(Util.now());
                if (reaction.getDeadline() == null) { // it's a real reaction, not a temporary one
                    reaction.setReplaced(true);
                }
                ExtendedDuration reactionTtl =
                        requestContext.getOptions().getDuration("reaction.deleted.lifetime");
                if (!reactionTtl.isNever()) {
                    reaction.setDeadline(Timestamp.from(Instant.now().plus(reactionTtl.getDuration())));
                }
                if (reactionDeleted != null) {
                    reactionDeleted.accept(reaction);
                }
            }

            reaction = new Reaction();
            reaction.setId(UUID.randomUUID());
            reaction.setEntryRevision(entry.getCurrentRevision());
            reactionDescription.toReaction(reaction);
            if (reactionDescription.getSignature() == null) {
                reaction.setDeadline(Timestamp.from(Instant.now().plus(ReactionOperations.UNSIGNED_TTL)));
            }
            reaction.setMoment(momentFinder.find(
                    moment -> reactionRepository.countMoments(entry.getId(), moment) == 0,
                    Util.now()));
            reaction = reactionRepository.save(reaction);
            log.debug("Created reaction {}, deadline {}",
                    LogUtil.format(reaction.getId()), LogUtil.format(reaction.getDeadline()));
            entry.getCurrentRevision().addReaction(reaction);

            reactionTotalOperations.changeTotals(entry, reaction, 1);
            if (reaction.getSignature() != null && reactionAdded != null) {
                reactionAdded.accept(reaction);
            }
        }
        reactionRepository.flush();

        return reaction;
    }

    public ReactionsSliceInfo getBefore(UUID entryId, boolean negative, Integer emoji, long before, int limit) {
        Pageable pageable = PageRequest.of(0, limit + 1, Sort.Direction.DESC, "moment");
        Page<Reaction> page = emoji == null
                ? reactionRepository.findSlice(entryId, negative,
                                                SafeInteger.MIN_VALUE, before, pageable)
                : reactionRepository.findSliceWithEmoji(entryId, negative, emoji,
                                                SafeInteger.MIN_VALUE, before, pageable);
        ReactionsSliceInfo sliceInfo = new ReactionsSliceInfo();
        sliceInfo.setBefore(before);
        if (page.getNumberOfElements() < limit + 1) {
            sliceInfo.setAfter(SafeInteger.MIN_VALUE);
        } else {
            sliceInfo.setAfter(page.getContent().get(limit).getMoment());
        }
        sliceInfo.setTotal((int) page.getTotalElements());
        sliceInfo.setReactions(page.stream().map(ReactionInfo::new).collect(Collectors.toList()));
        return sliceInfo;
    }

    public void delete(String ownerName, Entry entry, Consumer<Reaction> reactionDeleted) {
        Reaction reaction = reactionRepository.findByEntryIdAndOwner(entry.getId(), ownerName);
        if (reaction != null) {
            log.debug("Deleting reaction {}", LogUtil.format(reaction.getId()));
            reactionTotalOperations.changeTotals(entry, reaction, -1);
            reaction.setDeletedAt(Util.now());
            ExtendedDuration reactionTtl = requestContext.getOptions().getDuration("reaction.deleted.lifetime");
            if (!reactionTtl.isNever()) {
                reaction.setDeadline(Timestamp.from(Instant.now().plus(reactionTtl.getDuration())));
            }
            if (reactionDeleted != null) {
                reactionDeleted.accept(reaction);
            }
        }
        reactionRepository.flush();
    }

    @Scheduled(fixedDelayString = "PT15M")
    public void purgeExpired() throws Throwable {
        Set<Entry> changed = new HashSet<>();
        Transaction.execute(txManager, () -> {
            reactionRepository.findExpired(Util.now()).forEach(reaction -> {
                log.debug("Purging reaction {}, deletedAt = {}",
                        LogUtil.format(reaction.getId()), LogUtil.format(reaction.getDeletedAt()));
                Entry entry = reaction.getEntryRevision().getEntry();
                if (reaction.getDeletedAt() == null) { // it's the current active reaction of the user to the entry
                    List<Reaction> replaced = reactionRepository.findReplacedByEntryIdAndOwner(
                            entry.getId(),
                            reaction.getOwnerName(),
                            PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "deletedAt")));
                    if (replaced.size() > 0) {
                        Reaction next = replaced.get(0);
                        next.setDeletedAt(null);
                        next.setReplaced(false);
                        if (next.getSignature() != null) {
                            next.setDeadline(null);
                        }
                        log.debug("Restored reaction {}, deadline {}",
                                LogUtil.format(next.getId()), LogUtil.format(next.getDeadline()));
                        reactionTotalOperations.changeTotals(entry, next, 1);
                    }
                    reactionTotalOperations.changeTotals(entry, reaction, -1);
                    changed.add(entry);
                }
                reactionRepository.delete(reaction);
            });
            return null;
        });
        for (Entry entry : changed) {
            switch (entry.getEntryType()) {
                case POSTING: {
                    Posting posting = (Posting) entry;
                    var totalsInfo = reactionTotalOperations.getInfo(posting);
                    liberinManager.send(
                            new PostingReactionTotalsUpdatedLiberin(posting, totalsInfo.getPublicInfo())
                                    .withNodeId(posting.getNodeId()));
                    break;
                }

                case COMMENT: {
                    Comment comment = (Comment) entry;
                    liberinManager.send(
                            new CommentReactionTotalsUpdatedLiberin(comment).withNodeId(comment.getNodeId()));
                    break;
                }
            }
        }
    }

}
