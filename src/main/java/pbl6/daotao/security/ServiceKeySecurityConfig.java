package pbl6.daotao.security;
import java.util.*;
import java.time.Clock;
import javax.sql.DataSource;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.bind.*;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import pbl6.daotao.units.config.UnitExportProperties;
import pbl6.daotao.units.service.UnitExportService;
import pbl6.daotao.units.service.UnitExportStore;
import pbl6.daotao.units.repository.UnitChangeTrackingRepository;
@Configuration
@ConditionalOnProperty(name="slice-a.enabled",havingValue="true")
public class ServiceKeySecurityConfig {
    @Bean UnitExportProperties unitExportProperties(Environment env) {
        String epoch=env.getRequiredProperty("SIS_SOURCE_EPOCH");
        if(epoch.length()!=36)throw new IllegalArgumentException("A canonical persistent source epoch is required");
        return UnitExportProperties.defaults(UUID.fromString(epoch));
    }
    @Bean @ConditionalOnMissingBean(Clock.class) Clock unitsClock(){return Clock.systemUTC();}
    @Bean UnitExportStore unitExportStore(UnitExportProperties properties,ObjectMapper mapper,Clock clock){return new UnitExportStore(properties,mapper,clock);}
    @Bean UnitExportService unitExportService(ObjectProvider<DataSource> sources,UnitExportProperties properties,Clock clock,UnitExportStore store){
        var source=sources.getIfAvailable();
        return source==null?new UnitExportService():new UnitExportService(new UnitChangeTrackingRepository(source,properties,clock),store);
    }
    @Bean FilterRegistrationBean<ServiceKeyAuthenticationFilter> serviceKeyFilter(Environment env,ObjectMapper mapper) {
        Map<String,String> keys=Binder.get(env).bind("sis.units.service-keys",Bindable.mapOf(String.class,String.class)).orElse(Map.of());
        Map<String,String> grants=Binder.get(env).bind("sis.units.service-resources",Bindable.mapOf(String.class,String.class)).orElse(Map.of());
        if(keys.isEmpty()||keys.values().stream().anyMatch(String::isBlank)||new HashSet<>(keys.values()).size()!=keys.size())
            throw new IllegalArgumentException("Unique service credentials must be configured");
        Map<String,Set<String>> permissions=new HashMap<>();
        grants.forEach((caller,value)->permissions.put(caller,Arrays.stream(value.split(",")).map(String::trim).collect(Collectors.toUnmodifiableSet())));
        var registration=new FilterRegistrationBean<>(new ServiceKeyAuthenticationFilter(keys,permissions,mapper));
        registration.addUrlPatterns(pbl6.daotao.units.controller.UnitExportController.PATH);registration.setOrder(-100);
        return registration;
    }
}

