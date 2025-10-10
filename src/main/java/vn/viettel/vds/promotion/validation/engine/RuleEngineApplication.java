package vn.viettel.vds.promotion.validation.engine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableFeignClients
@EnableJpaRepositories(basePackages = "vn.viettel.vds.promotion.validation.engine.adapter.out.persistence.jpa.repository")
@EntityScan(basePackages = "vn.viettel.vds.promotion.validation.engine.adapter.out.persistence.jpa.entity")
public class RuleEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(RuleEngineApplication.class, args);
    }

}
