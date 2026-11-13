-- P01-05 fault target APPROVED by the user on 12/11/2026.
-- Create only when absent. Existing DB must pass marker/owner checks; never overwrite it here.
-- No source/test database is overwritten. Migration uses the existing reviewed Flyway files.
USE [master];
GO
IF CONVERT(nvarchar(128),SERVERPROPERTY('InstanceName'))<>N'DEV2022'
    THROW 51080,N'Unexpected instance for the isolated fault database.',1;
IF DB_ID(N'p01_sis_fault_20261111') IS NOT NULL
    THROW 51081,N'Fault database already exists; inspect it, do not overwrite.',1;
CREATE DATABASE [p01_sis_fault_20261111] COLLATE Vietnamese_100_CI_AS_SC;
ALTER DATABASE [p01_sis_fault_20261111] SET COMPATIBILITY_LEVEL=160;
GO
USE [p01_sis_fault_20261111];
GO
EXEC sys.sp_addextendedproperty @name=N'p01_slice_a_fixture_owner',@value=N'ctsv-mghp-20261111';
-- Later retention, backup, restore and reset operations must verify this marker and exact DB name.
-- Keep this database for inspection after the test. No DROP DATABASE is included here.
