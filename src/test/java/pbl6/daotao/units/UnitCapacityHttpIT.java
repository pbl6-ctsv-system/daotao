package pbl6.daotao.units;

import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import javax.sql.DataSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import pbl6.daotao.DaotaoApplication;
import pbl6.daotao.units.config.UnitExportProperties;
import pbl6.daotao.units.repository.UnitChangeTrackingRepository;
import pbl6.daotao.units.service.*;
import pbl6.p00.SliceATestDatabaseSupport;
import static org.assertj.core.api.Assertions.*;

abstract class UnitCapacitySupport {
    static final String EPOCH="11111111-2222-3333-4444-555555555555";
    static final String KEY=UUID.randomUUID().toString();
    @LocalServerPort int port;
    @Autowired Barrier barrier;
    @Autowired UnitExportStore store;
    @Autowired UnitExportBudgetIT.TestClock clock;
    @Autowired UnitExportProperties limits;
    static class Barrier {
        final CountDownLatch reached,release=new CountDownLatch(1);
        Barrier(int builders){reached=new CountDownLatch(builders);}
    }
    @TestConfiguration(proxyBeanMethods=false)
    static class Config {
        @Bean DataSource source()throws Exception{return SliceATestDatabaseSupport.dataSource();}
        @Bean @Primary UnitExportBudgetIT.TestClock clock(){return new UnitExportBudgetIT.TestClock();}
        @Bean @Primary UnitExportProperties testLimits(Environment environment){
            var defaults=UnitExportProperties.defaults(UUID.fromString(EPOCH));
            return new UnitExportProperties(defaults.sourceEpoch(),10000,8L*1024*1024,2,
                environment.getRequiredProperty("p01.test.total-bytes",Long.class),Duration.ofSeconds(30),Duration.ofMinutes(15));
        }
        @Bean Barrier barrier(UnitExportProperties limits){return new Barrier(limits.maxRetainedBytes()==16L*1024*1024?2:1);}
        @Bean @Primary UnitExportService blockingExporter(DataSource ds,UnitExportProperties limits,Clock clock,UnitExportStore store,Barrier barrier){
            var repository=new UnitChangeTrackingRepository(ds,limits,clock){
                @Override protected void afterUpperVersionCaptured(java.sql.Connection connection,long upper)throws Exception{
                    barrier.reached.countDown();
                    if(!barrier.release.await(15,TimeUnit.SECONDS))throw new IllegalStateException("Test builder barrier timeout");
                }
            };
            return new UnitExportService(repository,store);
        }
    }
    @BeforeAll static void migration()throws Exception{SliceATestDatabaseSupport.migrate();}
    @org.springframework.test.context.DynamicPropertySource
    static void configuration(org.springframework.test.context.DynamicPropertyRegistry r){
        r.add("slice-a.enabled",()->"true");r.add("SIS_SOURCE_EPOCH",()->EPOCH);
        r.add("sis.units.service-keys.ctsv",()->KEY);r.add("sis.units.service-resources.ctsv",()->"units");
    }
    void verifyCapacity(int builders)throws Exception{
        var request=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/dao-tao/don-vi?limit=500"))
            .timeout(Duration.ofSeconds(25)).header("X-Service-Key",KEY).GET().build();
        try(var client=HttpClient.newHttpClient()){
            var inProgress=new ArrayList<CompletableFuture<HttpResponse<String>>>();
            try{
                for(int i=0;i<builders;i++)inProgress.add(client.sendAsync(request,HttpResponse.BodyHandlers.ofString()));
                assertThat(barrier.reached.await(10,TimeUnit.SECONDS)).isTrue();
                assertThat(store.activeExports()).isEqualTo(builders);
                assertThat(store.retainedBytes()).isEqualTo(builders*8L*1024*1024);
                var rejected=client.send(request,HttpResponse.BodyHandlers.ofString());
                assertThat(rejected.statusCode()).isEqualTo(503);
                assertThat(new ObjectMapper().readTree(rejected.body()).path("code").asText()).isEqualTo("EXPORT_CAPACITY_EXCEEDED");
                new ObjectMapper().writeValue(java.nio.file.Path.of("target","p01-capacity-"+limits.maxRetainedBytes()+".json").toFile(),
                    Map.of("totalBudgetBytes",limits.maxRetainedBytes(),"buildersAtBarrier",builders,"maxActive",2,"status",503,"source","SQL_SERVER_REAL","transport","REAL_HTTP"));
            }finally{barrier.release.countDown();}
            for(var response:inProgress)assertThat(response.get(10,TimeUnit.SECONDS).statusCode()).isEqualTo(200);
        }finally{
            clock.advance(Duration.ofMinutes(16));store.prune();
            assertThat(store.activeExports()).isZero();assertThat(store.retainedBytes()).isZero();
        }
    }
}
@SpringBootTest(classes=DaotaoApplication.class,webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties="p01.test.total-bytes=16777216")
@Import(UnitCapacitySupport.Config.class)
class UnitCapacityHttpIT extends UnitCapacitySupport {
    @Test void sa052_thirdHttpRequestIsRejectedWhileTwoRealSqlExportsAreStillBuilding()throws Exception{verifyCapacity(2);}
}
@SpringBootTest(classes=DaotaoApplication.class,webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties="p01.test.total-bytes=12582912")
@Import(UnitCapacitySupport.Config.class)
class UnitTotalBudgetHttpIT extends UnitCapacitySupport {
    @Test void sa053_lowerTestBudgetIsRejectedOverHttpBeforeSlotLimitAndDoesNotDamageFirstExport()throws Exception{verifyCapacity(1);}
}
