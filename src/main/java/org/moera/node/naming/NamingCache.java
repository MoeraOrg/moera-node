package org.moera.node.naming;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.moera.node.util.Util;
import org.springframework.stereotype.Service;

@Service
public class NamingCache {

    private static final int MAX_SIZE = 1024;

    private ReadWriteLock cacheLock = new ReentrantReadWriteLock();
    private Map<String, RegisteredNameDetails> cache = new HashMap<>();
    private List<String> recentNames = new ArrayList<>();

    public RegisteredNameDetails getFast(String name) {
        return new RegisteredNameDetails(false, getRedirector(name));
    }

    public RegisteredNameDetails get(String name) {
        return new RegisteredNameDetails(false, getRedirector(name));
    }

    private String getRedirector(String name) {
        return "/moera/gotoname?name=" + Util.ue(name);
    }

}
