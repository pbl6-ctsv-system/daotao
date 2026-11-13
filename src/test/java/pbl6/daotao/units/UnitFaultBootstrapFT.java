package pbl6.daotao.units;
import org.junit.jupiter.api.Test;
import pbl6.p01.FaultDatabase;
import static org.assertj.core.api.Assertions.assertThat;
/** Explicit -Dit.test suite, never discovered by default *IT smoke execution. */
class UnitFaultBootstrapFT {
    @org.junit.jupiter.api.BeforeAll static void migrate()throws Exception{pbl6.p01.FaultMigrationRunner.main(new String[0]);}
    @Test void faultDatabaseHasApprovedSisMigrationsAndOptions()throws Exception {
        assertThat(FaultDatabase.number("SELECT COUNT(*) FROM sys.tables WHERE object_id=OBJECT_ID('dbo.DonVi')")).isEqualTo(1);
        assertThat(FaultDatabase.number("SELECT COUNT(*) FROM dbo.flyway_schema_history WHERE success=1")).isEqualTo(3);
        assertThat(FaultDatabase.number("SELECT snapshot_isolation_state FROM sys.databases WHERE database_id=DB_ID()")).isEqualTo(1);
        assertThat(FaultDatabase.number("SELECT COUNT(*) FROM sys.change_tracking_tables WHERE object_id=OBJECT_ID('dbo.DonVi')")).isEqualTo(1);
    }
}
