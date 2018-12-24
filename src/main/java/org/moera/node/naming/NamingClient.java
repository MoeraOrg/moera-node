package org.moera.node.naming;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.PublicKey;
import javax.annotation.PostConstruct;
import javax.inject.Inject;

import com.googlecode.jsonrpc4j.JsonRpcHttpClient;
import com.googlecode.jsonrpc4j.ProxyUtil;
import org.moera.commons.util.CryptoUtil;
import org.moera.commons.util.Util;
import org.moera.naming.rpc.NamingService;
import org.moera.node.option.Options;
import org.springframework.stereotype.Service;

@Service
public class NamingClient {

    private NamingService namingService;

    @Inject
    private Options options;

    @PostConstruct
    protected void init() throws MalformedURLException {
        JsonRpcHttpClient client = new JsonRpcHttpClient(new URL(options.getString("naming.location")));
        namingService = ProxyUtil.createClientProxy(getClass().getClassLoader(), NamingService.class, client);
    }

    public void register(String name, PublicKey updatingKey) {
        String updatingKeyE = Util.base64encode(CryptoUtil.toRawPublicKey(updatingKey));
        namingService.put(name, false, updatingKeyE, "", null, null, null);
    }

}
