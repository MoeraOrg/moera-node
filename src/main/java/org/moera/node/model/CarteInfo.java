package org.moera.node.model;

import java.net.InetAddress;
import java.security.PrivateKey;
import java.time.Instant;
import java.util.List;

import org.moera.node.auth.Scope;
import org.moera.node.util.Carte;

public class CarteInfo {

    private String carte;
    private long beginning;
    private long deadline;
    private List<String> permissions;

    public static CarteInfo generate(String ownerName, InetAddress address, Instant beginning, PrivateKey signingKey,
                                     String nodeName, long authScope) {
        CarteInfo carteInfo = new CarteInfo();
        carteInfo.setCarte(Carte.generate(ownerName, address, beginning, signingKey, nodeName, authScope));
        carteInfo.setBeginning(beginning.getEpochSecond());
        carteInfo.setDeadline(Carte.getDeadline(beginning).getEpochSecond());
        carteInfo.setPermissions(Scope.toValues(authScope));
        return carteInfo;
    }

    public String getCarte() {
        return carte;
    }

    public void setCarte(String carte) {
        this.carte = carte;
    }

    public long getBeginning() {
        return beginning;
    }

    public void setBeginning(long beginning) {
        this.beginning = beginning;
    }

    public long getDeadline() {
        return deadline;
    }

    public void setDeadline(long deadline) {
        this.deadline = deadline;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }

}
