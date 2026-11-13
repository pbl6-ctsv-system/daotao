package pbl6.daotao.units;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.*;
import pbl6.daotao.DaotaoApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.assertThat;
@SpringBootTest(classes=DaotaoApplication.class,webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class UnitExportHttpIT {
    static final String KEY=UUID.randomUUID().toString(),DENIED=UUID.randomUUID().toString();
    @LocalServerPort int port;
    @DynamicPropertySource static void configuration(DynamicPropertyRegistry r) {
        r.add("slice-a.enabled",()->"true");r.add("SIS_SOURCE_EPOCH",()->"11111111-2222-3333-4444-555555555555");
        r.add("sis.units.service-keys.ctsv",()->KEY);r.add("sis.units.service-keys.denied",()->DENIED);
        r.add("sis.units.service-resources.ctsv",()->"units");r.add("sis.units.service-resources.denied",()->"students");
    }
    HttpResponse<String> call(String key,String query) throws Exception {
        var b=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/dao-tao/don-vi"+query))
                .timeout(Duration.ofSeconds(5)).header("Accept","application/json").GET();
        if(key!=null)b.header("X-Service-Key",key);
        try(var c=HttpClient.newHttpClient()){return c.send(b.build(),HttpResponse.BodyHandlers.ofString());}
    }
    @Test void sa039_realSocketEnforcesMissingAndInvalidKey() throws Exception {
        assertThat(call(null,"").statusCode()).isEqualTo(401);
        var r=call("invalid","");
        assertThat(r.statusCode()).isEqualTo(401);assertThat(r.body()).doesNotContain(KEY,DENIED);
    }
    @Test void sa039_realSocketEnforcesResourcePermission() throws Exception {
        var r=call(DENIED,"");assertThat(r.statusCode()).isEqualTo(403);
        assertThat(new ObjectMapper().readTree(r.body()).path("code").asText()).isEqualTo("SERVICE_FORBIDDEN");
    }
    @Test void sa038_realSocketRejectsUnsupportedQuery() throws Exception {
        var r=call(KEY,"?updated_since=x");assertThat(r.statusCode()).isEqualTo(400);
    }
    @Test void sa029_realSocketDoesNotPretendUnreadySourceIsEmpty() throws Exception {
        var r=call(KEY,"");assertThat(r.statusCode()).isEqualTo(503);
        assertThat(new ObjectMapper().readTree(r.body()).path("code").asText()).isEqualTo("SYNC_NOT_READY");
        assertThat(r.headers().firstValue("Content-Type").orElse("")).contains("application/problem+json");
    }
}

