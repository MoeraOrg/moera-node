package org.moera.node.api.pushrelay;

import java.net.MalformedURLException;
import java.net.URL;
import javax.annotation.PostConstruct;
import javax.inject.Inject;

import com.googlecode.jsonrpc4j.JsonRpcHttpClient;
import com.googlecode.jsonrpc4j.ProxyUtil;
import org.moera.commons.util.LogUtil;
import org.moera.node.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FcmRelay {

    private static final Logger log = LoggerFactory.getLogger(FcmRelay.class);

    private PushRelayService service;

    @Inject
    private Config config;

    @PostConstruct
    public void init() {
        try {
            JsonRpcHttpClient client = new JsonRpcHttpClient(new URL(config.getFcmRelay()));
            service = ProxyUtil.createClientProxy(getClass().getClassLoader(), PushRelayService.class, client);
        } catch (MalformedURLException e) {
            log.error("Malformed FCM relay service URL: {}", LogUtil.format(config.getFcmRelay()));
            System.exit(1);
        }
    }

    public PushRelayService getService() {
        return service;
    }

}
