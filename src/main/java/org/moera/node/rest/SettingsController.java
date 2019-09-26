package org.moera.node.rest;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;

import org.moera.node.global.Admin;
import org.moera.node.global.ApiController;
import org.moera.node.global.RequestContext;
import org.moera.node.model.SettingInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@ApiController
@RequestMapping("/moera/api/settings")
public class SettingsController {

    private static Logger log = LoggerFactory.getLogger(SettingsController.class);

    @Inject
    private RequestContext requestContext;

    @GetMapping
    @Admin
    @ResponseBody
    public List<SettingInfo> get(@RequestParam(required = false) String prefix) {
        log.info("GET /settings");

        List<SettingInfo> list = new ArrayList<>();
        requestContext.getOptions().forEach((name, value, optionType) -> {
            if (prefix != null && !name.startsWith(prefix)) {
                return;
            }
            String s = optionType != null ? optionType.getString(value) : value.toString();
            list.add(new SettingInfo(name, s));
        });
        list.sort(Comparator.comparing(SettingInfo::getName));

        return list;
    }

}
