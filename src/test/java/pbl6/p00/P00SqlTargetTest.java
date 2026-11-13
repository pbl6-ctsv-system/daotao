package pbl6.p00;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class P00SqlTargetTest {
    @Test
    void shouldUseOnlyTheApprovedLoopbackTarget_whenPortIsProvided() {
        assertThat(P00SqlTarget.jdbcUrl("51433"))
                .isEqualTo("jdbc:sqlserver://localhost:51433;databaseName=sis_test_20261111");
    }

    @Test
    void shouldRefuseMissingOrInvalidPort_whenBuildingTheTarget() {
        assertThatIllegalStateException().isThrownBy(() -> P00SqlTarget.jdbcUrl(null));
        assertThatIllegalStateException().isThrownBy(() -> P00SqlTarget.jdbcUrl("65536"));
        assertThatIllegalStateException().isThrownBy(() -> P00SqlTarget.jdbcUrl("1433;databaseName=master"));
    }

    @Test
    void shouldRefuseOtherDatabasesOrInstances_whenConnected() {
        P00SqlTarget.checkConnectedTarget("DEV2022", P00SqlTarget.DATABASE);
        assertThatIllegalStateException()
                .isThrownBy(() -> P00SqlTarget.checkConnectedTarget("DEV2022", "master"));
        assertThatIllegalStateException()
                .isThrownBy(() -> P00SqlTarget.checkConnectedTarget("SQLEXPRESS", P00SqlTarget.DATABASE));
    }
}
