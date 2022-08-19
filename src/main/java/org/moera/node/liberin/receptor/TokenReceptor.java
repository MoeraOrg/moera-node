package org.moera.node.liberin.receptor;

import org.moera.node.liberin.LiberinMapping;
import org.moera.node.liberin.LiberinReceptor;
import org.moera.node.liberin.LiberinReceptorBase;
import org.moera.node.liberin.model.TokenAddedLiberin;
import org.moera.node.liberin.model.TokenDeletedLiberin;
import org.moera.node.liberin.model.TokenUpdatedLiberin;
import org.moera.node.model.TokenInfo;
import org.moera.node.model.event.TokenAddedEvent;
import org.moera.node.model.event.TokenDeletedEvent;
import org.moera.node.model.event.TokenUpdatedEvent;

@LiberinReceptor
public class TokenReceptor extends LiberinReceptorBase {

    @LiberinMapping
    public void added(TokenAddedLiberin liberin) {
        send(liberin, new TokenAddedEvent(new TokenInfo(liberin.getToken(), false)));
    }

    @LiberinMapping
    public void updated(TokenUpdatedLiberin liberin) {
        send(liberin, new TokenUpdatedEvent(new TokenInfo(liberin.getToken(), false)));
    }

    @LiberinMapping
    public void deleted(TokenDeletedLiberin liberin) {
        send(liberin, new TokenDeletedEvent(liberin.getTokenId().toString()));
    }

}
