package pbl6.daotao.sync;
import java.util.Map;import jakarta.servlet.http.HttpServletRequest;import org.springframework.http.*;import org.springframework.web.bind.annotation.*;import org.springframework.web.server.ResponseStatusException;import pbl6.daotao.shared.web.ApiProblemHandler;
@RestControllerAdvice(assignableTypes=CtExportController.class)
public class CtProblemHandler {
 @ExceptionHandler(CtApiException.class) ResponseEntity<Map<String,Object>> problem(CtApiException e,HttpServletRequest r){return ResponseEntity.status(e.status).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(ApiProblemHandler.problem(e.status,e.code,e.field,r.getRequestURI()));}
 @ExceptionHandler(ResponseStatusException.class) ResponseEntity<Void> missing(ResponseStatusException e){return ResponseEntity.status(e.getStatusCode()).build();}
 @ExceptionHandler(Exception.class) ResponseEntity<Map<String,Object>> sanitized(Exception e,HttpServletRequest r){return ResponseEntity.status(503).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(ApiProblemHandler.problem(503,"SOURCE_UNAVAILABLE",null,r.getRequestURI()));}
}
