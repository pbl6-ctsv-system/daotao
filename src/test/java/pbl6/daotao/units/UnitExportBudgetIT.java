package pbl6.daotao.units;
import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.util.*;
import javax.sql.DataSource;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.*;
import org.springframework.test.context.*;
import pbl6.daotao.DaotaoApplication;
import pbl6.daotao.units.service.UnitExportStore;
import pbl6.p00.SliceATestDatabaseSupport;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(classes=DaotaoApplication.class,webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(UnitExportBudgetIT.Config.class)
@ActiveProfiles("test")
class UnitExportBudgetIT {
    static final String KEY=UUID.randomUUID().toString(),ALT=UUID.randomUUID().toString();
    static final ObjectMapper JSON=new ObjectMapper();
    @LocalServerPort int port;
    @Autowired UnitExportStore store;
    @Autowired TestClock clock;
    String prefix;
    int sequence;
    static class TestClock extends Clock {
        private Instant now=Instant.parse("2026-11-11T00:00:00Z");
        synchronized void advance(Duration value){now=now.plus(value);}
        public ZoneId getZone(){return ZoneOffset.UTC;}
        public Clock withZone(ZoneId zone){return this;}
        public synchronized Instant instant(){return now;}
    }
    @TestConfiguration static class Config {
        @Bean DataSource source()throws Exception{return SliceATestDatabaseSupport.dataSource();}
        @Bean @Primary TestClock clock(){return new TestClock();}
    }
    @DynamicPropertySource static void configure(DynamicPropertyRegistry r){
        r.add("slice-a.enabled",()->"true");r.add("SIS_SOURCE_EPOCH",()->"11111111-2222-3333-4444-555555555555");
        r.add("sis.units.service-keys.ctsv",()->KEY);r.add("sis.units.service-resources.ctsv",()->"units");
        r.add("sis.units.service-keys.alt",()->ALT);r.add("sis.units.service-resources.alt",()->"units");
    }
    @BeforeAll static void migrate()throws Exception{SliceATestDatabaseSupport.migrate();}
    @BeforeEach void initialize(){prefix="P01-BUDGET-"+UUID.randomUUID()+"-";}
    @AfterEach void cleanup()throws Exception {
        try(var c=SliceATestDatabaseSupport.open();var s=c.prepareStatement("DELETE dbo.DonVi WHERE MaDV LIKE ?")){
            s.setString(1,prefix+"%");s.executeUpdate();
        }
        clock.advance(Duration.ofMinutes(16));store.prune();
        assertThat(store.activeExports()).isZero();assertThat(store.retainedBytes()).isZero();
    }
    void addRows(int count,boolean wide)throws Exception {
        try(var c=SliceATestDatabaseSupport.open();var s=c.prepareStatement("INSERT dbo.DonVi(MaDV,TenDV,MaBP,LuuTru,IsUs) VALUES(?,?,?,?,?)")){
            c.setAutoCommit(false);
            try{
                for(int i=0;i<count;i++){
                    s.setString(1,prefix+(sequence++));s.setString(2,wide?"ệ".repeat(512):"Synthetic unit");
                    s.setString(3,wide?"ệ".repeat(128):null);s.setString(4,wide?"ệ".repeat(128):null);s.setString(5,wide?"ệ".repeat(128):null);
                    s.addBatch();if(i%250==249)s.executeBatch();
                }
                s.executeBatch();c.commit();
            }catch(Exception e){c.rollback();throw e;}
        }
    }
    HttpResponse<String> call(String query,String key)throws Exception {
        var r=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/dao-tao/don-vi"+query))
                .timeout(Duration.ofSeconds(40)).header("X-Service-Key",key).GET().build();
        try(var c=HttpClient.newHttpClient()){return c.send(r,HttpResponse.BodyHandlers.ofString());}
    }
    JsonNode ok(String query)throws Exception {
        var r=call(query,KEY);assertThat(r.statusCode()).isEqualTo(200);
        var n=JSON.readTree(r.body());new pbl6.daotao.units.validation.UnitContractValidator().validate(n);return n;
    }
    @Test void sa009_046_052_055_stableReplayCallerBindingCapacityAndTtlOverActualHttp()throws Exception {
        addRows(2,false);
        JsonNode first=ok("?limit=1");String cursor=first.path("next_cursor").asText();
        try(var c=SliceATestDatabaseSupport.open();var s=c.prepareStatement("UPDATE dbo.DonVi SET TenDV=N'changed-after-export' WHERE MaDV LIKE ?")){s.setString(1,prefix+"%");s.executeUpdate();}
        var next=call("?cursor="+cursor,KEY);assertThat(next.statusCode()).isEqualTo(200);
        assertThat(call("?cursor="+cursor,KEY).body()).isEqualTo(next.body());
        assertThat(call("?cursor="+cursor,ALT).statusCode()).isEqualTo(403);
        ok("?limit=1");
        var capacity=call("?limit=1",KEY);assertThat(capacity.statusCode()).isEqualTo(503);
        assertThat(JSON.readTree(capacity.body()).path("code").asText()).isEqualTo("EXPORT_CAPACITY_EXCEEDED");
        clock.advance(Duration.ofMinutes(14));assertThat(call("?cursor="+cursor,KEY).body()).isEqualTo(next.body());
        clock.advance(Duration.ofMinutes(1));
        var expired=call("?cursor="+cursor,KEY);assertThat(expired.statusCode()).isEqualTo(409);
        assertThat(JSON.readTree(expired.body()).path("code").asText()).isEqualTo("EXPORT_EXPIRED");
    }
    @Test void sa050_defaultTenThousandRowsBoundaryAndOverflowAreRealSqlHttp()throws Exception {
        long existing;try(var c=SliceATestDatabaseSupport.open()){existing=SliceATestDatabaseSupport.scalar(c,"SELECT COUNT(*) FROM dbo.DonVi");}
        assertThat(existing).as("owned test dataset should be small before quota setup").isLessThan(1000);
        addRows((int)(10000-existing),false);
        var full=ok("?limit=500");assertThat(full.path("items").size()).isEqualTo(500);
        addRows(1,false);
        var over=call("?limit=500",KEY);assertThat(over.statusCode()).isEqualTo(503);
        assertThat(JSON.readTree(over.body()).path("code").asText()).isEqualTo("EXPORT_LIMIT_EXCEEDED");
        assertThat(store.activeExports()).isEqualTo(1);
    }
    @Test void sa051_defaultEightMiBUtf8LimitAbortsRealSqlExportWithoutTruncating()throws Exception {
        addRows(4000,true);
        var result=call("?limit=500",KEY);assertThat(result.statusCode()).isEqualTo(503);
        assertThat(JSON.readTree(result.body()).path("code").asText()).isEqualTo("EXPORT_LIMIT_EXCEEDED");
        assertThat(store.activeExports()).isZero();assertThat(store.retainedBytes()).isZero();
    }
    @Test void sa050_incrementalDefaultTenThousandRowsBoundaryAndOverflow()throws Exception {
        long lower;try(var c=SliceATestDatabaseSupport.open()){lower=SliceATestDatabaseSupport.scalar(c,"SELECT CHANGE_TRACKING_CURRENT_VERSION()");}
        addRows(10000,false);
        String query="?since_version="+lower+"&source_epoch=11111111-2222-3333-4444-555555555555&limit=500";
        assertThat(ok(query).path("items").size()).isEqualTo(500);
        addRows(1,false);
        var over=call(query,KEY);assertThat(over.statusCode()).isEqualTo(503);
        assertThat(JSON.readTree(over.body()).path("code").asText()).isEqualTo("EXPORT_LIMIT_EXCEEDED");
        assertThat(store.activeExports()).isEqualTo(1);
    }
    @Test void sa058_twoNearLimitBatchesStayWithinRecordedHeapAndProcessBudgetAcrossThreeCycles()throws Exception {
        long heapBudget=512L*1024*1024,processBudget=1024L*1024*1024;
        var os=(com.sun.management.OperatingSystemMXBean)java.lang.management.ManagementFactory.getOperatingSystemMXBean();
        assertThat(Runtime.getRuntime().maxMemory()).isLessThanOrEqualTo(heapBudget);
        var samples=new ArrayList<Map<String,Object>>();
        samples.add(memory("before",processBudget)); // Budgets recorded before dataset/export allocation.
        JSON.writerWithDefaultPrettyPrinter().writeValue(java.nio.file.Path.of("target","p01-memory-evidence.json").toFile(),
            Map.of("heapBudgetBytes",heapBudget,"processBudgetBytes",processBudget,"physicalRamBytes",os.getTotalMemorySize(),"samples",samples));
        addRows(2600,true);
        for(int cycle=0;cycle<3;cycle++){
            JsonNode first=ok("?limit=500"),second=ok("?limit=500");
            assertThat(store.activeExports()).isEqualTo(2);
            assertThat(store.retainedBytes()).isBetween(14L*1024*1024,16L*1024*1024);
            samples.add(memory("retained-"+cycle,processBudget));
            assertThat(call("?cursor="+first.path("next_cursor").asText(),KEY).statusCode()).isEqualTo(200);
            assertThat(call("?cursor="+second.path("next_cursor").asText(),KEY).statusCode()).isEqualTo(200);
            clock.advance(Duration.ofMinutes(16));store.prune();
            assertThat(store.activeExports()).isZero();assertThat(store.retainedBytes()).isZero();
            samples.add(memory("released-"+cycle,processBudget));
        }
        JSON.writerWithDefaultPrettyPrinter().writeValue(java.nio.file.Path.of("target","p01-memory-evidence.json").toFile(),
            Map.of("java",System.getProperty("java.runtime.version"),"heapBudgetBytes",heapBudget,"heapMaxBytes",Runtime.getRuntime().maxMemory(),
                "processBudgetBytes",processBudget,"physicalRamBytes",os.getTotalMemorySize(),"cycles",3,"samples",samples));
    }
    Map<String,Object> memory(String stage,long processBudget)throws Exception {
        long heap=java.lang.management.ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
        var process=new ProcessBuilder("powershell.exe","-NoProfile","-NonInteractive","-WindowStyle","Hidden","-Command",
            "(Get-Process -Id "+ProcessHandle.current().pid()+").WorkingSet64").redirectErrorStream(true).start();
        String output=new String(process.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).trim();
        assertThat(process.waitFor()).isZero();long rss=Long.parseLong(output);
        assertThat(rss).isLessThan(processBudget);
        return Map.of("stage",stage,"heapUsedBytes",heap,"workingSetBytes",rss,"exportBytes",store.retainedBytes(),"activeExports",store.activeExports());
    }
}

