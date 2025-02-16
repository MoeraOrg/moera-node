package org.moera.node.rest;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.PrivateKey;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;

import org.moera.lib.node.types.CarteAttributes;
import org.moera.lib.node.types.CarteInfo;
import org.moera.lib.node.types.CarteSet;
import org.moera.lib.node.types.CarteVerificationInfo;
import org.moera.lib.node.types.Scope;
import org.moera.lib.util.LogUtil;
import org.moera.node.auth.Admin;
import org.moera.node.auth.AuthenticationManager;
import org.moera.node.auth.CarteAuthInfo;
import org.moera.node.auth.InvalidCarteException;
import org.moera.node.global.ApiController;
import org.moera.node.global.Entitled;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.model.CarteInfoUtil;
import org.moera.node.model.ClientCarte;
import org.moera.node.model.OperationFailure;
import org.moera.node.util.UriUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @Inject
    private AuthenticationManager authenticationManager;

    @Inject
    private MessageSource messageSource;

    // FIXME GET is for backward compatibility only
    @GetMapping
    @Admin(Scope.REMOTE_IDENTIFY)
    @Entitled
    @Transactional
    @Deprecated
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
            List<CarteInfo> cartes =
                    generateCarteList(ownerName, signingKey, remoteAddress, Scope.ALL.getMask(), 0, limit);
            cartes.addAll(
                    generateCarteList(ownerName, signingKey, remoteAddress, Scope.VIEW_MEDIA.getMask(), 0, limit));
            carteSet.setCartes(cartes);
            carteSet.setCreatedAt(Instant.now().getEpochSecond());
            return carteSet;
        } catch (UnknownHostException e) {
            throw new OperationFailure("carte.client-address-unknown");
        }
    }

    @PostMapping
    @Admin(Scope.REMOTE_IDENTIFY)
    @Entitled
    @Transactional
    public CarteSet post(@Valid @RequestBody CarteAttributes attributes, HttpServletRequest request) {
        log.info("POST /cartes");

        int limit = attributes.getLimit() != null ? attributes.getLimit() : DEFAULT_SET_SIZE;
        limit = (limit > 0 && limit <= MAX_SET_SIZE) ? limit : MAX_SET_SIZE;
        long scopeMask = ObjectUtils.isEmpty(attributes.getClientScope())
                ? Scope.ALL.getMask()
                : Scope.forValues(attributes.getClientScope());
        scopeMask &= requestContext.getAdminScope();
        long adminMask = Scope.forValues(attributes.getAdminScope());

        String ownerName = requestContext.nodeName();
        PrivateKey signingKey = requestContext.getOptions().getPrivateKey("profile.signing-key");

        try {
            InetAddress remoteAddress = UriUtil.remoteAddress(request);

            CarteSet carteSet = new CarteSet();
            carteSet.setCartesIp(remoteAddress.getHostAddress());
            carteSet.setCartes(generateCarteList(ownerName, signingKey, remoteAddress, scopeMask, adminMask, limit));
            carteSet.setCreatedAt(Instant.now().getEpochSecond());
            return carteSet;
        } catch (UnknownHostException e) {
            throw new OperationFailure("carte.client-address-unknown");
        }
    }

    private List<CarteInfo> generateCarteList(String ownerName, PrivateKey signingKey, InetAddress remoteAddress,
                                              long scopeMask, long adminMask, int limit) {
        List<CarteInfo> cartes = new ArrayList<>();
        Instant beginning = Instant.now().minusSeconds(BEGINNING_IN_PAST);
        for (int i = 0; i < limit; i++) {
            CarteInfo carteAll = CarteInfoUtil.generate(
                ownerName, remoteAddress, beginning, signingKey, null, scopeMask, adminMask
            );
            cartes.add(carteAll);
            beginning = Instant.ofEpochSecond(carteAll.getDeadline());
        }
        return cartes;
    }

    @PostMapping("/verify")
    @Admin(Scope.OTHER)
    @Entitled
    @Transactional
    public CarteVerificationInfo verify(@Valid @RequestBody ClientCarte clientCarte) {
        log.info("POST /cartes/verify");

        CarteVerificationInfo info = new CarteVerificationInfo();

        try {
            CarteAuthInfo authInfo = authenticationManager.getCarte(clientCarte.getCarte(), null);
            if (clientCarte.getClientName() != null && !clientCarte.getClientName().equals(authInfo.getClientName())) {
                throw new InvalidCarteException("carte.wrong-client");
            }
            info.setValid(true);
            info.setClientName(authInfo.getClientName());
            info.setClientScope(Scope.toValues(authInfo.getClientScope()));
            info.setAdminScope(Scope.toValues(authInfo.getAdminScope()));
        } catch (InvalidCarteException e) {
            info.setValid(false);
            info.setErrorCode(e.getErrorCode());
            String errorMessage = messageSource.getMessage(e.getErrorCode(), null, Locale.getDefault());
            info.setErrorMessage(errorMessage);
        }

        return info;
    }

}
