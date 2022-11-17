package org.moera.node.data;

import java.sql.Timestamp;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.moera.node.auth.principal.Principal;
import org.moera.node.option.Options;
import org.moera.node.util.Util;

@Entity
@Table(name = "friends")
public class Friend {

    @Id
    private UUID id;

    @NotNull
    @Size(max = 63)
    private String nodeName;

    @ManyToOne
    private FriendGroup friendGroup;

    @NotNull
    private Timestamp createdAt = Util.now();

    private Principal viewPrincipal = Principal.PUBLIC;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public FriendGroup getFriendGroup() {
        return friendGroup;
    }

    public void setFriendGroup(FriendGroup friendGroup) {
        this.friendGroup = friendGroup;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    private Principal toAbsolute(Principal principal) {
        return principal.withOwner(getNodeName());
    }

    public static Principal getViewAllPrincipal(Options options) {
        return options.getPrincipal("friends.view");
    }

    public static Principal getViewAllE(Options options) {
        return getViewAllPrincipal(options);
    }

    public Principal getViewE() {
        return toAbsolute(getViewPrincipal());
    }

    public Principal getViewPrincipal() {
        return viewPrincipal;
    }

    public void setViewPrincipal(Principal viewPrincipal) {
        this.viewPrincipal = viewPrincipal;
    }

}
