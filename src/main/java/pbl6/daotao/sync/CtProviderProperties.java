package pbl6.daotao.sync;
import java.nio.file.Path;import java.util.*;import org.springframework.boot.context.properties.ConfigurationProperties;
@ConfigurationProperties("sis.ct26")
public record CtProviderProperties(UUID sourceEpoch,Path spoolDirectory,Map<String,String> serviceKeys,Map<String,Set<String>> serviceResources) {
 public CtProviderProperties {serviceKeys=serviceKeys==null?Map.of():Map.copyOf(serviceKeys);serviceResources=serviceResources==null?Map.of():Map.copyOf(serviceResources);if(serviceKeys.values().stream().anyMatch(String::isBlank)||new HashSet<>(serviceKeys.values()).size()!=serviceKeys.size())throw new IllegalArgumentException("Unique nonblank service credentials required");}
 @Override public String toString(){return "CtProviderProperties[credentials redacted]";}
}
