package com.barboya.web;

import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;
import java.util.Arrays;

/** Sirve la interfaz compilada, si se configuro su carpeta como recurso estatico. */
@Configuration
class WebConfig implements WebMvcConfigurer {

    private final String[] locations;

    WebConfig(WebProperties properties) {
        this.locations = properties.getResources().getStaticLocations();
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        for (String page : SpaRoutes.PAGES) {
            registry.addViewController(page).setViewName("forward:/index.html");
        }
    }

    /** Los archivos de /assets llevan una huella en el nombre: se pueden guardar un ano. */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/assets/**")
                .addResourceLocations(Arrays.stream(locations).map(l -> l + "assets/").toArray(String[]::new))
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable());
    }
}
