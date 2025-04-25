package com.tour.paymentservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebMvc // Thêm annotation này để kích hoạt cấu hình Spring MVC
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Cấu hình để xử lý các static resources
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/");

        // Thêm registry cụ thể cho các tệp HTML
        registry.addResourceHandler("/*.html")
                .addResourceLocations("classpath:/static/");

        // Cấu hình cho các tệp CSS, JS...
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/");
        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/js/");
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Định nghĩa các URL paths cụ thể để định tuyến đến các trang tĩnh
        registry.addViewController("/").setViewName("forward:/index.html");
        registry.addViewController("/payment-result").setViewName("forward:/payment-result.html");
    }
}