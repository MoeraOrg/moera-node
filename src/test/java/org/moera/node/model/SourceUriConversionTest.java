package org.moera.node.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.moera.lib.node.types.CommentInfo;
import org.moera.lib.node.types.CommentText;
import org.moera.lib.node.types.PostingText;
import org.moera.lib.node.types.ReactionDescription;
import org.moera.lib.node.types.RepliedTo;
import org.moera.node.data.Entry;
import org.moera.node.data.Reaction;

class SourceUriConversionTest {

    @Test
    void copiesOwnerSourceUriToPersistentModels() {
        var postingText = new PostingText();
        postingText.setOwnerSourceUri("https://source.example/posting-owner");
        var posting = new Entry();

        PostingTextUtil.toEntry(postingText, posting);

        assertThat(posting.getOwnerSourceUri()).isEqualTo("https://source.example/posting-owner");

        var commentText = new CommentText();
        commentText.setOwnerSourceUri("https://source.example/comment-owner");
        var comment = new Entry();

        CommentTextUtil.toEntry(commentText, comment);

        assertThat(comment.getOwnerSourceUri()).isEqualTo("https://source.example/comment-owner");

        var description = new ReactionDescription();
        description.setOwnerSourceUri("https://source.example/reaction-owner");
        var reaction = new Reaction();

        ReactionDescriptionUtil.toReaction(description, reaction);

        assertThat(reaction.getOwnerSourceUri()).isEqualTo("https://source.example/reaction-owner");
    }

    @Test
    void readsRepliedToSourceUri() {
        var repliedTo = new RepliedTo();
        repliedTo.setSourceUri("https://source.example/replied-to");
        var commentInfo = new CommentInfo();
        commentInfo.setRepliedTo(repliedTo);

        assertThat(CommentInfoUtil.getRepliedToSourceUri(commentInfo))
            .isEqualTo("https://source.example/replied-to");
    }

}
