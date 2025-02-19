package org.moera.node.model;

import jakarta.transaction.Transactional;

import org.moera.lib.node.types.FundraiserInfo;
import org.moera.lib.node.types.ProfileAttributes;
import org.moera.lib.node.types.ProfileOperations;
import org.moera.lib.node.types.SourceFormat;
import org.moera.lib.node.types.principal.Principal;
import org.moera.node.option.Options;
import org.moera.node.text.TextConverter;
import org.springframework.util.ObjectUtils;

public class ProfileAttributesUtil {

    @Transactional
    public static void toOptions(ProfileAttributes attributes, Options options, TextConverter textConverter) {
        SourceFormat bioSrcFormat = attributes.getBioSrcFormat() != null
            ? attributes.getBioSrcFormat()
            : SourceFormat.MARKDOWN;
        String bioHtml;
        if (!ObjectUtils.isEmpty(attributes.getBioSrc()) && bioSrcFormat != SourceFormat.APPLICATION) {
            bioHtml = textConverter.toHtml(bioSrcFormat, attributes.getBioSrc());
        } else {
            bioHtml = attributes.getBioSrc();
        }
        options.runInTransaction(opt -> {
            toOption("profile.full-name", attributes.getFullName(), opt);
            toOption("profile.gender", attributes.getGender(), opt);
            toOption("profile.email", attributes.getEmail(), opt);
            toOption(
                "profile.email.view",
                ProfileOperations.getViewEmail(attributes.getOperations(), Principal.ADMIN),
                opt
            );
            toOption("profile.title", attributes.getTitle(), opt);
            toOption("profile.bio.src", attributes.getBioSrc(), opt);
            toOption("profile.bio.src.format", bioSrcFormat.getValue(), opt);
            toOption("profile.bio.html", bioHtml, opt);
            toOption("profile.avatar.id", attributes.getAvatarId(), opt);
            toOption("profile.fundraisers", FundraiserInfo.serializeValue(attributes.getFundraisers()), opt);
        });
    }

    private static void toOption(String name, String value, Options options) {
        if (value != null) {
            if (!value.isEmpty()) {
                options.set(name, value);
            } else {
                options.reset(name);
            }
        }
    }

    private static void toOption(String name, Principal value, Options options) {
        if (value != null) {
            if (!value.isUnset()) {
                options.set(name, value);
            } else {
                options.reset(name);
            }
        }
    }

}
