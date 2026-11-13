package pbl6.daotao.sync;
import org.junit.jupiter.api.Test;
import pbl6.p01.FaultDatabase;
import pbl6.p01.FaultMigrationRunner;
import static org.assertj.core.api.Assertions.*;
class CtBootstrapIT {
 @Test void bootstrapMigrationValidatesExisting26PrimaryKeysAndPreservesUnits()throws Exception {
  FaultMigrationRunner.main(new String[0]);
  assertThat(FaultDatabase.number("SELECT COUNT(*) FROM dbo.flyway_schema_history WHERE success=1")).isEqualTo(4);
  assertThat(FaultDatabase.number("SELECT COUNT(*) FROM sys.change_tracking_tables WHERE object_id=OBJECT_ID('dbo.DonVi')")).isEqualTo(1);
 }
}
