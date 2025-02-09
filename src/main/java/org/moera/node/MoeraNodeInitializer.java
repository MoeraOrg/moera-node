package org.moera.node;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;

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
