package org.moera.node.model;

import java.util.List;

public class CarteSet {

    private String cartesIp;
    private List<CarteInfo> cartes;
    private long createdAt;

    public String getCartesIp() {
        return cartesIp;
    }

    public void setCartesIp(String cartesIp) {
        this.cartesIp = cartesIp;
    }

    public List<CarteInfo> getCartes() {
        return cartes;
    }

    public void setCartes(List<CarteInfo> cartes) {
        this.cartes = cartes;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

}
