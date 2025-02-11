package org.moera.node.model;

import java.net.InetAddress;
import java.security.PrivateKey;
import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.lib.node.types.Scope;
import org.moera.node.util.Carte;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CarteInfo {

    private String carte;
    private long beginning;
    private long deadline;
    private String nodeName;
    private List<String> clientScope;
    private List<String> adminScope;

    public static CarteInfo generate(String ownerName, InetAddress address, Instant beginning, PrivateKey signingKey,
                                     String nodeName, long clientScope, long adminScope) {
        CarteInfo carteInfo = new CarteInfo();
        carteInfo.setCarte(Carte.generate(ownerName, address, beginning, signingKey, nodeName, clientScope, adminScope));
        carteInfo.setBeginning(beginning.getEpochSecond());
        carteInfo.setDeadline(Carte.getDeadline(beginning).getEpochSecond());
        carteInfo.setNodeName(nodeName);
        carteInfo.setClientScope(Scope.toValues(clientScope));
        carteInfo.setAdminScope(Scope.toValues(adminScope));
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

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public List<String> getClientScope() {
        return clientScope;
    }

    public void setClientScope(List<String> clientScope) {
        this.clientScope = clientScope;
    }

    public List<String> getAdminScope() {
        return adminScope;
    }

    public void setAdminScope(List<String> adminScope) {
        this.adminScope = adminScope;
    }

}
