package pbl6.daotao.sync;
import java.sql.*;import java.time.*;import java.nio.file.*;import java.util.*;import java.util.concurrent.*;import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.*;import org.junit.jupiter.api.io.TempDir;import com.fasterxml.jackson.databind.*;import pbl6.p01.*;
import static org.assertj.core.api.Assertions.*;
class CtReaderIT {
 @TempDir Path directory;static final ObjectMapper JSON=new ObjectMapper();static final UUID E=UUID.fromString("22222222-aaaa-4444-bbbb-222222222222");
 static CtBinding binding(){return new CtBinding(new CtCatalog().resource("ethnicities"),r->JSON.createObjectNode().put("code",r.getString("MaDanToc")).put("name",r.getString("TenDanToc")));}
 @BeforeAll static void setup()throws Exception{FaultMigrationRunner.main(new String[0]);try(var c=FaultDatabase.open()){CtBootstrap.enableTableTracking(c,binding().resource());}}
 @BeforeEach void reset()throws Exception{try(var c=FaultDatabase.open();var s=c.createStatement()){FaultDatabase.guard(c);s.execute("DELETE dbo.DanToc");}}
 static long insert(Connection c,String suffix)throws Exception{try(var s=c.prepareStatement("INSERT dbo.DanToc(MaDanToc,TenDanToc) VALUES(?,N'Dân tộc giả lập');SELECT CAST(SCOPE_IDENTITY() AS bigint)")){s.setString(1,"P02-"+UUID.randomUUID()+"-"+suffix);try(var r=s.executeQuery()){r.next();return r.getLong(1);}}}
 CtRequest full(int limit){return new CtRequest(null,null,limit,null);}
 @Test void snapshotUpperCoversLateCommitWithoutLosingOrLeakingTheRow()throws Exception {
  var upper=new CountDownLatch(1);var proceed=new CountDownLatch(1);var captured=new AtomicLong();var executor=Executors.newSingleThreadExecutor();
  try(var writer=FaultDatabase.open();var store=new DiskExportStore(directory.resolve("late"),Clock.systemUTC())){
   writer.setAutoCommit(false);long id=insert(writer,"LATE");
   var reader=new CtSnapshotReader(FaultDatabase.dataSource(),E,Clock.systemUTC()){@Override protected void afterUpperVersionCaptured(Connection c,long u)throws Exception{captured.set(u);upper.countDown();if(!proceed.await(10,TimeUnit.SECONDS))throw new IllegalStateException("Barrier expired");}};
   var result=executor.submit(()->store.create(binding().resource(),"ctsv",full(500),(m,s)->reader.read(binding(),full(500),m,s)));
   assertThat(upper.await(10,TimeUnit.SECONDS)).isTrue();writer.commit();proceed.countDown();JsonNode first=result.get(15,TimeUnit.SECONDS);assertThat(first.path("items").size()).isZero();
   var delta=new CtRequest(captured.get(),E,500,null);var normal=new CtSnapshotReader(FaultDatabase.dataSource(),E,Clock.systemUTC());
   JsonNode next=store.create(binding().resource(),"ctsv",delta,(m,s)->normal.read(binding(),delta,m,s));assertThat(next.path("items").size()).isEqualTo(1);assertThat(next.path("items").get(0).path("source_id").asText()).isEqualTo(Long.toString(id));
   assertThat(CtSchema.number(next.path("items").get(0).path("change_version"))).isGreaterThan(captured.get());
  }finally{proceed.countDown();executor.shutdownNow();}
 }
 @Test void materializedExportSurvivesSourceUpdateDeleteAndDeltaKeepsTombstone()throws Exception{
  long a,b;try(var c=FaultDatabase.open()){a=insert(c,"A");b=insert(c,"B");}
  var reader=new CtSnapshotReader(FaultDatabase.dataSource(),E,Clock.systemUTC());
  try(var store=new DiskExportStore(directory.resolve("stable"),Clock.systemUTC())){
   var page=store.create(binding().resource(),"ctsv",full(1),(m,s)->reader.read(binding(),full(1),m,s));long u=CtSchema.number(page.path("to_version"));
   try(var c=FaultDatabase.open();var s=c.createStatement()){s.execute("UPDATE dbo.DanToc SET TenDanToc=N'Changed' WHERE DanTocID="+a+";DELETE dbo.DanToc WHERE DanTocID="+b);}
   var second=store.next(page.path("next_cursor").asText(),"ctsv","ethnicities",E);assertThat(second.path("items").get(0).path("source_id").asText()).isEqualTo(Long.toString(b));assertThat(second.path("items").get(0).path("data").path("name").asText()).isEqualTo("Dân tộc giả lập");
   var request=new CtRequest(u,E,500,null);var delta=store.create(binding().resource(),"ctsv",request,(m,s)->reader.read(binding(),request,m,s));
   assertThat(delta.path("items").size()).isEqualTo(2);assertThat(delta.path("items").get(1).path("operation").asText()).isEqualTo("delete");assertThat(delta.path("items").get(1).path("data").isNull()).isTrue();
  }
 }
 @Test void sourceRollbackAndReadFailureNeverCreateFalseRowsOrManifest()throws Exception{
  try(var c=FaultDatabase.open()){c.setAutoCommit(false);insert(c,"ROLLBACK");c.rollback();}
  var normal=new CtSnapshotReader(FaultDatabase.dataSource(),E,Clock.systemUTC());
  try(var store=new DiskExportStore(directory.resolve("failure"),Clock.systemUTC())){
   var empty=store.create(binding().resource(),"ctsv",full(500),(m,s)->normal.read(binding(),full(500),m,s));assertThat(empty.path("items").isEmpty()).isTrue();assertThat(CtSchema.number(empty.path("to_version"))).isGreaterThanOrEqualTo(0);
   try(var c=FaultDatabase.open()){insert(c,"FAIL");}
   var broken=new CtSnapshotReader(FaultDatabase.dataSource(),E,Clock.systemUTC()){@Override protected void afterRowRead(long id)throws Exception{throw new SQLException("Injected reader failure");}};
   long before=store.bytes();assertThatThrownBy(()->store.create(binding().resource(),"ctsv",full(500),(m,s)->broken.read(binding(),full(500),m,s))).isInstanceOfSatisfying(CtApiException.class,e->assertThat(e.code).isEqualTo("SOURCE_UNAVAILABLE"));assertThat(store.active()).isEqualTo(1);assertThat(store.bytes()).isEqualTo(before);
  }
 }

 @Test void wrongKeyBindingIsSourceNotReadyInsteadOfAnEmptyExport()throws Exception {
  var normal=binding().resource();var wrong=new CtResource(normal.resource(),normal.url(),normal.table(),"ID",normal.sqlKeyType(),normal.profile());var b=new CtBinding(wrong,binding().mapper());
  try(var store=new DiskExportStore(directory.resolve("bad-key"),Clock.systemUTC())){
   var reader=new CtSnapshotReader(FaultDatabase.dataSource(),E,Clock.systemUTC());
   assertThatThrownBy(()->store.create(wrong,"ctsv",full(500),(m,out)->reader.read(b,full(500),m,out))).isInstanceOfSatisfying(CtApiException.class,e->assertThat(e.code).isEqualTo("SYNC_NOT_READY"));assertThat(store.active()).isZero();
  }
 }
 @Test void oneSecondTestDeadlineCancelsRealSqlReadAndRemovesPartialSpool()throws Exception {
  try(var c=FaultDatabase.open()){insert(c,"TIMEOUT");}
  var normal=binding().resource();var r=new CtResource(normal.resource(),normal.url(),normal.table(),normal.primaryKey(),normal.sqlKeyType(),new CtResource.Profile(100,500,10000,8388608,1));var b=new CtBinding(r,binding().mapper());
  var reader=new CtSnapshotReader(FaultDatabase.dataSource(),E,Clock.systemUTC()){@Override protected void afterRowRead(long id)throws Exception{Thread.sleep(1200);}};
  try(var store=new DiskExportStore(directory.resolve("deadline"),Clock.systemUTC())){
   long start=System.nanoTime();assertThatThrownBy(()->store.create(r,"ctsv",full(500),(m,out)->reader.read(b,full(500),m,out))).isInstanceOfSatisfying(CtApiException.class,e->assertThat(e.code).isEqualTo("SOURCE_UNAVAILABLE"));assertThat(Duration.ofNanos(System.nanoTime()-start).toMillis()).isBetween(1000L,10000L);assertThat(store.active()).isZero();assertThat(store.bytes()).isZero();
  }
 }
}
