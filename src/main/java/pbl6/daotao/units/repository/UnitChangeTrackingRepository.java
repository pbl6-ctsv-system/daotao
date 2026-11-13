package pbl6.daotao.units.repository;

import java.sql.*;
import java.time.*;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.Consumer;
import javax.sql.DataSource;
import pbl6.daotao.units.config.UnitExportProperties;
import pbl6.daotao.units.dto.*;
import pbl6.daotao.units.service.UnitApiException;

/** One bounded read transaction, never held across HTTP pages. */
public class UnitChangeTrackingRepository {
    public record Metadata(String mode,String fromVersion,String toVersion,String asOf) {}
    private final DataSource source;
    private final UnitExportProperties properties;
    private final Clock clock;
    public UnitChangeTrackingRepository(DataSource source,UnitExportProperties properties,Clock clock) {
        this.source=source;this.properties=properties;this.clock=clock;
    }
    protected void afterUpperVersionCaptured(Connection connection,long upper) throws Exception {}
    protected void afterRowRead(long sourceId) throws Exception {}

    public void read(UnitExportRequest request,Consumer<Metadata> metadata,Consumer<UnitChangeDto> sink) {
        if(request.sourceEpoch()!=null&&!properties.sourceEpoch().equals(request.sourceEpoch()))
            throw new UnitApiException(409,"SOURCE_EPOCH_CHANGED",null);
        long deadline=System.nanoTime()+properties.readTimeout().toNanos();
        AtomicReference<Statement> current=new AtomicReference<>();
        AtomicBoolean cancelled=new AtomicBoolean();
        var timer=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"units-read-deadline");t.setDaemon(true);return t;});
        var cancellation=timer.schedule(()->{
            cancelled.set(true);var statement=current.get();
            if(statement!=null)try{statement.cancel();}catch(SQLException ignored){}
        },properties.readTimeout().toNanos(),TimeUnit.NANOSECONDS);
        try(var connection=source.getConnection()) {
            int originalIsolation=connection.getTransactionIsolation();
            try {
                connection.setTransactionIsolation(4096); // Microsoft JDBC TRANSACTION_SNAPSHOT.
                connection.setAutoCommit(false);
                long upper,min;
                try(var query=connection.createStatement()) {
                    current.set(query);query.setQueryTimeout(Math.max(1,(int)properties.readTimeout().toSeconds()));
                    try(var r=query.executeQuery("""
                        SELECT d.snapshot_isolation_state,
                          (SELECT COUNT(*) FROM sys.change_tracking_tables WHERE object_id=OBJECT_ID('dbo.DonVi')),
                          COL_LENGTH('dbo.DonVi','thoi_diem_cap_nhat'),
                          COL_LENGTH('dbo.DonVi','dang_hoat_dong'),
                          CHANGE_TRACKING_CURRENT_VERSION(),
                          CHANGE_TRACKING_MIN_VALID_VERSION(OBJECT_ID('dbo.DonVi'))
                        FROM sys.databases d WHERE d.database_id=DB_ID()
                        """)) {
                        if(!r.next()||r.getInt(1)!=1||r.getInt(2)!=1||r.getObject(3)==null||r.getObject(4)==null
                                ||r.getObject(5)==null||r.getObject(6)==null)
                            throw new UnitApiException(503,"SYNC_NOT_READY",null);
                        upper=r.getLong(5);min=r.getLong(6);
                    }
                }
                if(request.sinceVersion()!=null) {
                    if(request.sinceVersion()>upper)throw new UnitApiException(409,"SOURCE_LINEAGE_INVALID",null);
                    if(request.sinceVersion()<min)throw new UnitApiException(409,"FULL_SYNC_REQUIRED",null);
                }
                checkDeadline(deadline,cancelled);
                metadata.accept(new Metadata(request.sinceVersion()==null?"full":"incremental",
                        request.sinceVersion()==null?null:request.sinceVersion().toString(),
                        Long.toString(upper),clock.instant().toString()));
                afterUpperVersionCaptured(connection,upper);
                String columns="d.MaDV,d.TenDV,d.MaBP,d.thoi_diem_cap_nhat,d.dang_hoat_dong,d.LuuTru,d.IsUs";
                String sql=request.sinceVersion()==null
                        ?"SELECT d.ID,'I',CAST(NULL AS bigint),"+columns+" FROM dbo.DonVi d ORDER BY d.ID"
                        :"SELECT c.ID,c.SYS_CHANGE_OPERATION,c.SYS_CHANGE_VERSION,"+columns
                            +" FROM CHANGETABLE(CHANGES dbo.DonVi,?) c LEFT JOIN dbo.DonVi d ON d.ID=c.ID"
                            +" WHERE c.SYS_CHANGE_VERSION<=? ORDER BY c.ID";
                try(var query=connection.prepareStatement(sql)) {
                    current.set(query);query.setFetchSize(500);
                    query.setQueryTimeout(Math.max(1,(int)TimeUnit.NANOSECONDS.toSeconds(Math.max(0,deadline-System.nanoTime()))));
                    if(request.sinceVersion()!=null){query.setLong(1,request.sinceVersion());query.setLong(2,upper);}
                    checkDeadline(deadline,cancelled);
                    try(var rows=query.executeQuery()) {
                        while(rows.next()) {
                            checkDeadline(deadline,cancelled);
                            long id=rows.getLong(1);boolean deleted="D".equals(rows.getString(2));
                            String version=request.sinceVersion()==null?null:Long.toString(rows.getLong(3));
                            UnitDataDto data=deleted?null:new UnitDataDto(rows.getString(4),rows.getString(5),rows.getString(6),
                                    rows.getObject(7,OffsetDateTime.class).toInstant().toString(),rows.getBoolean(8),
                                    rows.getString(9),rows.getString(10));
                            afterRowRead(id);checkDeadline(deadline,cancelled);
                            sink.accept(new UnitChangeDto(Long.toString(id),deleted?"delete":"upsert",version,data));
                        }
                    }
                }
                checkDeadline(deadline,cancelled);connection.commit();
            } catch(Exception e) {
                if(!connection.getAutoCommit())connection.rollback();
                if(e instanceof UnitApiException api)throw api;
                if(e instanceof SQLException sql && Set.of(207,208,229,3952,22105).contains(sql.getErrorCode()))
                    throw new UnitApiException(503,"SYNC_NOT_READY",null);
                throw new UnitApiException(503,"SOURCE_UNAVAILABLE",null);
            } finally {
                current.set(null);connection.setAutoCommit(true);connection.setTransactionIsolation(originalIsolation);
            }
        } catch(SQLException e){throw new UnitApiException(503,"SOURCE_UNAVAILABLE",null);}
        finally {cancellation.cancel(false);timer.shutdownNow();}
    }
    private static void checkDeadline(long deadline,AtomicBoolean cancelled) {
        if(cancelled.get()||System.nanoTime()>deadline)throw new UnitApiException(503,"SOURCE_UNAVAILABLE",null);
    }
}

