package pbl6.p00;

import java.sql.DriverManager;
import java.util.Properties;
import org.junit.jupiter.api.RepeatedTest;

import static org.assertj.core.api.Assertions.assertThat;

class P00SqlSmokeIT {
    @RepeatedTest(2)
    void shouldReadAndRollBackSessionFixture_whenConnectedToApprovedDatabase() throws Exception {
        assertThat(System.getProperty("spring.profiles.active"))
                .as("SQL smoke requires the explicit test profile").isEqualTo("test");
        String url = P00SqlTarget.jdbcUrl(System.getenv("P00_SQL_PORT"));

        Properties options = new Properties();
        options.setProperty("integratedSecurity", "true");
        options.setProperty("encrypt", "true");
        // The approved loopback test instance uses a local certificate.
        options.setProperty("trustServerCertificate", "true");
        options.setProperty("loginTimeout", "5");
        options.setProperty("socketTimeout", "10000");

        try (var connection = DriverManager.getConnection(url, options);
             var statement = connection.createStatement()) {
            try (var target = statement.executeQuery(
                    "SELECT CONVERT(nvarchar(128), SERVERPROPERTY('InstanceName')), DB_NAME()")) {
                assertThat(target.next()).isTrue();
                P00SqlTarget.checkConnectedTarget(target.getString(1), target.getString(2));
            }

            connection.setAutoCommit(false);
            try {
                statement.executeUpdate("CREATE TABLE #p00_smoke (sample int NOT NULL)");
                statement.executeUpdate("INSERT INTO #p00_smoke (sample) VALUES (42)");
                try (var fixture = statement.executeQuery("SELECT sample FROM #p00_smoke")) {
                    assertThat(fixture.next()).isTrue();
                    assertThat(fixture.getInt(1)).isEqualTo(42);
                    assertThat(fixture.next()).isFalse();
                }
            } finally {
                connection.rollback();
            }
            connection.setAutoCommit(true);

            try (var cleanup = statement.executeQuery("SELECT OBJECT_ID('tempdb..#p00_smoke')")) {
                assertThat(cleanup.next()).isTrue();
                assertThat(cleanup.getObject(1)).isNull();
            }
        }
    }
}
