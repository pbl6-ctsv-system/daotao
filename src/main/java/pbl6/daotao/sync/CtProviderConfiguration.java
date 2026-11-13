package pbl6.daotao.sync;
import java.time.*;import java.util.*;import java.nio.charset.StandardCharsets;import java.security.MessageDigest;import java.io.IOException;
import javax.sql.DataSource;import jakarta.servlet.*;import jakarta.servlet.http.*;
import org.springframework.context.annotation.*;import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;import org.springframework.boot.context.properties.EnableConfigurationProperties;import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.web.filter.OncePerRequestFilter;import org.springframework.core.env.Environment;
import com.fasterxml.jackson.databind.ObjectMapper;import pbl6.daotao.shared.web.ApiProblemHandler;
@Configuration(proxyBeanMethods=false)
@ConditionalOnProperty(name="ct26.enabled",havingValue="true")
@EnableConfigurationProperties(CtProviderProperties.class)
public class CtProviderConfiguration {
 @Bean CtSnapshotReader ctSnapshotReader(DataSource ds,CtProviderProperties p,Environment e){return new CtSnapshotReader(ds,epoch(p,e),Clock.systemUTC());}
 private static UUID epoch(CtProviderProperties p,Environment e){return p.sourceEpoch()!=null?p.sourceEpoch():UUID.fromString(e.getRequiredProperty("SIS_SOURCE_EPOCH"));}
 @Bean(destroyMethod="close") DiskExportStore ctDiskStore(CtProviderProperties p){if(p.spoolDirectory()==null)throw new IllegalArgumentException("Private CT spool directory required");return new DiskExportStore(p.spoolDirectory(),Clock.systemUTC());}
 @Bean CtExportService ctExportService(List<CtBinding> bindings,CtSnapshotReader reader,DiskExportStore store,CtProviderProperties p,Environment e){return new CtExportService(bindings,reader,store,epoch(p,e));}
 @Bean FilterRegistrationBean<OncePerRequestFilter> ctServiceKeyFilter(List<CtBinding> bindings,CtProviderProperties p,ObjectMapper mapper){
  var map=new HashMap<String,String>();bindings.forEach(b->map.put(b.resource().url(),b.resource().resource()));
  var filter=new OncePerRequestFilter(){
   protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws ServletException,IOException {
    var headers=Collections.list(req.getHeaders("X-Service-Key"));String supplied=headers.size()==1?headers.getFirst():null,caller=null;
    if(supplied!=null)for(var entry:p.serviceKeys().entrySet())if(MessageDigest.isEqual(supplied.getBytes(StandardCharsets.UTF_8),entry.getValue().getBytes(StandardCharsets.UTF_8)))caller=entry.getKey();
    int status=caller==null?401:!p.serviceResources().getOrDefault(caller,Set.of()).contains(map.get(req.getRequestURI()))?403:0;
    if(status!=0){res.setStatus(status);res.setContentType("application/problem+json");res.setHeader("Cache-Control","no-store");mapper.writeValue(res.getOutputStream(),ApiProblemHandler.problem(status,status==401?"INVALID_SERVICE_KEY":"SERVICE_FORBIDDEN",null,req.getRequestURI()));return;}
    req.setAttribute("ct26.caller",caller);res.setHeader("Cache-Control","no-store");chain.doFilter(req,res);
   }
  };
  var result=new FilterRegistrationBean<OncePerRequestFilter>(filter);result.setEnabled(!bindings.isEmpty());result.setUrlPatterns(new ArrayList<>(map.keySet()));return result;
 }
}
