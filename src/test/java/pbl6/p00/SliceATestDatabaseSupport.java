package pbl6.p00;
import java.sql.*;
import java.util.Properties;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

/** Test-only bridge to P00's existing target guard, no arbitrary JDBC targets. */
public final class SliceATestDatabaseSupport {
    private SliceATestDatabaseSupport() {}
    public static SimpleDriverDataSource dataSource() throws SQLException {
        if (!"test".equals(System.getProperty("spring.profiles.active")))
            throw new IllegalStateException("Explicit test profile required");
        Properties p=new Properties();
        p.setProperty("integratedSecurity","true"); p.setProperty("encrypt","true");
        p.setProperty("trustServerCertificate","true"); p.setProperty("loginTimeout","5");
        p.setProperty("socketTimeout","45000");
        var ds=new SimpleDriverDataSource(new com.microsoft.sqlserver.jdbc.SQLServerDriver(),
                P00SqlTarget.jdbcUrl(System.getenv("P00_SQL_PORT")),p);
        try(var c=ds.getConnection();var s=c.createStatement();
            var r=s.executeQuery("SELECT CONVERT(nvarchar(128),SERVERPROPERTY('InstanceName')), DB_NAME()")){
            if(!r.next()) throw new IllegalStateException("No target identity");
            P00SqlTarget.checkConnectedTarget(r.getString(1),r.getString(2));
        }
        return ds;
    }
    public static Connection open() throws SQLException {return dataSource().getConnection();}
    public static Flyway flyway() throws SQLException {
        return Flyway.configure().dataSource(dataSource()).locations("classpath:db/migration")
                .baselineOnMigrate(false).cleanDisabled(true).validateOnMigrate(true).load();
    }
    public static void migrate() throws SQLException {
        var f=flyway();
        // migrate's validateOnMigrate checks applied checksums and permits pending versions.
        // A standalone validate before migrate rejects those legitimate pending versions.
        f.migrate();
        f.validate();
    }
    public static long scalar(Connection c,String sql) throws SQLException {
        try(var s=c.createStatement();var r=s.executeQuery(sql)){
            if(!r.next()) throw new IllegalStateException("Expected result row");
            return r.getLong(1);
        }
    }
}

