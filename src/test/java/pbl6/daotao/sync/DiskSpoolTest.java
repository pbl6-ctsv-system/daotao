package pbl6.daotao.sync;
import java.nio.file.*;import java.time.*;import java.util.*;
import org.junit.jupiter.api.*;import org.junit.jupiter.api.io.TempDir;
import com.fasterxml.jackson.databind.*;import com.fasterxml.jackson.databind.node.*;
import static org.assertj.core.api.Assertions.*;
class DiskSpoolTest {
 @TempDir Path temporary;static final ObjectMapper JSON=new ObjectMapper();
 static class Time extends Clock {volatile Instant now=Instant.parse("2026-11-12T00:00:00Z");public ZoneId getZone(){return ZoneOffset.UTC;}public Clock withZone(ZoneId zone){return this;}public Instant instant(){return now;}}
 static final UUID E=UUID.fromString("11111111-aaaa-4444-bbbb-111111111111");
 static CtResource resource(long bytes,long rows){return new CtResource("ethnicities","/api/dao-tao/dan-toc","DanToc","DanTocID","bigint",new CtResource.Profile(3,500,rows,bytes,30));}
 static JsonNode row(long id){var n=JSON.createObjectNode().put("source_id",Long.toString(id)).put("operation","upsert");n.putNull("change_version");n.set("data",JSON.createObjectNode().put("code","DT"+id).put("name","Tiếng Việt 😀"));return n;}
 static DiskExportStore.Producer producer(long... ids){return (meta,sink)->{meta.accept(new CtMetadata("full",E,UUID.randomUUID(),null,"55","2026-11-12T00:00:00Z"));for(long id:ids)sink.accept(row(id));};}
 @Test void indexedPagesReplayNumericallyAndExpireAtAbsoluteBoundary()throws Exception {
  var clock=new Time();var r=resource(8388608,10000);var req=new CtRequest(null,null,3,null);
  try(var store=new DiskExportStore(temporary.resolve("private"),clock)){
   var p1=store.create(r,"ctsv",req,producer(2,10,11,20,21,30,100));String cursor=p1.path("next_cursor").asText();
   assertThat(p1.path("items").size()).isEqualTo(3);assertThat(p1.path("items").get(1).path("source_id").asText()).isEqualTo("10");
   var p2=store.next(cursor,"ctsv","ethnicities",E);assertThat(store.next(cursor,"ctsv","ethnicities",E)).isEqualTo(p2);
   assertThat(store.next(p2.path("next_cursor").asText(),"ctsv","ethnicities",E).path("items").size()).isEqualTo(1);
   assertThatThrownBy(()->store.next(cursor,"other","ethnicities",E)).isInstanceOfSatisfying(CtApiException.class,x->assertThat(x.status).isEqualTo(400));
   clock.now=clock.now.plusSeconds(899);store.next(cursor,"ctsv","ethnicities",E);clock.now=clock.now.plusSeconds(1);
   assertThatThrownBy(()->store.next(cursor,"ctsv","ethnicities",E)).isInstanceOfSatisfying(CtApiException.class,x->assertThat(x.code).isEqualTo("EXPORT_EXPIRED"));
   assertThat(store.active()).isZero();assertThat(store.bytes()).isZero();
  }
 }
 @Test void exactWireBytesIncludingEveryEnvelopeAndCursorAreCharged()throws Exception {
  var clock=new Time();long exact;
  try(var s=new DiskExportStore(temporary.resolve("measure"),clock)){var a=s.create(resource(100000,10),"ctsv",new CtRequest(null,null,2,null),producer(2,10,11));var b=s.next(a.path("next_cursor").asText(),"ctsv","ethnicities",E);exact=JSON.writeValueAsBytes(a).length+JSON.writeValueAsBytes(b).length;assertThat(s.bytes()).isEqualTo(exact);}
  try(var s=new DiskExportStore(temporary.resolve("exact"),clock)){s.create(resource(exact,10),"ctsv",new CtRequest(null,null,2,null),producer(2,10,11));assertThat(s.bytes()).isEqualTo(exact);}
  try(var s=new DiskExportStore(temporary.resolve("one-less"),clock)){assertThatThrownBy(()->s.create(resource(exact-1,10),"ctsv",new CtRequest(null,null,2,null),producer(2,10,11))).isInstanceOfSatisfying(CtApiException.class,x->assertThat(x.code).isEqualTo("EXPORT_LIMIT_EXCEEDED"));assertThat(s.active()).isZero();assertThat(s.bytes()).isZero();}
 }
 @Test void rowLimitAndGlobalBudgetFailuresNeverPublishPartialBatch()throws Exception {
  var clock=new Time();try(var s=new DiskExportStore(temporary.resolve("rows"),clock)){assertThatThrownBy(()->s.create(resource(100000,1),"ctsv",new CtRequest(null,null,3,null),producer(2,10))).isInstanceOfSatisfying(CtApiException.class,x->assertThat(x.code).isEqualTo("EXPORT_LIMIT_EXCEEDED"));assertThat(s.active()).isZero();}
  try(var s=new DiskExportStore(temporary.resolve("budget"),clock,2,100,Duration.ofMinutes(15))){assertThatThrownBy(()->s.create(resource(100000,10),"ctsv",new CtRequest(null,null,3,null),producer(2,10))).isInstanceOfSatisfying(CtApiException.class,x->assertThat(x.code).isEqualTo("EXPORT_CAPACITY_EXCEEDED"));assertThat(s.bytes()).isZero();assertThat(s.active()).isZero();}
 }
 @Test void thirdExportIsRefusedWithoutEvictingLiveBatch()throws Exception {
  try(var s=new DiskExportStore(temporary.resolve("slots"),new Time())){
   var r=resource(100000,10);var q=new CtRequest(null,null,1,null);var first=s.create(r,"ctsv",q,producer(2,10));s.create(r,"ctsv",q,producer(2,10));
   assertThatThrownBy(()->s.create(r,"ctsv",q,producer(2,10))).isInstanceOfSatisfying(CtApiException.class,x->assertThat(x.code).isEqualTo("EXPORT_CAPACITY_EXCEEDED"));assertThat(s.active()).isEqualTo(2);s.next(first.path("next_cursor").asText(),"ctsv","ethnicities",E);
  }
 }
 @Test void failedReaderOrCommitLeavesNoManifestAndRestartExpiresOldCursor()throws Exception {
  Path root=temporary.resolve("restart");String token;
  try(var s=new DiskExportStore(root,new Time())){
   var r=resource(100000,10);var q=new CtRequest(null,null,1,null);
   assertThatThrownBy(()->s.create(r,"ctsv",q,(m,out)->{producer(2).read(m,out);throw new CtApiException(503,"SOURCE_UNAVAILABLE");})).isInstanceOf(CtApiException.class);assertThat(s.active()).isZero();assertThat(s.bytes()).isZero();token=s.create(r,"ctsv",q,producer(2,10)).path("next_cursor").asText();
  }
  try(var s=new DiskExportStore(root,new Time())){assertThatThrownBy(()->s.next(token,"ctsv","ethnicities",E)).isInstanceOfSatisfying(CtApiException.class,x->assertThat(x.status).isEqualTo(409));}
 }
 @Test void limitsAreResourceSpecificAndLegacyShapeIsRejected()throws Exception {
  var catalog=new CtCatalog();assertThat(CtRequest.parse(Map.of(),false,catalog.resource("students").profile()).limit()).isEqualTo(500);assertThat(CtRequest.parse(Map.of(),false,catalog.resource("academic-results").profile()).limit()).isEqualTo(1000);
  assertThatThrownBy(()->CtRequest.parse(Map.of("limit",new String[]{"501"}),false,catalog.resource("ethnicities").profile())).isInstanceOf(CtApiException.class);
  assertThatThrownBy(()->CtRequest.parse(Map.of("cursor",new String[]{"x"},"limit",new String[]{"1"}),false,catalog.resource("students").profile())).isInstanceOf(CtApiException.class);
  var old=JSON.createObjectNode().put("mode","full").put("source_epoch",E.toString()).put("export_id",UUID.randomUUID().toString()).put("as_of","2026-11-12T00:00:00Z");old.set("items",JSON.createArrayNode());old.putNull("next_cursor");assertThatThrownBy(()->new CtSchema("ethnicities").validate(old)).isInstanceOf(IllegalArgumentException.class);
 }

 @Test void secondStoreCannotOwnDirectoryOrLeaveAnOpenHandle()throws Exception {
  Path root=temporary.resolve("exclusive");
  try(var first=new DiskExportStore(root,new Time())){
   var opened=new java.util.concurrent.atomic.AtomicReference<java.nio.channels.FileChannel>();
   assertThatThrownBy(()->new DiskExportStore(root,new Time()){
    @Override protected java.nio.channels.FileChannel openLockChannel(Path path)throws java.io.IOException{var channel=super.openLockChannel(path);opened.set(channel);return channel;}
   }).isInstanceOf(RuntimeException.class);
   assertThat(opened.get().isOpen()).as("Failed constructor must close its own channel").isFalse();
   first.create(resource(100000,10),"ctsv",new CtRequest(null,null,1,null),producer(2,10));
  }
  Files.delete(root.resolve(".lock")); // Windows refuses this when the failed constructor leaked a channel.
 }

 @Test void injectedDiskFailureCleansPartialFilesAndIdleTtlIsCleanedWithoutRequests()throws Exception {
  Path failed=temporary.resolve("io-fault");
  try(var s=new DiskExportStore(failed,new Time()){@Override protected void afterRecordStored(long row)throws java.io.IOException{throw new java.nio.file.FileSystemException("synthetic-spool",null,"injected disk quota fault");}}){
   assertThatThrownBy(()->s.create(resource(100000,10),"ctsv",new CtRequest(null,null,1,null),producer(2,10))).isInstanceOfSatisfying(CtApiException.class,x->assertThat(x.code).isEqualTo("SOURCE_UNAVAILABLE"));assertThat(s.active()).isZero();assertThat(s.bytes()).isZero();
   try(var files=Files.list(failed)){assertThat(files.filter(p->p.getFileName().toString().startsWith("ct-")).count()).isZero();}
  }
  Path idle=temporary.resolve("idle");
  try(var s=new DiskExportStore(idle,Clock.systemUTC(),2,100000,Duration.ofMillis(200))){
   s.create(resource(100000,10),"ctsv",new CtRequest(null,null,1,null),producer(2,10));long deadline=System.nanoTime()+Duration.ofSeconds(4).toNanos();boolean cleaned=false;
   while(System.nanoTime()<deadline){try(var files=Files.list(idle)){cleaned=files.noneMatch(p->p.getFileName().toString().startsWith("ct-"));}if(cleaned)break;Thread.sleep(100);}
   assertThat(cleaned).as("janitor cleans idle TTL without HTTP/active()/bytes() polling").isTrue();assertThat(s.active()).isZero();
  }
 }
}
