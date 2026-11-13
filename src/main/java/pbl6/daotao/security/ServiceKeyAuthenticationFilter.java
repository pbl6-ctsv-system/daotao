package pbl6.daotao.security;
import java.util.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.filter.OncePerRequestFilter;
import pbl6.daotao.shared.web.ApiProblemHandler;
public final class ServiceKeyAuthenticationFilter extends OncePerRequestFilter {
    private final Map<String,String> keys;
    private final Map<String,Set<String>> permissions;
    private final ObjectMapper mapper;
    public ServiceKeyAuthenticationFilter(Map<String,String> keys,Map<String,Set<String>> permissions,ObjectMapper mapper) {
        this.keys=Map.copyOf(keys);this.permissions=Map.copyOf(permissions);this.mapper=mapper;
    }
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)
            throws ServletException,IOException {
        var headers=Collections.list(request.getHeaders("X-Service-Key"));
        String supplied=headers.size()==1?headers.getFirst():null;
        String caller=null;
        if(supplied!=null) {
            byte[] candidate=supplied.getBytes(StandardCharsets.UTF_8);
            for(var entry:keys.entrySet()) {
                if(MessageDigest.isEqual(entry.getValue().getBytes(StandardCharsets.UTF_8),candidate))
                    caller=entry.getKey();
            }
        }
        if(caller==null){reject(response,request,401,"INVALID_SERVICE_KEY");return;}
        if(!permissions.getOrDefault(caller,Set.of()).contains("units")) {
            reject(response,request,403,"SERVICE_FORBIDDEN");return;
        }
        request.setAttribute("sis.service.caller",caller);
        chain.doFilter(request,response);
    }
    private void reject(HttpServletResponse response,HttpServletRequest request,int status,String code) throws IOException {
        response.setStatus(status);response.setContentType("application/problem+json");
        mapper.writeValue(response.getOutputStream(),ApiProblemHandler.problem(status,code,null,request.getRequestURI()));
    }
}

