package pbl6.daotao.sync;
import java.sql.*;
import java.time.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import pbl6.p01.FaultDatabase;
import com.fasterxml.jackson.databind.JsonNode;
import static org.assertj.core.api.Assertions.*;

/** Same production reader, isolated leaf fixture IDs; never clears referenced shared tables. */
class CtLeafReaderIT {
    @TempDir Path dir;
    static long insert(Connection c)throws Exception{
        try(var s=c.prepareStatement("INSERT dbo.LichSuDungPhong(MalopHP) VALUES(?);SELECT CONVERT(bigint,SCOPE_IDENTITY())")){
            s.setString(1,"P02-LATE-"+UUID.randomUUID());try(var r=s.executeQuery()){r.next();return r.getLong(1);}
        }
    }
    static Set<String> ids(DiskExportStore store,JsonNode page,UUID epoch){
        var ids=new HashSet<String>();while(true){page.path("items").forEach(i->ids.add(i.path("source_id").asText()));if(page.path("next_cursor").isNull())return ids;page=store.next(page.path("next_cursor").asText(),"ctsv","room-usage",epoch);}
    }
    @Test void lateCommitIsExcludedFromFullSnapshotAndIncludedInNextNativeCtDelta()throws Exception{
        var epoch=UUID.randomUUID();var binding=SisResourceMapper.binding("room-usage");var ready=new CountDownLatch(1);var proceed=new CountDownLatch(1);var upper=new AtomicLong();var pool=Executors.newSingleThreadExecutor();
        try(var writer=FaultDatabase.open();var spool=new DiskExportStore(dir,Clock.systemUTC())){
            writer.setAutoCommit(false);long id=insert(writer);var full=new CtRequest(null,null,2000,null);
            var reader=new CtSnapshotReader(FaultDatabase.dataSource(),epoch,Clock.systemUTC()){
                @Override protected void afterUpperVersionCaptured(Connection c,long u)throws Exception{upper.set(u);ready.countDown();if(!proceed.await(10,TimeUnit.SECONDS))throw new SQLException("Barrier expired");}
            };
            var pending=pool.submit(()->spool.create(binding.resource(),"ctsv",full,(m,sink)->reader.read(binding,full,m,sink)));
            assertThat(ready.await(10,TimeUnit.SECONDS)).isTrue();writer.commit();proceed.countDown();var page=pending.get(20,TimeUnit.SECONDS);
            assertThat(ids(spool,page,epoch)).doesNotContain(Long.toString(id));
            var delta=new CtRequest(upper.get(),epoch,2000,null);var normal=new CtSnapshotReader(FaultDatabase.dataSource(),epoch,Clock.systemUTC());
            var changed=spool.create(binding.resource(),"ctsv",delta,(m,sink)->normal.read(binding,delta,m,sink));
            assertThat(ids(spool,changed,epoch)).contains(Long.toString(id));assertThat(CtSchema.number(changed.path("to_version"))).isGreaterThan(upper.get());
        }finally{proceed.countDown();pool.shutdownNow();}
    }
    @Test void rolledBackSourceInsertNeverAppearsInFullOrDelta()throws Exception{
        var epoch=UUID.randomUUID();var binding=SisResourceMapper.binding("room-usage");long id;
        try(var c=FaultDatabase.open()){c.setAutoCommit(false);id=insert(c);c.rollback();}
        var request=new CtRequest(null,null,2000,null);var reader=new CtSnapshotReader(FaultDatabase.dataSource(),epoch,Clock.systemUTC());
        try(var spool=new DiskExportStore(dir,Clock.systemUTC())){
            var page=spool.create(binding.resource(),"ctsv",request,(m,sink)->reader.read(binding,request,m,sink));assertThat(ids(spool,page,epoch)).doesNotContain(Long.toString(id));
        }
    }
}
