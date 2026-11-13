package pbl6.p00;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.assertThat;
import static pbl6.p00.SliceATestDatabaseSupport.*;

class MigrationIT {
    @BeforeAll static void migrateApprovedTarget() throws Exception {
        var f=flyway();
        if(f.info().current()==null) {
            org.flywaydb.core.Flyway.configure().dataSource(dataSource()).locations("classpath:db/migration")
                    .target("1").baselineOnMigrate(false).cleanDisabled(true).load().migrate();
            System.out.println("P01-MIGRATION SIS applied V1 before upgrade fixture");
        }
        try(var c=open();var s=c.createStatement()){
            s.executeUpdate("IF NOT EXISTS(SELECT 1 FROM dbo.DonVi WHERE MaDV=N'P01-BASELINE-UPGRADE') INSERT dbo.DonVi(MaDV,TenDV,LuuTru,IsUs) VALUES(N'P01-BASELINE-UPGRADE',N'Fixture P01',N'raw',NULL)");
        }
        migrate();
    }
    @Test void sa001_preservesBaselineInventoryAndData() throws Exception {
        try(var c=open()){
            assertThat(scalar(c,"SELECT COUNT(*) FROM sys.tables WHERE is_ms_shipped=0 AND name<>'flyway_schema_history'")).isEqualTo(26);
            assertThat(scalar(c,"SELECT COUNT(*) FROM sys.foreign_keys")).isEqualTo(46);
            assertThat(scalar(c,"SELECT max_length FROM sys.columns WHERE object_id=OBJECT_ID('dbo.DonVi') AND name='LuuTru'")).isEqualTo(256);
            assertThat(scalar(c,"SELECT COUNT(*) FROM dbo.DonVi WHERE MaDV=N'P01-BASELINE-UPGRADE' AND LuuTru=N'raw' AND IsUs IS NULL")).isEqualTo(1);
        }
    }
    @Test void sa001_hasSourceMetadataAndSnapshot() throws Exception {
        try(var c=open()){
            assertThat(scalar(c,"SELECT COUNT(*) FROM sys.columns WHERE object_id=OBJECT_ID('dbo.DonVi') AND name='thoi_diem_cap_nhat' AND is_nullable=0")).as("source timestamp metadata").isEqualTo(1);
            assertThat(scalar(c,"SELECT COUNT(*) FROM sys.columns WHERE object_id=OBJECT_ID('dbo.DonVi') AND name='dang_hoat_dong' AND is_nullable=0")).isEqualTo(1);
            assertThat(scalar(c,"SELECT snapshot_isolation_state FROM sys.databases WHERE database_id=DB_ID()")).isEqualTo(1);
            assertThat(scalar(c,"SELECT is_read_committed_snapshot_on FROM sys.databases WHERE database_id=DB_ID()")).isZero();
        }
    }
    @Test void sa001_hasRealCtWithNinetyDayRetention() throws Exception {
        try(var c=open()){
            assertThat(scalar(c,"SELECT COUNT(*) FROM sys.change_tracking_databases WHERE database_id=DB_ID() AND retention_period=90 AND retention_period_units_desc='DAYS' AND is_auto_cleanup_on=1")).as("CT configuration").isEqualTo(1);
            assertThat(scalar(c,"SELECT COUNT(*) FROM sys.change_tracking_tables WHERE object_id=OBJECT_ID('dbo.DonVi')")).isEqualTo(1);
        }
    }
    @Test void sae02_writerHandlesMultirowOldTimestampRawUpdateAndRollback() throws Exception {
        try(var c=open()){
            assertThat(scalar(c,"SELECT COUNT(*) FROM sys.triggers WHERE name='tr_DonVi_source_timestamp'")).as("metadata writer").isEqualTo(1);
            c.setAutoCommit(false);
            try(var s=c.createStatement()){
                s.executeUpdate("INSERT dbo.DonVi(MaDV,thoi_diem_cap_nhat) VALUES(N'P01-WRITER-A','2000-01-01T00:00:00+00:00'),(N'P01-WRITER-B','2000-01-01T00:00:00+00:00')");
                assertThat(scalar(c,"SELECT COUNT(*) FROM dbo.DonVi WHERE MaDV IN(N'P01-WRITER-A',N'P01-WRITER-B') AND thoi_diem_cap_nhat>'2026-01-01' AND DATEPART(TZOFFSET,thoi_diem_cap_nhat)=0")).isEqualTo(2);
                OffsetDateTime before;
                try(var r=s.executeQuery("SELECT thoi_diem_cap_nhat FROM dbo.DonVi WHERE MaDV=N'P01-WRITER-A'")){r.next();before=r.getObject(1,OffsetDateTime.class);}
                s.executeUpdate("UPDATE dbo.DonVi SET LuuTru=N'archived',IsUs=N'class-A',thoi_diem_cap_nhat='2000-01-01T00:00:00+00:00' WHERE MaDV IN(N'P01-WRITER-A',N'P01-WRITER-B')");
                try(var r=s.executeQuery("SELECT thoi_diem_cap_nhat FROM dbo.DonVi WHERE MaDV=N'P01-WRITER-A'")){r.next();assertThat(r.getObject(1,OffsetDateTime.class)).isAfterOrEqualTo(before);}
            } finally {c.rollback();c.setAutoCommit(true);}
            assertThat(scalar(c,"SELECT COUNT(*) FROM dbo.DonVi WHERE MaDV IN(N'P01-WRITER-A',N'P01-WRITER-B')")).isZero();
        }
    }
    @Test void migrationIsIdempotentAndChecksumsValidate() throws Exception {
        var f=flyway();f.validate();assertThat(f.migrate().migrationsExecuted).isZero();
        assertThat(f.info().current().getVersion().getVersion()).isEqualTo("3");
    }
}

