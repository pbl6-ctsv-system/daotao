ALTER TABLE dbo.DonVi ADD
    thoi_diem_cap_nhat datetimeoffset(7) NOT NULL
        CONSTRAINT df_DonVi_thoi_diem_cap_nhat DEFAULT (TODATETIMEOFFSET(SYSUTCDATETIME(), '+00:00')) WITH VALUES,
    dang_hoat_dong bit NOT NULL CONSTRAINT df_DonVi_dang_hoat_dong DEFAULT (1) WITH VALUES;
GO
CREATE TRIGGER dbo.tr_DonVi_source_timestamp ON dbo.DonVi AFTER INSERT, UPDATE AS
BEGIN
    SET NOCOUNT ON;
    IF TRIGGER_NESTLEVEL(OBJECT_ID(N'dbo.tr_DonVi_source_timestamp'), 'AFTER', 'DML') > 1 RETURN;
    UPDATE d SET thoi_diem_cap_nhat=TODATETIMEOFFSET(SYSUTCDATETIME(), '+00:00')
    FROM dbo.DonVi d INNER JOIN inserted i ON i.ID=d.ID;
END;
GO
ALTER TABLE dbo.DonVi ENABLE CHANGE_TRACKING WITH (TRACK_COLUMNS_UPDATED=OFF);
