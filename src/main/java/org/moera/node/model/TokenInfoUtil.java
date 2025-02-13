package org.moera.node.model;

import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.TokenInfo;
import org.moera.node.data.Token;
import org.moera.node.util.Util;

public class TokenInfoUtil {
    
    public static TokenInfo build(Token tokenData, boolean includeToken) {
        TokenInfo tokenInfo = new TokenInfo();
        
        tokenInfo.setId(tokenData.getId().toString());
        
        String token = tokenData.getToken();
        if (!includeToken) {
            token = token.substring(0, 4) + '\u2026';
        }
        tokenInfo.setToken(token);
        
        tokenInfo.setName(tokenData.getName());
        tokenInfo.setPermissions(Scope.toValues(tokenData.getAuthScope()));
        tokenInfo.setPluginName(tokenData.getPluginName());
        tokenInfo.setCreatedAt(Util.toEpochSecond(tokenData.getCreatedAt()));
        tokenInfo.setDeadline(Util.toEpochSecond(tokenData.getDeadline()));
        tokenInfo.setLastUsedAt(Util.toEpochSecond(tokenData.getLastUsedAt()));
        tokenInfo.setLastUsedBrowser(tokenData.getLastUsedBrowser());
        
        if (tokenData.getLastUsedIp() != null) {
            tokenInfo.setLastUsedIp(tokenData.getLastUsedIp().getAddress());
        }
        
        return tokenInfo;
    }

}
