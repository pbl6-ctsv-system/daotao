package pbl6.daotao.units;
import java.sql.*;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import pbl6.daotao.units.config.UnitExportProperties;
import pbl6.daotao.units.dto.*;
import pbl6.daotao.units.repository.UnitChangeTrackingRepository;
import pbl6.daotao.units.repository.UnitChangeTrackingRepository.Metadata;
import pbl6.p00.SliceATestDatabaseSupport;
import static org.assertj.core.api.Assertions.*;

class UnitSnapshotIsolationIT {
    private final UnitExportProperties properties=UnitExportProperties.defaults(UUID.fromString("11111111-2222-3333-4444-555555555555"));
    private long ownId;
    private final List<Long> cleanup=new ArrayList<>();
    @BeforeAll static void migrate() throws Exception {SliceATestDatabaseSupport.migrate();}
    long insert(Connection c,String name) throws Exception {
        try(var s=c.prepareStatement("DECLARE @ids TABLE(id bigint); INSERT dbo.DonVi(MaDV,TenDV) OUTPUT inserted.ID INTO @ids VALUES(?,?); SELECT id FROM @ids;")){
            s.setString(1,"P01-SNAP-"+UUID.randomUUID());s.setString(2,name);
            try(var r=s.executeQuery()){r.next();long id=r.getLong(1);cleanup.add(id);return id;}
        }
    }
    @BeforeEach void seed() throws Exception {try(var c=SliceATestDatabaseSupport.open()){ownId=insert(c,"before");}}
    @AfterEach void removeOwnedFixtures() throws Exception {
        try(var c=SliceATestDatabaseSupport.open();var s=c.prepareStatement("DELETE dbo.DonVi WHERE ID=?")){
            for(long id:cleanup){s.setLong(1,id);s.addBatch();}s.executeBatch();
        }
    }
    UnitChangeTrackingRepository repo() throws Exception {return new UnitChangeTrackingRepository(SliceATestDatabaseSupport.dataSource(),properties,Clock.systemUTC());}
    @Test void sa006_lateCommitIsAbsentFromFullAndPresentInNextDelta() throws Exception {
        try(var writer=SliceATestDatabaseSupport.open()){
            writer.setAutoCommit(false);long pendingId=insert(writer,"late");
            var captured=new AtomicReference<Metadata>();var full=new ArrayList<UnitChangeDto>();
            var reader=new UnitChangeTrackingRepository(SliceATestDatabaseSupport.dataSource(),properties,Clock.systemUTC()){
                @Override protected void afterUpperVersionCaptured(Connection c,long u)throws Exception {writer.commit();}
            };
            reader.read(new UnitExportRequest(null,null,100,null),captured::set,full::add);
            assertThat(full).noneMatch(row->row.sourceId().equals(Long.toString(pendingId)));
            var delta=new ArrayList<UnitChangeDto>();
            repo().read(new UnitExportRequest(Long.parseLong(captured.get().toVersion()),properties.sourceEpoch(),100,null),m->{},delta::add);
            assertThat(delta).anyMatch(row->row.sourceId().equals(Long.toString(pendingId)));
            System.out.println("SA-006 barrier after-U committed writer; L="+captured.get().toVersion());
        }
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans={false,true})
    void sa008_updateCommittedAfterUpperCannotLeakIntoFullOrDeltaSnapshot(boolean incremental) throws Exception {
        Long lower=null;
        if(incremental)try(var c=SliceATestDatabaseSupport.open();var s=c.prepareStatement("UPDATE dbo.DonVi SET TenDV=N'before' WHERE ID=?")){
            lower=SliceATestDatabaseSupport.scalar(c,"SELECT CHANGE_TRACKING_CURRENT_VERSION()");s.setLong(1,ownId);s.executeUpdate();
        }
        var captured=new AtomicReference<Metadata>();var full=new ArrayList<UnitChangeDto>();
        var reader=new UnitChangeTrackingRepository(SliceATestDatabaseSupport.dataSource(),properties,Clock.systemUTC()){
            @Override protected void afterUpperVersionCaptured(Connection c,long u)throws Exception {
                try(var writer=SliceATestDatabaseSupport.open();var s=writer.prepareStatement("UPDATE dbo.DonVi SET TenDV=N'after' WHERE ID=?")){
                    s.setLong(1,ownId);s.executeUpdate();
                }
            }
        };
        reader.read(new UnitExportRequest(lower,incremental?properties.sourceEpoch():null,100,null),captured::set,full::add);
        assertThat(full.stream().filter(x->x.sourceId().equals(Long.toString(ownId))).findFirst().orElseThrow().data().unitName()).isEqualTo("before");
        var delta=new ArrayList<UnitChangeDto>();
        repo().read(new UnitExportRequest(Long.parseLong(captured.get().toVersion()),properties.sourceEpoch(),100,null),m->{},delta::add);
        assertThat(delta.stream().filter(x->x.sourceId().equals(Long.toString(ownId))).findFirst().orElseThrow().data().unitName()).isEqualTo("after");
    }
    @Test void sa007_rolledBackSourceMutationNeverAppears() throws Exception {
        try(var writer=SliceATestDatabaseSupport.open()){
            long lower=SliceATestDatabaseSupport.scalar(writer,"SELECT CHANGE_TRACKING_CURRENT_VERSION()");
            writer.setAutoCommit(false);
            try(var s=writer.prepareStatement("UPDATE dbo.DonVi SET TenDV=N'rolled-back' WHERE ID=?")){s.setLong(1,ownId);s.executeUpdate();}
            writer.rollback();
            var rows=new ArrayList<UnitChangeDto>();
            repo().read(new UnitExportRequest(lower,properties.sourceEpoch(),100,null),m->{},rows::add);
            assertThat(rows).noneMatch(x->x.sourceId().equals(Long.toString(ownId)));
        }
    }
    @Test void sa020_deleteRetainsCtPrimaryKeyAndNullData() throws Exception {
        try(var c=SliceATestDatabaseSupport.open();var s=c.createStatement()){
            long lower=SliceATestDatabaseSupport.scalar(c,"SELECT CHANGE_TRACKING_CURRENT_VERSION()");
            s.executeUpdate("DELETE dbo.DonVi WHERE ID="+ownId);
            var rows=new ArrayList<UnitChangeDto>();
            repo().read(new UnitExportRequest(lower,properties.sourceEpoch(),100,null),m->{},rows::add);
            var item=rows.stream().filter(x->x.sourceId().equals(Long.toString(ownId))).findFirst().orElseThrow();
            assertThat(item.operation()).isEqualTo("delete");assertThat(item.data()).isNull();
        }
    }
}

