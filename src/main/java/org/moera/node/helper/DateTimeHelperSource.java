package org.moera.node.helper;

import java.time.LocalDateTime;
import java.util.Locale;

import com.github.jknack.handlebars.Options;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.Calendar;
import org.moera.node.util.Util;

@HelperSource
public class DateTimeHelperSource {

    public CharSequence cal(String pattern, Options options) {
        LocalDateTime timestamp = HelperUtils.timestampArg("date", options.hash("date"));
        Calendar calendar = Calendar.getInstance();
        Util.copyToCalendar(timestamp, calendar);
        return new SimpleDateFormat(pattern, Locale.ENGLISH).format(calendar);
    }

}
