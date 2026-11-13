-- Optional manual fixture, never run automatically. Synthetic data only.
IF DB_NAME()<>N'sis_test_20261111' OR CONVERT(nvarchar(128),SERVERPROPERTY('InstanceName'))<>N'DEV2022'
    THROW 51090,N'Unexpected P01 manual fixture target.',1;
SET XACT_ABORT ON;
BEGIN TRAN;
IF NOT EXISTS(SELECT 1 FROM dbo.DonVi WHERE MaDV=N'P01-MANUAL-A')
    INSERT dbo.DonVi(MaDV,TenDV,LuuTru,IsUs) VALUES(N'P01-MANUAL-A',N'Đơn vị thử A',NULL,N'raw');
IF NOT EXISTS(SELECT 1 FROM dbo.DonVi WHERE MaDV=N'P01-MANUAL-B')
    INSERT dbo.DonVi(MaDV,TenDV,LuuTru,IsUs) VALUES(N'P01-MANUAL-B',N'Đơn vị thử B',N'raw',NULL);
COMMIT;
SELECT ID,MaDV,TenDV,LuuTru,IsUs FROM dbo.DonVi WHERE MaDV IN(N'P01-MANUAL-A',N'P01-MANUAL-B');
