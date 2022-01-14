package org.moera.node.ui.helper;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
        String isoDate = timestamp.toInstant(ZoneOffset.UTC).toString();
        Calendar calendar = Calendar.getInstance();
        Util.copyToCalendar(timestamp, calendar);
        String exactDate = new SimpleDateFormat(pattern, Locale.ENGLISH).format(calendar);
        if (isDisplayDateFromNow(options)) {
            return new SafeString(String.format("<time datetime=\"%s\" title=\"%s\">%s</time>",
                    isoDate, exactDate, Util.fromNow(timestamp)));
        } else {
            return new SafeString(String.format("<time datetime=\"%s\">%s</time>", isoDate, exactDate));
        }
    }

    private boolean isDisplayDateFromNow(Options options) {
        String fromNowOption = options.hash("fromNow");
        boolean fromNow = false;
        if (fromNowOption != null) {
            switch (fromNowOption) {
                case "true":
                    fromNow = true;
                    break;
                case "false":
                    break;
                default:
                    fromNow = requestContext.getOptions().getBool(fromNowOption);
            }
        }
        return fromNow;
    }

}
