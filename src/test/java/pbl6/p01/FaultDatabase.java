package pbl6.p01;

import java.sql.*;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

/** Explicit opt-in fault target. There is no fallback to either protected P00 database. */
public final class FaultDatabase {
    public static final String NAME="p01_sis_fault_20261111", INSTANCE="DEV2022", MARKER="ctsv-mghp-20261111";
    private FaultDatabase() {}
    private static void enabled(){
        if(!"test".equals(System.getProperty("spring.profiles.active"))||!Boolean.getBoolean("p01.fault.enabled"))
            throw new IllegalStateException("Fault suite requires explicit test profile and -Dp01.fault.enabled=true");
    }
    public static DataSource dataSource()throws SQLException {
        enabled();String port=System.getenv("P00_SQL_PORT");
        if(port==null||!port.matches("[1-9][0-9]{0,4}")||Integer.parseInt(port)>65535)
            throw new IllegalStateException("Verified local SQL TCP port is required");
        var p=new Properties();p.setProperty("integratedSecurity","true");p.setProperty("encrypt","true");
        p.setProperty("trustServerCertificate","true");p.setProperty("connectRetryCount","0");
        p.setProperty("loginTimeout","5");p.setProperty("socketTimeout","45000");
        var ds=new SimpleDriverDataSource(new com.microsoft.sqlserver.jdbc.SQLServerDriver(),
            "jdbc:sqlserver://localhost:"+port+";databaseName="+NAME+";",p);
        try(var c=ds.getConnection()){guard(c);}
        return ds;
    }
    public static Connection open()throws SQLException {var c=dataSource().getConnection();try{guard(c);return c;}catch(Exception e){c.close();throw e;}}
    public static void guard(Connection c)throws SQLException {
        enabled();
        try(var s=c.createStatement();var r=s.executeQuery("""
            SELECT DB_NAME(),CONVERT(nvarchar(128),SERVERPROPERTY('InstanceName')),
              (SELECT CONVERT(nvarchar(128),value) FROM sys.extended_properties WHERE class=0 AND name=N'p01_slice_a_fixture_owner'),
              SUSER_SNAME(owner_sid),SUSER_SNAME(),IS_MEMBER('db_owner')
            FROM sys.databases WHERE database_id=DB_ID()
            """)){
            if(!r.next()||!NAME.equals(r.getString(1))||!INSTANCE.equals(r.getString(2))
                    ||!MARKER.equals(r.getString(3))||!Objects.equals(r.getString(4),r.getString(5))||r.getInt(6)!=1)
                throw new IllegalStateException("Fault target/owner/marker mismatch; no mutation allowed");
        }
    }
    public static long number(String sql)throws SQLException {
        try(var c=open();var s=c.createStatement();var r=s.executeQuery(sql)){if(!r.next())throw new IllegalStateException("No scalar");return r.getLong(1);}
    }
    public static void execute(String sql)throws SQLException {
        try(var c=open()){guard(c);try(var s=c.createStatement()){s.execute(sql);}}
    }
}
