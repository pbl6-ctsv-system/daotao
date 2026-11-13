-- SQL Server database options run outside a Flyway transaction.
-- Do not force/disconnect other sessions if switching an option cannot complete.
DECLARE @sql nvarchar(max);
IF NOT EXISTS (SELECT 1 FROM sys.databases WHERE database_id=DB_ID() AND snapshot_isolation_state=1)
BEGIN
    SET @sql=N'ALTER DATABASE '+QUOTENAME(DB_NAME())+N' SET ALLOW_SNAPSHOT_ISOLATION ON';
    EXEC sys.sp_executesql @sql;
END;
IF NOT EXISTS (SELECT 1 FROM sys.change_tracking_databases WHERE database_id=DB_ID())
BEGIN
    SET @sql=N'ALTER DATABASE '+QUOTENAME(DB_NAME())+N' SET CHANGE_TRACKING = ON (CHANGE_RETENTION = 90 DAYS, AUTO_CLEANUP = ON)';
    EXEC sys.sp_executesql @sql;
END;
IF NOT EXISTS (SELECT 1 FROM sys.databases WHERE database_id=DB_ID() AND snapshot_isolation_state=1)
    THROW 51060, N'SNAPSHOT is not ready; inspect options before retry.', 1;
