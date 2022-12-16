package org.moera.node.data;

import java.util.UUID;

public class FriendGroupTotal {

    private UUID id;
    private long total;

    public FriendGroupTotal(UUID id, long total) {
        this.id = id;
        this.total = total;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

}
