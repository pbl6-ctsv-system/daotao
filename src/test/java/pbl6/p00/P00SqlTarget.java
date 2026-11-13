package pbl6.p00;

final class P00SqlTarget {
    static final String DATABASE = "sis_test_20261111";
    private static final String INSTANCE = "DEV2022";

    private P00SqlTarget() {
    }

    static String jdbcUrl(String portValue) {
        if (portValue == null || !portValue.matches("[0-9]{1,5}")) {
            throw new IllegalStateException("Set P00_SQL_PORT to the verified DEV2022 loopback TCP port");
        }
        int port = Integer.parseInt(portValue);
        if (port < 1 || port > 65535) {
            throw new IllegalStateException("P00_SQL_PORT is outside the valid TCP port range");
        }
        // Windows Authentication accepts localhost on DEV2022. The IPv4 literal was rejected.
        return "jdbc:sqlserver://localhost:" + port + ";databaseName=" + DATABASE;
    }

    static void checkConnectedTarget(String instance, String database) {
        if (!INSTANCE.equalsIgnoreCase(instance) || !DATABASE.equals(database)) {
            throw new IllegalStateException("SQL smoke refused an unexpected instance or database");
        }
    }
}
