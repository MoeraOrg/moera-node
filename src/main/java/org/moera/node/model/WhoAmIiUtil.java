package org.moera.node.model;

import org.moera.lib.naming.types.OperationStatus;
import org.moera.lib.node.types.WhoAmI;
import org.moera.node.global.RequestContext;

public class WhoAmIiUtil {

    public static WhoAmI build(RequestContext requestContext) {
        WhoAmI whoAmI = new WhoAmI();
        
        whoAmI.setNodeName(requestContext.nodeName());
        
        OperationStatus status = OperationStatus.forValue(
            requestContext.getOptions().getString("naming.operation.status")
        );
        whoAmI.setNodeNameChanging(
            status == OperationStatus.WAITING
            || status == OperationStatus.ADDED
            || status == OperationStatus.STARTED
        );
                
        whoAmI.setFullName(requestContext.fullName());
        whoAmI.setGender(requestContext.getOptions().getString("profile.gender"));
        whoAmI.setTitle(requestContext.getOptions().getString("profile.title"));
        
        if (requestContext.getAvatar() != null) {
            whoAmI.setAvatar(AvatarImageUtil.build(requestContext.getAvatar()));
        }
        
        if (requestContext.getOptions().getBool("frozen")) {
            whoAmI.setFrozen(true);
        }
        
        return whoAmI;
    }

}
