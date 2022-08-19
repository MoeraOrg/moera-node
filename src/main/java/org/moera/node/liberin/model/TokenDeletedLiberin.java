package org.moera.node.liberin.model;

import java.util.Map;
import java.util.UUID;

import org.moera.node.liberin.Liberin;

public class TokenDeletedLiberin extends Liberin {

    private UUID tokenId;

    public TokenDeletedLiberin(UUID tokenId) {
        this.tokenId = tokenId;
    }

    public UUID getTokenId() {
        return tokenId;
    }

    public void setTokenId(UUID tokenId) {
        this.tokenId = tokenId;
    }

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("tokenId", tokenId);
    }

}
