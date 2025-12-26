package vn.viettel.vds.promotion.rule.engine.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class SchedulingConfig {
    // Enable scheduling for OutboxEventProcessor
}