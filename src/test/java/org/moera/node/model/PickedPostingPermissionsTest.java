package org.moera.node.model;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.moera.lib.node.types.PostingInfo;
import org.moera.lib.node.types.PostingOperations;
import org.moera.lib.node.types.principal.Principal;
import org.moera.node.data.Posting;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PickedPostingPermissionsTest {

    @ParameterizedTest
    @MethodSource("restrictedPrincipals")
    void remoteGroupPermissionsAllowLocalAdmin(Principal principal) {
        Posting posting = new Posting();

        PostingInfoUtil.toPickedPosting(postingInfo(principal), posting, null);

        assertEquals(Principal.ADMIN, posting.getViewPrincipal());
        assertEquals(Principal.ADMIN, posting.getViewExternalSourcePrincipal());
        assertEquals(Principal.ADMIN, posting.getViewSharingsPrincipal());
        assertEquals(principal, posting.getReceiverViewPrincipal());
        assertEquals(principal, posting.getReceiverViewExternalSourcePrincipal());
        assertEquals(principal, posting.getReceiverViewSharingsPrincipal());
        assertEquals(Principal.NONE, posting.getViewCommentsPrincipal());
        assertEquals(Principal.NONE, posting.getAddReactionPrincipal());
        assertEquals(Principal.PUBLIC, posting.getViewTotalSharingsPrincipal());
    }

    private static Stream<Principal> restrictedPrincipals() {
        return Stream.of(Principal.ofFriendGroup("friends"), Principal.SUBSCRIBED);
    }

    @ParameterizedTest
    @MethodSource("unchangedPrincipals")
    void otherPermissionsArePreserved(Principal principal) {
        Posting posting = new Posting();

        PostingInfoUtil.toPickedPosting(postingInfo(principal), posting, null);

        assertEquals(principal, posting.getViewPrincipal());
        assertEquals(principal, posting.getViewExternalSourcePrincipal());
        assertEquals(principal, posting.getViewSharingsPrincipal());
    }

    private static Stream<Principal> unchangedPrincipals() {
        return Stream.of(Principal.PUBLIC, Principal.OWNER, Principal.SECRET, Principal.NONE, Principal.ADMIN);
    }

    @Test
    void explicitViewPermissionIsPreserved() {
        Posting posting = new Posting();

        PostingInfoUtil.toPickedPosting(postingInfo(Principal.ofFriendGroup("friends")), posting, Principal.SECRET);

        assertEquals(Principal.SECRET, posting.getViewPrincipal());
        assertEquals(Principal.ofFriendGroup("friends"), posting.getReceiverViewPrincipal());
    }

    private static PostingInfo postingInfo(Principal principal) {
        PostingOperations operations = new PostingOperations();
        operations.setView(principal);
        operations.setViewExternalSource(principal);
        operations.setViewSharings(principal);
        PostingInfo info = new PostingInfo();
        info.setId("remote-posting");
        info.setTotalComments(0);
        info.setOperations(operations);
        return info;
    }

}
