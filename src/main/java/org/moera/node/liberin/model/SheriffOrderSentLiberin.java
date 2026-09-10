package org.moera.node.liberin.model;

import org.moera.node.data.SheriffOrder;
import org.moera.node.liberin.Liberin;

public class SheriffOrderSentLiberin extends Liberin {

    private String sheriffName;
    private SheriffOrder sheriffOrder;

    public SheriffOrderSentLiberin(String sheriffName, SheriffOrder sheriffOrder) {
        this.sheriffName = sheriffName;
        this.sheriffOrder = sheriffOrder;
    }

    public String getSheriffName() {
        return sheriffName;
    }

    public void setSheriffName(String sheriffName) {
        this.sheriffName = sheriffName;
    }

    public SheriffOrder getSheriffOrder() {
        return sheriffOrder;
    }

    public void setSheriffOrder(SheriffOrder sheriffOrder) {
        this.sheriffOrder = sheriffOrder;
    }

}
