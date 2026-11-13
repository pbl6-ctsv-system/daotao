package pbl6.daotao.units;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import pbl6.daotao.units.config.UnitExportProperties;
import static org.assertj.core.api.Assertions.*;

class UnitExportPropertiesTest {
    @Test void sourceEpochIsRequiredAndNeverGeneratedOnRestart() {
        assertThatThrownBy(()->UnitExportProperties.defaults(null)).isInstanceOf(IllegalArgumentException.class);
        UUID epoch=UUID.fromString("11111111-2222-3333-4444-555555555555");
        assertThat(UnitExportProperties.defaults(epoch).sourceEpoch()).isEqualTo(epoch);
        assertThat(UnitExportProperties.defaults(epoch)).isEqualTo(UnitExportProperties.defaults(epoch));
    }
    @Test void budgetCannotBeRaisedBeyondApprovedProfile() {
        assertThatThrownBy(()->new UnitExportProperties(UUID.randomUUID(),10001,8388608,2,16777216,
                Duration.ofSeconds(30),Duration.ofMinutes(15))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->new UnitExportProperties(UUID.randomUUID(),10000,8388609,2,16777216,
                Duration.ofSeconds(30),Duration.ofMinutes(15))).isInstanceOf(IllegalArgumentException.class);
    }
}

