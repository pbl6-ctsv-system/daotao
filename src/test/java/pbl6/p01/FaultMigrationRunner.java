package pbl6.p01;
/** One guarded setup action, no Flyway repair/clean/baseline or arbitrary database URL. */
public final class FaultMigrationRunner {
    public static void main(String[] args)throws Exception {
        var source=FaultDatabase.dataSource();
        try(var c=source.getConnection()){FaultDatabase.guard(c);}
        var flyway=org.flywaydb.core.Flyway.configure().dataSource(source).locations("classpath:db/migration")
            .baselineOnMigrate(false).cleanDisabled(true).validateOnMigrate(true).load();
        flyway.migrate();flyway.validate();
    }
}
