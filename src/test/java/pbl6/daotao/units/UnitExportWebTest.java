package pbl6.daotao.units;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pbl6.daotao.security.ServiceKeyAuthenticationFilter;
import pbl6.daotao.shared.web.ApiProblemHandler;
import pbl6.daotao.units.controller.UnitExportController;
import pbl6.daotao.units.service.UnitExportService;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class UnitExportWebTest {
    private MockMvc mvc; private UnitExportService source;
    @BeforeEach void setup(){
        source=spy(new UnitExportService());
        mvc=MockMvcBuilders.standaloneSetup(new UnitExportController(source))
                .setControllerAdvice(new ApiProblemHandler())
                .addFilters(new ServiceKeyAuthenticationFilter(Map.of("ctsv","fixture-key","other","fixture-other"),
                    Map.of("ctsv",Set.of("units"),"other",Set.of("students")),new ObjectMapper())).build();
    }
    @Test void sa039_missingKeyNeverCallsSource() throws Exception {
        mvc.perform(get(UnitExportController.PATH)).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("INVALID_SERVICE_KEY"));
        verifyNoInteractions(source);
    }
    @Test void sa039_invalidKeyNeverCallsSource() throws Exception {
        mvc.perform(get(UnitExportController.PATH).header("X-Service-Key","incorrect")).andExpect(status().isUnauthorized());
        verifyNoInteractions(source);
    }
    @Test void sa039_validCallerWithoutUnitsIsForbidden() throws Exception {
        mvc.perform(get(UnitExportController.PATH).header("X-Service-Key","fixture-other")).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SERVICE_FORBIDDEN"));verifyNoInteractions(source);
    }
    @Test void sa038_queryAndBodyValidationPrecedeExporter() throws Exception {
        mvc.perform(get(UnitExportController.PATH).header("X-Service-Key","fixture-key").param("updated_since","x"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors[0].field").value("updated_since"));
        mvc.perform(get(UnitExportController.PATH).header("X-Service-Key","fixture-key").content("{}"))
                .andExpect(status().isBadRequest());verifyNoInteractions(source);
    }
    @Test void sa049_unreadySourceHasSanitizedProblemShape() throws Exception {
        mvc.perform(get(UnitExportController.PATH).header("X-Service-Key","fixture-key")).andExpect(status().isServiceUnavailable())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.code").value("SYNC_NOT_READY")).andExpect(jsonPath("$.type").exists())
                .andExpect(jsonPath("$.title").exists()).andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.detail").exists()).andExpect(jsonPath("$.instance").value(UnitExportController.PATH))
                .andExpect(jsonPath("$.traceId").exists()).andExpect(jsonPath("$.errors").doesNotExist());
    }
}

