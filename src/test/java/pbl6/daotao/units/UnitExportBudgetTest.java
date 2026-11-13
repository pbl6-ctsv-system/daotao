package pbl6.daotao.units;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import pbl6.daotao.units.config.UnitExportProperties;
import pbl6.daotao.units.dto.*;
import pbl6.daotao.units.repository.UnitChangeTrackingRepository.Metadata;
import pbl6.daotao.units.service.*;
import static org.assertj.core.api.Assertions.*;

class UnitExportBudgetTest {
    static final UUID EPOCH=UUID.fromString("11111111-2222-3333-4444-555555555555");
    static class TestClock extends Clock {
        Instant now=Instant.parse("2026-11-11T00:00:00Z");
        public ZoneId getZone(){return ZoneOffset.UTC;} public Clock withZone(ZoneId zone){return this;}
        public Instant instant(){return now;}
    }
    UnitChangeDto item(int id,String name){return new UnitChangeDto(""+id,"upsert",null,new UnitDataDto("DV-"+id,name,null,"2026-11-11T00:00:00Z",true,null,"raw"));}
    void begin(UnitExportStore.Reservation r){r.start(new Metadata("full",null,"123","2026-11-11T00:00:00Z"));}
    UnitExportProperties small(int rows,long bytes,int active,long retained){return new UnitExportProperties(EPOCH,rows,bytes,active,retained,Duration.ofSeconds(30),Duration.ofMinutes(15));}
    @Test void sa050_rowLimitIsCheckedBeforeRetainingExtraItemAndFailureReleasesBudget() {
        var store=new UnitExportStore(small(2,10000,2,20000),new ObjectMapper(),new TestClock());
        try(var r=store.reserve(new UnitExportRequest(null,null,1,null),"ctsv")){
            begin(r);r.append(item(1,"a"));r.append(item(2,"b"));
            assertThatThrownBy(()->r.append(item(3,"c"))).isInstanceOfSatisfying(UnitApiException.class,e->assertThat(e.code()).isEqualTo("EXPORT_LIMIT_EXCEEDED"));
        }
        assertThat(store.activeExports()).isZero();assertThat(store.retainedBytes()).isZero();
    }
    @Test void sa051_utf8BytesIncludeEnvelopeAndUnicodeAndNeverTruncate() {
        var store=new UnitExportStore(small(10,700,2,1400),new ObjectMapper(),new TestClock());
        try(var r=store.reserve(new UnitExportRequest(null,null,100,null),"ctsv")){
            begin(r);
            assertThatThrownBy(()->r.append(item(1,"ệ".repeat(512)))).isInstanceOfSatisfying(UnitApiException.class,e->assertThat(e.code()).isEqualTo("EXPORT_LIMIT_EXCEEDED"));
        }
        assertThat(store.retainedBytes()).isZero();
    }
    @Test void sa052_buildingExportsConsumeSlotsBeforeAnySourceRead() {
        var store=new UnitExportStore(small(10,10000,2,20000),new ObjectMapper(),new TestClock());
        try(var a=store.reserve(new UnitExportRequest(null,null,1,null),"ctsv");var b=store.reserve(new UnitExportRequest(null,null,1,null),"ctsv")){
            assertThat(store.activeExports()).isEqualTo(2);
            assertThatThrownBy(()->store.reserve(new UnitExportRequest(null,null,1,null),"ctsv"))
                .isInstanceOfSatisfying(UnitApiException.class,e->assertThat(e.code()).isEqualTo("EXPORT_CAPACITY_EXCEEDED"));
        }
        assertThat(store.activeExports()).isZero();
    }
    @Test void sa053_totalReservationGuardIsIsolatedByDocumentedLowerTestBudget() {
        var store=new UnitExportStore(small(10,10000,2,15000),new ObjectMapper(),new TestClock());
        try(var a=store.reserve(new UnitExportRequest(null,null,1,null),"ctsv")){
            assertThatThrownBy(()->store.reserve(new UnitExportRequest(null,null,1,null),"ctsv"))
                .isInstanceOfSatisfying(UnitApiException.class,e->assertThat(e.code()).isEqualTo("EXPORT_CAPACITY_EXCEEDED"));
        }
        assertThat(store.retainedBytes()).isZero();
    }
    @Test void sa009_046_055_cursorReplayBindingAndNonSlidingTtl() {
        var clock=new TestClock();var store=new UnitExportStore(small(10,10000,2,20000),new ObjectMapper(),clock);
        String cursor;
        try(var r=store.reserve(new UnitExportRequest(null,null,1,null),"ctsv")){
            begin(r);r.append(item(1,"first"));r.append(item(2,"second"));cursor=r.publish().nextCursor();
        }
        var page=store.page(cursor,"ctsv");assertThat(page.items().getFirst().data().unitName()).isEqualTo("second");
        assertThat(store.page(cursor,"ctsv")).isEqualTo(page);
        assertThatThrownBy(()->store.page(cursor,"other")).isInstanceOfSatisfying(UnitApiException.class,e->assertThat(e.code()).isEqualTo("SERVICE_FORBIDDEN"));
        clock.now=clock.now.plus(Duration.ofMinutes(14));assertThat(store.page(cursor,"ctsv")).isEqualTo(page);
        clock.now=clock.now.plus(Duration.ofMinutes(1));
        assertThatThrownBy(()->store.page(cursor,"ctsv")).isInstanceOfSatisfying(UnitApiException.class,e->assertThat(e.code()).isEqualTo("EXPORT_EXPIRED"));
        assertThat(store.activeExports()).isZero();assertThat(store.retainedBytes()).isZero();
    }
    @Test void sa056_singlePageDoesNotRetainUnneededBatch() {
        var store=new UnitExportStore(small(10,10000,2,20000),new ObjectMapper(),new TestClock());
        try(var r=store.reserve(new UnitExportRequest(null,null,100,null),"ctsv")){begin(r);r.append(item(1,"a"));assertThat(r.publish().nextCursor()).isNull();}
        assertThat(store.activeExports()).isZero();assertThat(store.retainedBytes()).isZero();
    }
    String utf8(int bytes){return "ệ".repeat(bytes/3)+(bytes%3==2?"é":bytes%3==1?"x":"");}
    UnitChangeDto sizedItem(int id,int variableBytes){
        int name=Math.min(1536,variableBytes);variableBytes-=name;
        int department=Math.min(384,variableBytes);variableBytes-=department;
        int archive=Math.min(384,variableBytes);variableBytes-=archive;
        return new UnitChangeDto(Integer.toString(id),"upsert",null,new UnitDataDto("DV-"+id,utf8(name),utf8(department),
            "2026-11-11T00:00:00Z",true,utf8(archive),utf8(variableBytes)));
    }
    @Test void sa051_exactEightMiBEnvelopeAndOneByteOverflowWithValidUnicodeFields()throws Exception {
        var json=new ObjectMapper();var clock=new TestClock();var properties=UnitExportProperties.defaults(EPOCH);
        var store=new UnitExportStore(properties,json,clock);int count=3000;
        long overhead=json.writeValueAsBytes(new UnitExportResponse("full",EPOCH.toString(),EPOCH.toString(),null,"123",
            "2026-11-11T00:00:00Z",List.of(),EPOCH.toString())).length+count-1;
        for(int i=1;i<=count;i++)overhead+=json.writeValueAsBytes(sizedItem(i,0)).length;
        long remaining=8L*1024*1024-overhead;assertThat(remaining).isBetween(1L,count*2688L);
        var rows=new ArrayList<UnitChangeDto>();
        for(int i=1;i<=count;i++){int bytes=(int)Math.min(2688,remaining);rows.add(sizedItem(i,bytes));remaining-=bytes;}
        assertThat(remaining).isZero();assertThat(rows.getLast().data().unitName()).isEmpty();
        var request=new UnitExportRequest(null,null,500,null);
        try(var batch=store.reserve(request,"ctsv")){
            begin(batch);rows.forEach(batch::append);var first=batch.publish();
            new pbl6.daotao.units.validation.UnitContractValidator().validate(json.valueToTree(first));
        }
        assertThat(store.retainedBytes()).isEqualTo(8L*1024*1024);
        try(var batch=store.reserve(request,"ctsv")){
            begin(batch);rows.subList(0,count-1).forEach(batch::append);
            var last=rows.getLast();var d=last.data();var plusOne=new UnitChangeDto(last.sourceId(),last.operation(),null,
                new UnitDataDto(d.unitCode(),"x",d.departmentCode(),d.updatedAt(),true,d.archiveStatus(),d.unitClassification()));
            assertThatThrownBy(()->batch.append(plusOne)).isInstanceOfSatisfying(UnitApiException.class,e->assertThat(e.code()).isEqualTo("EXPORT_LIMIT_EXCEEDED"));
        }
        assertThat(store.activeExports()).isEqualTo(1);assertThat(store.retainedBytes()).isEqualTo(8L*1024*1024);
        clock.now=clock.now.plus(Duration.ofMinutes(15));store.prune();assertThat(store.retainedBytes()).isZero();
    }
}

