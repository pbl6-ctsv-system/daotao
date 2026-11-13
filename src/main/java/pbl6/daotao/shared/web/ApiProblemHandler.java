package pbl6.daotao.shared.web;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import pbl6.daotao.units.controller.UnitExportController;
import pbl6.daotao.units.service.UnitApiException;
import jakarta.servlet.http.HttpServletRequest;
@RestControllerAdvice(assignableTypes=UnitExportController.class)
public class ApiProblemHandler {
    public static Map<String,Object> problem(int status,String code,String field,String path) {
        Map<String,Object> p=new LinkedHashMap<>();
        p.put("type","urn:problem:sis:"+code.toLowerCase(Locale.ROOT).replace('_','-'));
        p.put("title",HttpStatus.valueOf(status).getReasonPhrase());p.put("status",status);
        p.put("detail","Không thể xử lý yêu cầu đồng bộ.");p.put("instance",path);
        p.put("code",code);p.put("traceId",UUID.randomUUID().toString());
        if(field!=null)p.put("errors",List.of(Map.of("field",field,"code",code,"message","Giá trị không hợp lệ.")));
        return p;
    }
    @ExceptionHandler(UnitApiException.class)
    ResponseEntity<Map<String,Object>> handle(UnitApiException e,HttpServletRequest request) {
        return ResponseEntity.status(e.status()).contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem(e.status(),e.code(),e.field(),request.getRequestURI()));
    }
    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String,Object>> sanitized(Exception e,HttpServletRequest request) {
        return ResponseEntity.status(503).contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem(503,"SOURCE_UNAVAILABLE",null,request.getRequestURI()));
    }
}

