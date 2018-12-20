package org.moera.node.naming;

import java.net.MalformedURLException;
import java.net.URL;
import javax.annotation.PostConstruct;

import com.googlecode.jsonrpc4j.JsonRpcHttpClient;
import com.googlecode.jsonrpc4j.ProxyUtil;
import org.moera.naming.rpc.NamingService;
import org.springframework.stereotype.Service;

@Service
public class NamingClient {

    private NamingService namingService;

    @PostConstruct
    protected void init() throws MalformedURLException {
        JsonRpcHttpClient client = new JsonRpcHttpClient(new URL("http://localhost.localdomain:8081/moera-naming"));
        namingService = ProxyUtil.createClientProxy(getClass().getClassLoader(), NamingService.class, client);
    }

    public void register(String name) {
        namingService.put(name, false, "CD", "", null, null, null);
    }

}
