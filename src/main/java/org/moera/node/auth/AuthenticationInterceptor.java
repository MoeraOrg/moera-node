package org.moera.node.auth;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Objects;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.moera.node.config.Config;
import org.moera.node.data.Token;
import org.moera.node.friends.FriendCache;
import org.moera.node.friends.SubscribedCache;
import org.moera.node.global.RequestContext;
import org.moera.node.model.Result;
import org.moera.node.operations.FeedOperations;
import org.moera.node.operations.GrantCache;
import org.moera.node.util.UriUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthenticationInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationInterceptor.class);

    @Inject
    private Config config;

    @Inject
    private AuthenticationManager authenticationManager;

    @Inject
    private FeedOperations feedOperations;

    @Inject
    private FriendCache friendCache;

    @Inject
    private SubscribedCache subscribedCache;

    @Inject
    private GrantCache grantCache;

    @Inject
    private RequestContext requestContext;

    @Inject
    private MessageSource messageSource;

    @Inject
    private ObjectMapper objectMapper;

    @PostConstruct
    public void init() throws RootSecretNotSetException {
        if (ObjectUtils.isEmpty(config.getRootSecret())) {
            throw new RootSecretNotSetException();
        }
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        try {
            processAuthParameters(request);

            if (!(handler instanceof HandlerMethod handlerMethod)) {
                return true;
            }
            if (handlerMethod.hasMethodAnnotation(RootAdmin.class) && !requestContext.isRootAdmin()) {
                throw new AuthenticationException();
            }
            Admin adminAnnotation = handlerMethod.getMethodAnnotation(Admin.class);
            if (adminAnnotation != null && !requestContext.isAdmin(adminAnnotation.value())) {
                throw new AuthenticationException();
            }

            return true;
        } catch (InvalidTokenException e) {
            handleError(response, HttpStatus.UNAUTHORIZED, "authentication.invalid",
                    "Bearer realm=\"Node\" error=\"invalid_token\"");
            return false;
        } catch (AuthenticationException e) {
            handleError(response, HttpStatus.FORBIDDEN, "authentication.required",
                    "Bearer realm=\"Node\"");
            return false;
        }
    }

    private void handleError(HttpServletResponse response, HttpStatus status, String errorCode, String wwwAuthHeader)
            throws IOException {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, wwwAuthHeader);
        response.setStatus(status.value());
        String message = messageSource.getMessage(errorCode, null, Locale.getDefault());
        objectMapper.writeValue(new PrintWriter(response.getOutputStream()), new Result(errorCode, message));
    }

    private AuthSecrets extractSecrets(HttpServletRequest request) {
        String auth = request.getParameter("auth");
        if (!ObjectUtils.isEmpty(auth)) {
            return new AuthSecrets(auth);
        }

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!ObjectUtils.isEmpty(authHeader)) {
            String[] parts = StringUtils.split(authHeader, " ");
            if (parts != null && parts[0].trim().equalsIgnoreCase("bearer")) {
                return new AuthSecrets(parts[1].trim());
            }
        }
        return new AuthSecrets();
    }

    private void processAuthParameters(HttpServletRequest request) throws InvalidTokenException, UnknownHostException {
        requestContext.setLocalAddr(InetAddress.getByName(request.getLocalAddr()));
        requestContext.setRemoteAddr(UriUtil.remoteAddress(request));
        AuthSecrets secrets = extractSecrets(request);
        if (Objects.equals(config.getRootSecret(), secrets.rootSecret)) {
            requestContext.setRootAdmin(true);
            requestContext.setAdminScope(Scope.ALL.getMask());
        } else {
            Token token = authenticationManager.getToken(secrets.token, requestContext.nodeId());
            if (token != null) {
                requestContext.setAdminScope(token.getAuthScope() != 0 ? token.getAuthScope() : Scope.ALL.getMask());
                requestContext.setClientName(requestContext.nodeName());
                requestContext.setClientScope(requestContext.getAdminScope());
                requestContext.setOwner(true);
                requestContext.setTokenId(token.getId());
            }
        }
        try {
            CarteAuthInfo carteAuthInfo = authenticationManager.getCarte(secrets.carte, UriUtil.remoteAddress(request));
            if (carteAuthInfo != null) {
                String clientName = carteAuthInfo.getClientName();
                requestContext.setClientName(clientName);
                requestContext.setFriendGroups(friendCache.getClientGroupIds(clientName));
                requestContext.setSubscribedToClient(subscribedCache.isSubscribed(clientName));
                requestContext.setClientScope(carteAuthInfo.getClientScope());
                requestContext.setOwner(Objects.equals(clientName, requestContext.nodeName()));
                long adminScope = carteAuthInfo.getAdminScope();
                adminScope &= grantCache.get(requestContext.nodeId(), clientName);
                if (requestContext.isOwner()) {
                    adminScope |= carteAuthInfo.getClientScope() & Scope.VIEW_ALL.getMask();
                }
                requestContext.setAdminScope(adminScope);
            }
        } catch (UnknownHostException e) {
            throw new InvalidCarteException("carte.client-address-unknown");
        }
        logAuthStatus();
        if (!ObjectUtils.isEmpty(requestContext.getClientName(Scope.SHERIFF))) {
            requestContext.setPossibleSheriff(
                    feedOperations.getAllPossibleSheriffs().stream()
                            .anyMatch(nodeName -> requestContext.isClient(nodeName, Scope.SHERIFF)));
        }
    }

    private void logAuthStatus() {
        String clientName = requestContext.getClientName(Scope.IDENTIFY);
        if (requestContext.isRootAdmin()) {
            MDC.put("auth", "!");
        } else {
            String prompt = (requestContext.getAdminScope() != 0 ? "#" : "-") + (clientName != null ? "$" : "-");
            MDC.put("auth", prompt);
            if (clientName != null) {
                log.info("Authorized with node name {}", clientName);
            }
        }
        if (requestContext.getAdminScope() != 0 && !requestContext.isAdmin(Scope.ALL)) {
            log.info("Admin scope is ({})", String.join(", ", Scope.toValues(requestContext.getAdminScope())));
        }
        if (clientName != null && !requestContext.hasClientScope(Scope.ALL)) {
            log.info("Client scope is ({})", String.join(", ", Scope.toValues(requestContext.getClientScope())));
        }
    }

}
