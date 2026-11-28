package com.taskcenter.config;

import com.taskcenter.security.resolver.CurrentUserArgumentResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    static final List<String> ROOT_ASSET_PATHS = List.of(
            "/style.css", "/app.js", "/games.js", "/ui-bind.js",
            "/piyola-3d.png", "/choylashamiz-logo.png",
            "/eclipse/theme.css", "/eclipse/scene.css", "/eclipse/scene.js",
            "/eclipse/cosmos.webp", "/eclipse/piyola.webp", "/eclipse/porcelain.webp",
            "/eclipse/fonts/rufina-bold.ttf", "/eclipse/fonts/barlow-semi-bold.ttf",
            "/arcade/score-bridge.js", "/arcade/LICENSE",
+            "/arcade/04-pong.html", "/arcade/04-pong.js",
            "/arcade/11-simon-says.html", "/arcade/11-simon-says.js",
            "/arcade/13-asteroids.html", "/arcade/13-asteroids.js",
            "/arcade/17-lunar-lander.html", "/arcade/17-lunar-lander.js",
            "/arcade/35-reversi.html", "/arcade/35-reversi.js",
            "/arcade/49-mini-golf.html", "/arcade/49-mini-golf.js",
            "/arcade/54-submarine.html", "/arcade/54-submarine.js",
            "/arcade/71-tower-defense.html", "/arcade/71-tower-defense.js",
            "/arcade/72-pinball.html", "/arcade/72-pinball.js",
            "/arcade/81-orbit-dodge.html", "/arcade/81-orbit-dodge.js");

    private final CurrentUserArgumentResolver currentUserArgumentResolver;

    public WebMvcConfig(CurrentUserArgumentResolver currentUserArgumentResolver) {
        this.currentUserArgumentResolver = currentUserArgumentResolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserArgumentResolver);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/app/**")
                .addResourceLocations("classpath:/static/app/");
        // A forward from /app keeps the browser URL, so relative assets resolve from /.
        registry.addResourceHandler(ROOT_ASSET_PATHS.toArray(String[]::new))
                .addResourceLocations("classpath:/static/app/");
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("redirect:/app/");
        registry.addViewController("/app").setViewName("forward:/app/index.html");
        registry.addViewController("/app/").setViewName("forward:/app/index.html");
    }
}
