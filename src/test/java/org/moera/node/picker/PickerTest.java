package org.moera.node.picker;

import org.junit.jupiter.api.Test;
import org.moera.lib.node.MoeraNode;
import org.moera.lib.node.types.Scope;
import org.moera.node.api.node.NodeApi;
import org.moera.node.data.Pick;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.moera.node.util.MediaTransactionTestSupport.assertOutsideTransaction;
import static org.moera.node.util.MediaTransactionTestSupport.transaction;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PickerTest {

    @Test
    void fetchingPostingDoesNotHoldTransactionOrConnection() throws Exception {
        NodeApi nodeApi = mock(NodeApi.class);
        MoeraNode remote = mock(MoeraNode.class);
        when(nodeApi.at("remote", "carte")).thenReturn(remote);
        IllegalStateException failure = new IllegalStateException("Download interrupted");
        when(remote.getPosting("posting", false)).thenAnswer(invocation -> {
            assertOutsideTransaction();
            throw failure;
        });
        Picker picker = new Picker(mock(PickerPool.class), "remote") {
            @Override
            protected String generateCarte(String targetNodeName, Scope clientScope) {
                return "carte";
            }
        };
        ReflectionTestUtils.setField(picker, "nodeApi", nodeApi);
        ReflectionTestUtils.setField(picker, "tx", transaction());
        Pick pick = new Pick();
        pick.setRemotePostingId("posting");

        assertSame(failure, assertThrows(IllegalStateException.class,
            () -> ReflectionTestUtils.invokeMethod(picker, "download", pick)));
        assertOutsideTransaction();
    }

}
