package org.moera.node.global;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedOutput;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

public class SyndFeedHttpMessageConverter extends AbstractHttpMessageConverter<SyndFeed> {

    public SyndFeedHttpMessageConverter() {
        super(StandardCharsets.UTF_8, MediaType.APPLICATION_RSS_XML);
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return SyndFeed.class.isAssignableFrom(clazz);
    }

    @Override
    protected SyndFeed readInternal(
        Class<? extends SyndFeed> clazz, HttpInputMessage inputMessage
    ) throws HttpMessageNotReadableException {
        throw new HttpMessageNotReadableException("Reading SyndFeed is not implemented", inputMessage);
    }

    @Override
    protected void writeInternal(
        SyndFeed syndFeed, HttpOutputMessage outputMessage
    ) throws IOException, HttpMessageNotWritableException {
        SyndFeedOutput feedOutput = new SyndFeedOutput();
        try {
            Writer writer = new OutputStreamWriter(outputMessage.getBody(), StandardCharsets.UTF_8);
            feedOutput.output(syndFeed, writer);
        } catch (FeedException ex) {
            throw new HttpMessageNotWritableException("Could not write SyndFeed: " + ex.getMessage(), ex);
        }
    }

}
