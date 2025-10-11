package vn.viettel.vds.promotion.validation.engine.adapter.out.config;

import feign.RequestInterceptor;
import feign.Retryer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;

@Configuration
public class FeignConfiguration {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            requestTemplate.header("User-Agent", "validation-engine/1.0");
            requestTemplate.header("Accept", "application/json");
            requestTemplate.header("Content-Type", "application/json");
        };
    }

    @Bean
    public Retryer retryer() {
        return new Retryer.Default(1000, 3000, 3);
    }

    @Bean
    public feign.Request.Options feignOptions() {
        return new feign.Request.Options(
                java.time.Duration.ofMillis(5000),   // connectTimeout
                java.time.Duration.ofMillis(10000),  // readTimeout
                true    // followRedirects
        );
    }

    @Bean
    public Executor virtualThreadExecutor() {
        return Thread.ofVirtual().factory()::newThread;
    }
}