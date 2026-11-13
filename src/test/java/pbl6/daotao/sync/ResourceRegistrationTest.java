package pbl6.daotao.sync;

import java.nio.file.*;
import java.util.*;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ResourceRegistrationTest {
    @Configuration(proxyBeanMethods=false)
    @ComponentScan(basePackageClasses=CtProviderConfiguration.class)
    static class Config {
        @Bean DataSource dataSource(){return mock(DataSource.class);}
        @Bean com.fasterxml.jackson.databind.ObjectMapper objectMapper(){return new com.fasterxml.jackson.databind.ObjectMapper();}
    }

    @Test void productionRegisters23ApprovedP02ResourcesAndNeverStaffOrUnits() throws Exception {
        Path spool=Files.createTempDirectory("p02-resource-registration-");
        new ApplicationContextRunner().withUserConfiguration(Config.class)
            .withPropertyValues("ct26.enabled=true", "sis.ct26.source-epoch=11111111-1111-4111-8111-111111111111",
                "sis.ct26.spool-directory="+spool, "sis.ct26.service-keys.ctsv=test-only",
                "sis.ct26.service-resources.ctsv=ethnicities")
            .run(context->{
                assertThat(context).hasNotFailed();
                var resources=context.getBeansOfType(CtBinding.class).values().stream().map(b->b.resource().resource()).toList();
                assertThat(resources).hasSize(23).contains("ethnicities","students","courses","student-progress")
                    .doesNotContain("units","staff","class-advisors");
            });
    }
}
