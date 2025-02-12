package org.moera.node.liberin.model;

import java.util.Map;
import jakarta.persistence.EntityManager;

import org.moera.node.data.SheriffOrder;
import org.moera.node.liberin.Liberin;
import org.moera.node.model.SheriffOrderInfoUtil;

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

    @Override
    protected void toModel(Map<String, Object> model, EntityManager entityManager) {
        super.toModel(model);
        model.put("sheriffName", sheriffName);
        model.put("sheriffOrder", SheriffOrderInfoUtil.build(sheriffOrder, sheriffName));
    }

}
