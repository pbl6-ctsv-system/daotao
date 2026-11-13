package pbl6.daotao.units.controller;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;
import pbl6.daotao.units.dto.*;
import pbl6.daotao.units.service.UnitExportService;
@RestController
@ConditionalOnProperty(name="slice-a.enabled",havingValue="true")
public class UnitExportController {
    public static final String PATH="/api/dao-tao/don-vi";
    private final UnitExportService service;
    public UnitExportController(UnitExportService service){this.service=service;}
    @GetMapping(value=PATH,produces="application/json")
    public UnitExportResponse export(HttpServletRequest request) throws IOException {
        boolean body=request.getInputStream().read()!=-1;
        var query=UnitExportRequest.parse(request.getParameterMap(),body);
        return service.export(query,(String)request.getAttribute("sis.service.caller"));
    }
}

