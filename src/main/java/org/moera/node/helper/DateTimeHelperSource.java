package org.moera.node.helper;

import java.time.LocalDateTime;
import java.util.Locale;

import javax.inject.Inject;

import com.github.jknack.handlebars.Handlebars.SafeString;
import com.github.jknack.handlebars.Options;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.Calendar;
import org.moera.node.global.RequestContext;
import org.moera.node.util.Util;

@HelperSource
public class DateTimeHelperSource {

    @Inject
    private RequestContext requestContext;

    public CharSequence cal(String pattern, Options options) {
        Long epochSeconds = HelperUtil.integerArg("es", options.hash("es"));
        LocalDateTime timestamp = epochSeconds != null
                ? Util.toTimestamp(epochSeconds).toLocalDateTime()
                : HelperUtil.timestampArg("date", options.hash("date"));
        Calendar calendar = Calendar.getInstance();
        Util.copyToCalendar(timestamp, calendar);
        String exactDate = new SimpleDateFormat(pattern, Locale.ENGLISH).format(calendar);
        String fromNowOption = options.hash("fromNow");
        boolean fromNow = fromNowOption != null ? requestContext.getOptions().getBool(fromNowOption) : false;
        if (!fromNow) {
            return exactDate;
        } else {
            return new SafeString(String.format("<span title=\"%s\">%s</span>", exactDate, Util.fromNow(timestamp)));
        }
    }

}
