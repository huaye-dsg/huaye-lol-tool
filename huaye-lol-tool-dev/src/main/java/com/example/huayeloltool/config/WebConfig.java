package com.example.huayeloltool.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import org.springframework.context.annotation.Bean;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    // --- SPA 路由回退的逻辑 ---
    //@Override
    //public void addResourceHandlers(ResourceHandlerRegistry registry) {
    //    registry.addResourceHandler("/**")
    //            .addResourceLocations("classpath:/static/")
    //            .resourceChain(true)
    //            .addResolver(new PathResourceResolver() {
    //                @Override
    //                protected Resource getResource(String resourcePath, Resource location) throws IOException {
    //                    if (resourcePath.startsWith("api/")) {
    //                        return null;
    //                    }
    //
    //                    Resource requestedResource = location.createRelative(resourcePath);
    //                    return requestedResource.exists() && requestedResource.isReadable() ? requestedResource
    //                            : new ClassPathResource("/static/index.html");
    //                }
    //            });
    //}

    // --- 全局 CORS 配置的逻辑 ---
    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(false); // 根据你的需求设置，通常开发时可以设为 true
        config.addAllowedOrigin("*"); // 允许所有来源，生产环境应配置具体的域名
        config.addAllowedHeader("*"); // 允许所有请求头
        config.addAllowedMethod("*"); // 允许所有方法 (GET, POST, etc.)
        source.registerCorsConfiguration("/**", config); // 对所有路径应用这个配置
        return new CorsFilter(source);
    }
}