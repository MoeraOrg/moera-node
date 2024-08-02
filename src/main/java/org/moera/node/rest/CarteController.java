package org.moera.node.rest;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.PrivateKey;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;

import org.moera.commons.util.LogUtil;
import org.moera.node.auth.Admin;
import org.moera.node.auth.AuthScope;
import org.moera.node.auth.Scope;
import org.moera.node.global.ApiController;
import org.moera.node.global.Entitled;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.model.CarteInfo;
import org.moera.node.model.CarteSet;
import org.moera.node.model.OperationFailure;
import org.moera.node.util.UriUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@ApiController
@RequestMapping("/moera/api/cartes")
@NoCache
public class CarteController {

    private static final Logger log = LoggerFactory.getLogger(CarteController.class);

    private static final int DEFAULT_SET_SIZE = 16;
    private static final int MAX_SET_SIZE = 128;
    private static final int BEGINNING_IN_PAST = 120; // seconds

    @Inject
    private RequestContext requestContext;

    @GetMapping
    @Admin
    @AuthScope(Scope.REMOTE_IDENTIFY)
    @Entitled
    @Transactional
    public CarteSet get(@RequestParam(required = false) Integer limit, HttpServletRequest request) {
        log.info("GET /cartes (limit = {})", LogUtil.format(limit));

        limit = limit != null ? limit : DEFAULT_SET_SIZE;
        limit = (limit > 0 && limit <= MAX_SET_SIZE) ? limit : MAX_SET_SIZE;

        String ownerName = requestContext.nodeName();
        PrivateKey signingKey = requestContext.getOptions().getPrivateKey("profile.signing-key");

        try {
            InetAddress remoteAddress = UriUtil.remoteAddress(request);

            CarteSet carteSet = new CarteSet();
            carteSet.setCartesIp(remoteAddress.getHostAddress());
            carteSet.setCartes(generateCarteList(ownerName, signingKey, remoteAddress, limit));
            carteSet.setCreatedAt(Instant.now().getEpochSecond());
            return carteSet;
        } catch (UnknownHostException e) {
            throw new OperationFailure("carte.client-address-unknown");
        }
    }

    // TODO take admin scope into account when setting scope for cartes
    private List<CarteInfo> generateCarteList(String ownerName, PrivateKey signingKey, InetAddress remoteAddress,
                                              int limit) {
        List<CarteInfo> cartes = new ArrayList<>();
        Instant beginning = Instant.now().minusSeconds(BEGINNING_IN_PAST);
        for (int i = 0; i < limit; i++) {
            CarteInfo carteAll = CarteInfo.generate(ownerName, remoteAddress, beginning, signingKey, null,
                    Scope.ALL.getMask());
            CarteInfo carteViewMedia = CarteInfo.generate(ownerName, remoteAddress, beginning, signingKey, null,
                    Scope.VIEW_MEDIA.getMask());
            cartes.add(carteAll);
            cartes.add(carteViewMedia);
            beginning = Instant.ofEpochSecond(carteAll.getDeadline());
        }
        return cartes;
    }

}
