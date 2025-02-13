package org.moera.node.model;

import java.net.InetAddress;
import java.security.PrivateKey;
import java.time.Instant;

import org.moera.lib.node.types.CarteInfo;
import org.moera.lib.node.types.Scope;
import org.moera.node.util.Carte;

public class CarteInfoUtil {

    public static CarteInfo generate(
        String ownerName,
        InetAddress address,
        Instant beginning,
        PrivateKey signingKey,
        String nodeName,
        long clientScope,
        long adminScope
    ) {
        CarteInfo carteInfo = new CarteInfo();
        carteInfo.setCarte(
            Carte.generate(ownerName, address, beginning, signingKey, nodeName, clientScope, adminScope)
        );
        carteInfo.setBeginning(beginning.getEpochSecond());
        carteInfo.setDeadline(Carte.getDeadline(beginning).getEpochSecond());
        carteInfo.setNodeName(nodeName);
        carteInfo.setClientScope(Scope.toValues(clientScope));
        carteInfo.setAdminScope(Scope.toValues(adminScope));
        return carteInfo;
    }

}
