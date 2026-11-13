package pbl6.daotao.units;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import javax.sql.DataSource;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.*;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.*;
import org.springframework.test.context.*;
import pbl6.daotao.DaotaoApplication;
import pbl6.p00.SliceATestDatabaseSupport;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(classes=DaotaoApplication.class,webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(UnitChangeTrackingIT.DatabaseConfig.class)
@ActiveProfiles("test")
class UnitChangeTrackingIT {
    static final String EPOCH="11111111-2222-3333-4444-555555555555",KEY=UUID.randomUUID().toString();
    static final ObjectMapper JSON=new ObjectMapper();
    @LocalServerPort int port;
    String code;
    @TestConfiguration static class DatabaseConfig {
        @Bean DataSource source() throws Exception {return SliceATestDatabaseSupport.dataSource();}
    }
    @DynamicPropertySource static void configuration(DynamicPropertyRegistry r){
        r.add("slice-a.enabled",()->"true");r.add("SIS_SOURCE_EPOCH",()->EPOCH);
        r.add("sis.units.service-keys.ctsv",()->KEY);r.add("sis.units.service-resources.ctsv",()->"units");
    }
    @BeforeAll static void migrate() throws Exception {SliceATestDatabaseSupport.migrate();}
    @BeforeEach void seed() throws Exception {
        code="P01-CT-"+UUID.randomUUID();
        try(var c=SliceATestDatabaseSupport.open();var s=c.prepareStatement("INSERT dbo.DonVi(MaDV,TenDV,LuuTru,IsUs) VALUES(?,N'Đơn vị giả',NULL,N'raw-class')")){
            s.setString(1,code);s.executeUpdate();
        }
    }
    @AfterEach void cleanup() throws Exception {
        try(var c=SliceATestDatabaseSupport.open();var s=c.prepareStatement("DELETE dbo.DonVi WHERE MaDV=?")){s.setString(1,code);s.executeUpdate();}
    }
    HttpResponse<String> call(String query) throws Exception {
        var req=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/dao-tao/don-vi"+query))
                .timeout(Duration.ofSeconds(40)).header("X-Service-Key",KEY).GET().build();
        try(var client=HttpClient.newHttpClient()){return client.send(req,HttpResponse.BodyHandlers.ofString());}
    }
    JsonNode ok(String query) throws Exception {
        var r=call(query);assertThat(r.statusCode()).as("provider SQL/HTTP response").isEqualTo(200);
        JsonNode json=JSON.readTree(r.body());new pbl6.daotao.units.validation.UnitContractValidator().validate(json);return json;
    }
    @Test void sa002_fullUsesRealSourceAndBothRawFields() throws Exception {
        JsonNode n=ok("?limit=500");
        assertThat(n.path("mode").asText()).isEqualTo("full");
        boolean found=false;for(var item:n.path("items")){if(item.path("data").path("unit_code").asText().equals(code)){
            found=true;assertThat(item.path("data").path("archive_status").isNull()).isTrue();
            assertThat(item.path("data").path("unit_classification").asText()).isEqualTo("raw-class");
        }}assertThat(found).isTrue();
    }
    @Test void sa004_sae02_onlyRawUpdateAppearsInDelta() throws Exception {
        JsonNode full=ok("?limit=500");String lower=full.path("to_version").asText();
        try(var c=SliceATestDatabaseSupport.open();var s=c.prepareStatement("UPDATE dbo.DonVi SET LuuTru=N'changed' WHERE MaDV=?")){s.setString(1,code);s.executeUpdate();}
        JsonNode delta=ok("?since_version="+lower+"&source_epoch="+EPOCH);
        assertThat(delta.path("mode").asText()).isEqualTo("incremental");
        assertThat(delta.path("items").size()).isEqualTo(1);
        assertThat(delta.path("items").get(0).path("data").path("archive_status").asText()).isEqualTo("changed");
    }
    @Test void sa005_noChangesHasRealUpperVersion() throws Exception {
        JsonNode full=ok("?limit=500");
        JsonNode delta=ok("?since_version="+full.path("to_version").asText()+"&source_epoch="+EPOCH);
        assertThat(delta.path("items").isEmpty()).isTrue();
        assertThat(Long.parseLong(delta.path("to_version").asText())).isGreaterThanOrEqualTo(Long.parseLong(full.path("to_version").asText()));
    }
    @Test void sa032_sourceEpochMismatchDoesNotExportData() throws Exception {
        var r=call("?since_version=0&source_epoch=aaaaaaaa-2222-3333-4444-555555555555");
        assertThat(r.statusCode()).isEqualTo(409);
        assertThat(JSON.readTree(r.body()).path("code").asText()).isEqualTo("SOURCE_EPOCH_CHANGED");
    }
    @Test void sa035_futureLineageIsRejectedWithoutAutomaticFull()throws Exception {
        var future=call("?since_version=9223372036854775807&source_epoch="+EPOCH);
        assertThat(future.statusCode()).isEqualTo(409);
        assertThat(JSON.readTree(future.body()).path("code").asText()).isEqualTo("SOURCE_LINEAGE_INVALID");
        // Both min-valid boundary variants need a small, isolated CT history; see the fault DB gate.
    }
}

