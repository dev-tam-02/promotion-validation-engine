package vn.viettel.vds.promotion.rule.engine;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;
import vn.viettel.vds.promotion.rule.engine.application.port.out.ObjectStoragePort;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Disabled("Disabled until ObjectStoragePort implementation is available for tests")
class RuleEngineApplicationTests {

    @MockitoBean
    private ObjectStoragePort objectStoragePort;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void contextLoads() {
        assertNotNull(applicationContext, "Application context should be loaded successfully");
    }

}
