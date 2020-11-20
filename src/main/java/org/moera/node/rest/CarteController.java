package org.moera.node.rest;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.PrivateKey;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.moera.commons.util.LogUtil;
import org.moera.node.auth.Admin;
import org.moera.node.global.ApiController;
import org.moera.node.global.Entitled;
import org.moera.node.global.RequestContext;
import org.moera.node.model.CarteInfo;
import org.moera.node.model.CarteSet;
import org.moera.node.model.OperationFailure;
import org.moera.node.util.Carte;
import org.moera.node.util.UriUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@ApiController
@RequestMapping("/moera/api/cartes")
public class CarteController {

    private static Logger log = LoggerFactory.getLogger(CarteController.class);

    private static final int DEFAULT_SET_SIZE = 16;
    private static final int MAX_SET_SIZE = 128;

    @Inject
    private RequestContext requestContext;

    @GetMapping
    @Admin
    @Entitled
    public CarteSet get(@RequestParam(required = false) Integer limit, HttpServletRequest request) {
        log.info("GET /cartes (limit = {})", LogUtil.format(limit));

        limit = limit != null ? limit : DEFAULT_SET_SIZE;
        limit = (limit > 0 && limit <= MAX_SET_SIZE) ? limit : MAX_SET_SIZE;

        String ownerName = requestContext.nodeName();
        PrivateKey signingKey = requestContext.getOptions().getPrivateKey("profile.signing-key");

        List<CarteInfo> cartes = new ArrayList<>();
        Instant beginning = Instant.now();
        try {
            InetAddress remoteAddress = UriUtil.remoteAddress(request);
            for (int i = 0; i <  limit; i++) {
                    CarteInfo carteInfo = new CarteInfo();
                    carteInfo.setCarte(Carte.generate(ownerName, remoteAddress, beginning, signingKey, null));
                    carteInfo.setBeginning(beginning.getEpochSecond());
                    beginning = Carte.getDeadline(beginning);
                    carteInfo.setDeadline(beginning.getEpochSecond());
                    cartes.add(carteInfo);
            }

            CarteSet carteSet = new CarteSet();
            carteSet.setCartesIp(remoteAddress.getHostAddress());
            carteSet.setCartes(cartes);
            return carteSet;
        } catch (UnknownHostException e) {
            throw new OperationFailure("carte.client-address-unknown");
        }
    }

}
