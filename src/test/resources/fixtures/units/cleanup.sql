-- Optional manual cleanup: only the two explicitly named synthetic units.
-- First stop the manual consumer. Keep CT history/options and all other rows.
IF DB_NAME()<>N'sis_test_20261111' OR CONVERT(nvarchar(128),SERVERPROPERTY('InstanceName'))<>N'DEV2022'
    THROW 51090,N'Unexpected P01 manual fixture target.',1;
SET XACT_ABORT ON;
BEGIN TRAN;
DELETE dbo.DonVi WHERE MaDV IN(N'P01-MANUAL-A',N'P01-MANUAL-B');
COMMIT;
