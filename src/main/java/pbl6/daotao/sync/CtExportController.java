package pbl6.daotao.sync;
import java.io.IOException;import jakarta.servlet.http.HttpServletRequest;import org.springframework.web.bind.annotation.*;import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;import org.springframework.web.server.ResponseStatusException;import org.springframework.http.HttpStatus;import com.fasterxml.jackson.databind.node.ObjectNode;
@RestController @ConditionalOnProperty(name="ct26.enabled",havingValue="true")
public class CtExportController {
 private final CtExportService service;public CtExportController(CtExportService service){this.service=service;}
 @GetMapping(value="/api/dao-tao/{tenTaiNguyen}",produces="application/json")
 public ObjectNode export(HttpServletRequest request)throws IOException {
  if(service.binding(request.getRequestURI())==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND);
  return service.export(request.getRequestURI(),request.getParameterMap(),request.getInputStream().read()!=-1,(String)request.getAttribute("ct26.caller"));
 }
}
