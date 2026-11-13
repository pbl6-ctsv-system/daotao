package pbl6.daotao.units;

import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import pbl6.daotao.units.config.UnitExportProperties;
import pbl6.daotao.units.dto.UnitExportRequest;
import pbl6.daotao.units.repository.*;
import pbl6.daotao.units.service.*;
import pbl6.p00.SliceATestDatabaseSupport;
import static org.assertj.core.api.Assertions.*;

class UnitReadDeadlineIT {
    @Test void sa054_defaultThirtySecondDeadlineDoesNotResetForProgressAndReleasesPartialBatch()throws Exception {
        SliceATestDatabaseSupport.migrate();
        String prefix="P01-DEADLINE-"+UUID.randomUUID();
        try(var c=SliceATestDatabaseSupport.open();var s=c.prepareStatement("INSERT dbo.DonVi(MaDV,TenDV) VALUES(?,N'Synthetic deadline')")){
            s.setString(1,prefix+"-1");s.executeUpdate();s.setString(1,prefix+"-2");s.executeUpdate();
        }
        var properties=UnitExportProperties.defaults(UUID.fromString("11111111-2222-3333-4444-555555555555"));
        var store=new UnitExportStore(properties,new ObjectMapper(),Clock.systemUTC());
        AtomicInteger progressed=new AtomicInteger();
        var source=new UnitChangeTrackingRepository(SliceATestDatabaseSupport.dataSource(),properties,Clock.systemUTC()){
            @Override protected void afterRowRead(long id)throws Exception {progressed.incrementAndGet();Thread.sleep(11000);}
        };
        long start=System.nanoTime();
        try {
            assertThatThrownBy(()->new UnitExportService(source,store).export(new UnitExportRequest(null,null,100,null),"ctsv"))
                .isInstanceOfSatisfying(UnitApiException.class,e->{assertThat(e.status()).isEqualTo(503);assertThat(e.code()).isEqualTo("SOURCE_UNAVAILABLE");});
            long elapsed=Duration.ofNanos(System.nanoTime()-start).toMillis();
            assertThat(progressed.get()).isGreaterThanOrEqualTo(2);assertThat(elapsed).isBetween(29000L,44000L);
            assertThat(store.activeExports()).isZero();assertThat(store.retainedBytes()).isZero();
            new ObjectMapper().writeValue(java.nio.file.Path.of("target","p01-deadline-evidence.json").toFile(),
                Map.of("deadlineMillis",30000,"elapsedMillis",elapsed,"rowsProgressed",progressed.get(),"activeExportsAfter",0,"retainedBytesAfter",0,"source","SQL_SERVER_REAL"));
        } finally {
            try(var c=SliceATestDatabaseSupport.open();var s=c.prepareStatement("DELETE dbo.DonVi WHERE MaDV IN(?,?)")){
                s.setString(1,prefix+"-1");s.setString(2,prefix+"-2");s.executeUpdate();
            }
        }
    }
}
