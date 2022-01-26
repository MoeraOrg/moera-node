package org.moera.node;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import com.twelvemonkeys.servlet.image.IIOProviderContextListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.WebApplicationInitializer;

@Configuration
public class MoeraNodeInitializer implements WebApplicationInitializer {

    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {
        servletContext.addListener(IIOProviderContextListener.class);
    }

}
