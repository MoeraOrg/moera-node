package org.moera.node.model;

import java.net.InetAddress;
import java.security.PrivateKey;
import java.time.Instant;
import java.util.List;

import org.moera.lib.node.carte.Carte;
import org.moera.lib.node.types.CarteInfo;
import org.moera.lib.node.types.Scope;

public class CarteInfoUtil {

    public static CarteInfo generate(
        String ownerName,
        List<InetAddress> addresses,
        Instant beginning,
        PrivateKey signingKey,
        String nodeName,
        long clientScope,
        long adminScope
    ) {
        CarteInfo carteInfo = new CarteInfo();
        carteInfo.setCarte(
            Carte.generate(ownerName, addresses, beginning, signingKey, nodeName, clientScope, adminScope)
        );
        carteInfo.setBeginning(beginning.getEpochSecond());
        carteInfo.setDeadline(Carte.getDeadline(beginning).getEpochSecond());
        carteInfo.setNodeName(nodeName);
        carteInfo.setClientScope(Scope.toValues(clientScope));
        carteInfo.setAdminScope(Scope.toValues(adminScope));
        return carteInfo;
    }

}
