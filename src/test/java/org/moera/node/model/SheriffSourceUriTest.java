package org.moera.node.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.moera.lib.node.types.SheriffComplaintText;
import org.moera.node.data.SheriffComplaint;
import org.moera.node.data.SheriffComplaintGroup;

class SheriffSourceUriTest {

    @Test
    void copiesComplaintAndRemoteOwnerSourceUris() {
        SheriffComplaintText text = new SheriffComplaintText();
        text.setOwnerSourceUri("https://source.example/reporter");
        text.setNodeSourceUri("https://source.example/node");
        text.setPostingOwnerSourceUri("https://source.example/posting-owner");
        text.setCommentOwnerSourceUri("https://source.example/comment-owner");
        SheriffComplaint complaint = new SheriffComplaint();
        SheriffComplaintGroup group = new SheriffComplaintGroup();

        SheriffComplaintTextUtil.toSheriffComplaint(text, complaint);
        SheriffComplaintTextUtil.toSheriffComplaintGroup(text, group);

        assertThat(complaint.getOwnerSourceUri()).isEqualTo("https://source.example/reporter");
        assertThat(group.getRemoteNodeSourceUri()).isEqualTo("https://source.example/node");
        assertThat(group.getRemotePostingOwnerSourceUri()).isEqualTo("https://source.example/posting-owner");
        assertThat(group.getRemoteCommentOwnerSourceUri()).isEqualTo("https://source.example/comment-owner");
    }

}
