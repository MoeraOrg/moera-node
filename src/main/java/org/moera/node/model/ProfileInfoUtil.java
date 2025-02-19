package org.moera.node.model;

import java.io.IOException;
import java.util.List;

import org.moera.lib.node.types.FundraiserInfo;
import org.moera.lib.node.types.ProfileInfo;
import org.moera.lib.node.types.ProfileOperations;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.SourceFormat;
import org.moera.lib.node.types.principal.Principal;
import org.moera.node.global.RequestContext;
import org.moera.node.option.Options;
import org.moera.node.option.exception.DeserializeOptionValueException;

public class ProfileInfoUtil {

    public static ProfileInfo build(RequestContext requestContext, boolean includeSource) {
        ProfileInfo profileInfo = new ProfileInfo();
        Options options = requestContext.getOptions();
        
        profileInfo.setFullName(requestContext.fullName());
        profileInfo.setGender(options.getString("profile.gender"));
        
        Principal viewEmail = options.getPrincipal("profile.email.view");
        if (requestContext.isPrincipal(viewEmail, Scope.VIEW_PROFILE)) {
            profileInfo.setEmail(options.getString("profile.email"));
        }
        
        profileInfo.setTitle(options.getString("profile.title"));
        
        if (includeSource) {
            profileInfo.setBioSrc(options.getString("profile.bio.src"));
            profileInfo.setBioSrcFormat(SourceFormat.forValue(options.getString("profile.bio.src.format")));
        }
        
        profileInfo.setBioHtml(options.getString("profile.bio.html"));
        
        if (requestContext.getAvatar() != null) {
            profileInfo.setAvatar(AvatarInfoUtil.build(requestContext.getAvatar()));
        }
        
        profileInfo.setFundraisers(deserializeFundraisers(options));
        buildOperations(profileInfo, viewEmail);

        return profileInfo;
    }

    public static ProfileInfo build(Options options) {
        ProfileInfo profileInfo = new ProfileInfo();
        
        profileInfo.setFullName(options.getString("profile.full-name"));
        profileInfo.setGender(options.getString("profile.gender"));
        
        Principal viewEmail = options.getPrincipal("profile.email.view");
        profileInfo.setEmail(options.getString("profile.email"));
        profileInfo.setTitle(options.getString("profile.title"));
        profileInfo.setBioSrc(options.getString("profile.bio.src"));
        profileInfo.setBioSrcFormat(SourceFormat.forValue(options.getString("profile.bio.src.format")));
        profileInfo.setBioHtml(options.getString("profile.bio.html"));
        profileInfo.setFundraisers(deserializeFundraisers(options));
        buildOperations(profileInfo, viewEmail);
        
        return profileInfo;
    }

    private static List<FundraiserInfo> deserializeFundraisers(Options options) {
        String value = options.getString("profile.fundraisers");
        try {
            return FundraiserInfo.deserializeValue(value);
        } catch (IOException e) {
            throw new DeserializeOptionValueException("FundraiserInfo[]", value);
        }
    }

    private static void buildOperations(ProfileInfo info, Principal viewEmail) {
        ProfileOperations operations = new ProfileOperations();
        operations.setEdit(Principal.ADMIN);
        operations.setViewEmail(viewEmail);
        info.setOperations(operations);
    }

}
