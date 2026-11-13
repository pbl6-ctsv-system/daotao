package pbl6.daotao.sync;

import java.sql.*;import java.time.*;import java.util.*;import java.util.concurrent.*;import java.util.concurrent.atomic.*;import java.util.function.Consumer;
import javax.sql.DataSource;import com.fasterxml.jackson.databind.*;

/** Captures U before streaming inside one SQL Server SNAPSHOT transaction. */
public class CtSnapshotReader {
 private final DataSource source;private final UUID epoch;private final Clock clock;private final ObjectMapper json=new ObjectMapper();
 public CtSnapshotReader(DataSource source,UUID epoch,Clock clock){this.source=source;this.epoch=Objects.requireNonNull(epoch);this.clock=clock;}
 protected void afterUpperVersionCaptured(Connection connection,long upper)throws Exception {}
 protected void afterRowRead(long id)throws Exception {}
 public void read(CtBinding binding,CtRequest request,Consumer<CtMetadata> metadata,Consumer<JsonNode> sink){
  if(request.sourceEpoch()!=null&&!request.sourceEpoch().equals(epoch))throw new CtApiException(409,"SOURCE_EPOCH_CHANGED");
  var resource=binding.resource();long deadline=System.nanoTime()+Duration.ofSeconds(resource.profile().deadlineSeconds()).toNanos();
  AtomicReference<Statement> active=new AtomicReference<>();AtomicBoolean cancelled=new AtomicBoolean();
  var timer=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"ct26-read-deadline");t.setDaemon(true);return t;});
  var cancel=timer.schedule(()->{cancelled.set(true);var s=active.get();if(s!=null)try{s.cancel();}catch(SQLException ignored){}},resource.profile().deadlineSeconds(),TimeUnit.SECONDS);
  try(var c=source.getConnection()){
   try{CtBootstrap.verifyKey(c,resource);}catch(IllegalStateException e){throw new CtApiException(503,"SYNC_NOT_READY");}int original=c.getTransactionIsolation();
   try{
    c.setTransactionIsolation(4096);c.setAutoCommit(false);long upper,minimum;
    try(var q=c.prepareStatement("SELECT snapshot_isolation_state,CHANGE_TRACKING_CURRENT_VERSION(),CHANGE_TRACKING_MIN_VALID_VERSION(OBJECT_ID(?)),(SELECT COUNT(*) FROM sys.change_tracking_tables WHERE object_id=OBJECT_ID(?)) FROM sys.databases WHERE database_id=DB_ID()")){
     active.set(q);q.setString(1,"dbo."+resource.table());q.setString(2,"dbo."+resource.table());q.setQueryTimeout(resource.profile().deadlineSeconds());
     try(var rows=q.executeQuery()){
      if(!rows.next()||rows.getInt(1)!=1||rows.getObject(2)==null||rows.getObject(3)==null||rows.getInt(4)!=1)throw new CtApiException(503,"SYNC_NOT_READY");upper=rows.getLong(2);minimum=rows.getLong(3);
     }
    }
    if(request.sinceVersion()!=null){if(request.sinceVersion()>upper)throw new CtApiException(409,"SOURCE_LINEAGE_INVALID");if(request.sinceVersion()<minimum)throw new CtApiException(409,"FULL_SYNC_REQUIRED");}
    check(deadline,cancelled);metadata.accept(new CtMetadata(request.sinceVersion()==null?"full":"incremental",epoch,UUID.randomUUID(),request.sinceVersion()==null?null:request.sinceVersion().toString(),Long.toString(upper),clock.instant().toString()));
    afterUpperVersionCaptured(c,upper);
    String table="dbo.["+resource.table()+"]",pk="["+resource.primaryKey()+"]";
    String sql=request.sinceVersion()==null?"SELECT s.*,s."+pk+" AS ct_source_id,CAST(NULL AS bigint) AS ct_version,'I' AS ct_operation FROM "+table+" s ORDER BY s."+pk
      :"SELECT s.*,ct."+pk+" AS ct_source_id,ct.SYS_CHANGE_VERSION AS ct_version,ct.SYS_CHANGE_OPERATION AS ct_operation FROM CHANGETABLE(CHANGES "+table+",?) ct LEFT JOIN "+table+" s ON s."+pk+"=ct."+pk+" WHERE ct.SYS_CHANGE_VERSION<=? ORDER BY ct."+pk;
    try(var q=c.prepareStatement(sql)){
     active.set(q);q.setFetchSize(250);q.setQueryTimeout(Math.max(1,(int)TimeUnit.NANOSECONDS.toSeconds(Math.max(0,deadline-System.nanoTime()))));
     if(request.sinceVersion()!=null){q.setLong(1,request.sinceVersion());q.setLong(2,upper);}
     try(var rows=q.executeQuery()){while(rows.next()){
      check(deadline,cancelled);long id=rows.getLong("ct_source_id");var row=json.createObjectNode();row.put("source_id",Long.toString(id));boolean deleted="D".equals(rows.getString("ct_operation"));row.put("operation",deleted?"delete":"upsert");
      if(request.sinceVersion()==null)row.putNull("change_version");else row.put("change_version",Long.toString(rows.getLong("ct_version")));
      if(deleted)row.putNull("data");else row.set("data",binding.mapper().data(rows));afterRowRead(id);check(deadline,cancelled);sink.accept(row);
     }}
    }
    check(deadline,cancelled);c.commit();
   }catch(Exception e){
    if(!c.getAutoCommit())c.rollback();if(e instanceof CtApiException a)throw a;
    if(e instanceof IllegalArgumentException||e instanceof IllegalStateException||e instanceof SQLException x&&Set.of(207,208,229,3952,22105).contains(x.getErrorCode()))throw new CtApiException(503,"SYNC_NOT_READY");
    throw new CtApiException(503,"SOURCE_UNAVAILABLE");
   }finally{active.set(null);c.setAutoCommit(true);c.setTransactionIsolation(original);}
  }catch(CtApiException e){throw e;}catch(SQLException e){throw new CtApiException(503,"SOURCE_UNAVAILABLE");}
  finally{cancel.cancel(false);timer.shutdownNow();}
 }
 private static void check(long deadline,AtomicBoolean cancelled){if(cancelled.get()||System.nanoTime()>deadline)throw new CtApiException(503,"SOURCE_UNAVAILABLE");}
}
