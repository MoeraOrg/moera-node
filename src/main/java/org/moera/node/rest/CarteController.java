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
import org.moera.node.auth.Scope;
import org.moera.node.global.ApiController;
import org.moera.node.global.Entitled;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.model.CarteInfo;
import org.moera.node.model.CarteSet;
import org.moera.node.model.OperationFailure;
import org.moera.node.util.UriUtil;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
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

    // FIXME GET is for backward compatibility only
    @RequestMapping(method = {RequestMethod.GET, RequestMethod.POST})
    @Admin(Scope.REMOTE_IDENTIFY)
    @Entitled
    @Transactional
    public CarteSet get(@RequestParam(required = false) String scope, @RequestParam(required = false) Integer limit,
                        HttpServletRequest request) {
        log.info("GET /cartes (scope = {}, limit = {})", LogUtil.format(scope), LogUtil.format(limit));

        limit = limit != null ? limit : DEFAULT_SET_SIZE;
        limit = (limit > 0 && limit <= MAX_SET_SIZE) ? limit : MAX_SET_SIZE;
        long scopeMask = ObjectUtils.isEmpty(scope) ? Scope.ALL.getMask() : Scope.forValues(Util.setParam(scope));
        scopeMask &= requestContext.getAuthScope();

        String ownerName = requestContext.nodeName();
        PrivateKey signingKey = requestContext.getOptions().getPrivateKey("profile.signing-key");

        try {
            InetAddress remoteAddress = UriUtil.remoteAddress(request);

            CarteSet carteSet = new CarteSet();
            carteSet.setCartesIp(remoteAddress.getHostAddress());
            carteSet.setCartes(generateCarteList(ownerName, signingKey, remoteAddress, scopeMask, limit));
            carteSet.setCreatedAt(Instant.now().getEpochSecond());
            return carteSet;
        } catch (UnknownHostException e) {
            throw new OperationFailure("carte.client-address-unknown");
        }
    }

    private List<CarteInfo> generateCarteList(String ownerName, PrivateKey signingKey, InetAddress remoteAddress,
                                              long scopeMask, int limit) {
        List<CarteInfo> cartes = new ArrayList<>();
        Instant beginning = Instant.now().minusSeconds(BEGINNING_IN_PAST);
        for (int i = 0; i < limit; i++) {
            CarteInfo carteAll = CarteInfo.generate(ownerName, remoteAddress, beginning, signingKey, null, scopeMask);
            cartes.add(carteAll);
            beginning = Instant.ofEpochSecond(carteAll.getDeadline());
        }
        return cartes;
    }

}
