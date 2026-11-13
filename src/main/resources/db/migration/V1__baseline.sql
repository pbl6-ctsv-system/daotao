-- Derived without schema changes from nghiepvu/07-sql-server/sis_db.sql.
-- Flyway owns target selection, transaction and history. Do not edit after application.
SET NOCOUNT ON;
SET XACT_ABORT ON;
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET ARITHABORT ON;
SET NUMERIC_ROUNDABORT OFF;
CREATE TABLE [dbo].[HoSoSV] (
    [ID] bigint IDENTITY(1,1) NOT NULL,
    [MaSV] nvarchar(64) NOT NULL,
    [HoTen] nvarchar(512) NULL,
    [NgaySinh] date NULL,
    [GioiTinh] nvarchar(128) NULL,
    [NoiSinhID] bigint NULL,
    [DanTocID] bigint NULL,
    [QuocTichID] bigint NULL,
    [SoCCCD] nvarchar(64) NULL,
    [SoDienThoai] nvarchar(64) NULL,
    [Email] nvarchar(254) NULL,
    [LopID] bigint NULL,
    [KhoaQLID] bigint NULL,
    [KhoaHocID] bigint NULL,
    [NganhID] bigint NULL,
    [TrangThaiHocTap] nvarchar(128) NULL,
    [CapDT] nvarchar(128) NULL,
    [HocKyApDungID] bigint NULL,
    [HocKyBDID] bigint NULL,
    [HocKyKTID] bigint NULL,
    [MaHS] nvarchar(128) NULL,
    [NgayNhapHoc] nvarchar(512) NULL,
    [HeDT] nvarchar(128) NULL,
    [NgayLuu] nvarchar(512) NULL,
    [LoaiLuu] nvarchar(128) NULL,
    [HocKyHanHocID] bigint NULL,
    [CLC] nvarchar(128) NULL,
    CONSTRAINT [pk_HoSoSV] PRIMARY KEY CLUSTERED ([ID])
);

CREATE TABLE [dbo].[DanToc] (
    [DanTocID] bigint IDENTITY(1,1) NOT NULL,
    [MaDanToc] nvarchar(128) NOT NULL,
    [TenDanToc] nvarchar(512) NULL,
    CONSTRAINT [pk_DanToc] PRIMARY KEY CLUSTERED ([DanTocID])
);

CREATE TABLE [dbo].[QuocTich] (
    [QuocTichID] bigint IDENTITY(1,1) NOT NULL,
    [MaQuocTich] nvarchar(128) NOT NULL,
    [TenQuocTich] nvarchar(512) NULL,
    CONSTRAINT [pk_QuocTich] PRIMARY KEY CLUSTERED ([QuocTichID])
);

CREATE TABLE [dbo].[DiaPhuong] (
    [DiaPhuongID] bigint IDENTITY(1,1) NOT NULL,
    [MaDP] nvarchar(128) NOT NULL,
    [TenDP] nvarchar(512) NULL,
    CONSTRAINT [pk_DiaPhuong] PRIMARY KEY CLUSTERED ([DiaPhuongID])
);

CREATE TABLE [dbo].[DonVi] (
    [ID] bigint IDENTITY(1,1) NOT NULL,
    [MaDV] nvarchar(128) NOT NULL,
    [TenDV] nvarchar(512) NULL,
    [MaBP] nvarchar(128) NULL,
    [LuuTru] nvarchar(128) NULL,
    [IsUs] nvarchar(128) NULL,
    CONSTRAINT [pk_DonVi] PRIMARY KEY CLUSTERED ([ID])
);

CREATE TABLE [dbo].[KeHoach] (
    [ID] bigint IDENTITY(1,1) NOT NULL,
    [MaKH] nvarchar(128) NOT NULL,
    [TenKH] nvarchar(512) NULL,
    [NgayDay] nvarchar(512) NULL,
    [TuanDau] nvarchar(512) NULL,
    [SoTuan] decimal(28,10) NULL,
    [NgayDau] nvarchar(512) NULL,
    [MaNganh] nvarchar(128) NULL,
    [NamHoc] nvarchar(128) NULL,
    CONSTRAINT [pk_KeHoach] PRIMARY KEY CLUSTERED ([ID])
);

CREATE TABLE [dbo].[KhoaHoc] (
    [ID] bigint IDENTITY(1,1) NOT NULL,
    [MaKH] nvarchar(128) NOT NULL,
    [NamTuyenSinh] int NULL,
    CONSTRAINT [pk_KhoaHoc] PRIMARY KEY CLUSTERED ([ID])
);

CREATE TABLE [dbo].[NganhQG] (
    [ID] bigint IDENTITY(1,1) NOT NULL,
    [MaNganhQG] nvarchar(128) NOT NULL,
    [TenNganhQG] nvarchar(512) NULL,
    [CapDT] nvarchar(128) NULL,
    CONSTRAINT [pk_NganhQG] PRIMARY KEY CLUSTERED ([ID])
);

CREATE TABLE [dbo].[NganhTQ] (
    [ID] bigint IDENTITY(1,1) NOT NULL,
    [MaNganhTQ] nvarchar(128) NOT NULL,
    [TenNganhTQ] nvarchar(512) NULL,
    [NganhQGID] bigint NULL,
    [CapDT] nvarchar(128) NULL,
    [KhoiNganh] nvarchar(128) NULL,
    [Huy] nvarchar(128) NULL,
    [KyHieu] nvarchar(128) NULL,
    CONSTRAINT [pk_NganhTQ] PRIMARY KEY CLUSTERED ([ID])
);

CREATE TABLE [dbo].[Nganh] (
    [ID] bigint IDENTITY(1,1) NOT NULL,
    [MaNganh] nvarchar(128) NOT NULL,
    [TenNganh] nvarchar(512) NULL,
    [NganhTQID] bigint NULL,
    [NganhQGID] bigint NULL,
    [CapDTSo] nvarchar(128) NULL,
    [SoTinChi] decimal(28,10) NULL,
    [SoHK] int NULL,
    [NgonNguDT] nvarchar(128) NULL,
    [BatDau] nvarchar(512) NULL,
    [KetThuc] nvarchar(512) NULL,
    [KyBDauID] bigint NULL,
    [KyKThucID] bigint NULL,
    [KyHieu] nvarchar(128) NULL,
    [InUse] nvarchar(128) NULL,
    [CapDT] nvarchar(128) NULL,
    [TenNganhMoi] nvarchar(512) NULL,
    [LoaiCTDT] nvarchar(128) NULL,
    CONSTRAINT [pk_Nganh] PRIMARY KEY CLUSTERED ([ID])
);

CREATE TABLE [dbo].[Lop] (
    [ID] bigint IDENTITY(1,1) NOT NULL,
    [MaLop] nvarchar(128) NOT NULL,
    [TenLop] nvarchar(512) NULL,
    [NganhID] bigint NULL,
    [KhoaQLID] bigint NULL,
    [HinhThuc] nvarchar(128) NULL,
    [CapDT] nvarchar(128) NULL,
    [KhoaHocID] bigint NULL,
    [MaKhung] nvarchar(128) NULL,
    [LopGhep] nvarchar(128) NULL,
    CONSTRAINT [pk_Lop] PRIMARY KEY CLUSTERED ([ID])
);

CREATE TABLE [dbo].[CVHT_GV] (
    [ID] bigint IDENTITY(1,1) NOT NULL,
    [LopID] bigint NULL,
    [KeHoachID] bigint NULL,
    [HoSoCanBoID] bigint NULL,
    [NgayGan] datetimeoffset(7) NULL,
    [NguoiGan] nvarchar(512) NULL,
    [HoSoGVCNID] bigint NULL,
    CONSTRAINT [pk_CVHT_GV] PRIMARY KEY CLUSTERED ([ID])
);

CREATE TABLE [dbo].[HocVu] (
    [ID] bigint IDENTITY(1,1) NOT NULL,
    [HoSoSVID] bigint NULL,
    [KeHoachID] bigint NULL,
    [NganhID] bigint NULL,
    [NamHoc] nvarchar(128) NULL,
    [HK] nvarchar(128) NULL,
    [HPDangKy] decimal(28,10) NULL,
    [HPBatBuoc] decimal(28,10) NULL,
    [SoTCHocLai] decimal(28,10) NULL,
    [DiemTBHT10] decimal(28,10) NULL,
    [DiemTBHB] decimal(28,10) NULL,
    [XepLoaiCK] nvarchar(128) NULL,
    [HPTichLuy] decimal(28,10) NULL,
    [DiemTBTL] decimal(28,10) NULL,
    [DiemTBTLThang10] decimal(28,10) NULL,
    [XepLoai] nvarchar(128) NULL,
    [KLDK] decimal(28,10) NULL,
    [KLHL] decimal(28,10) NULL,
    [TCKhung] decimal(28,10) NULL,
    [TCTiendo] decimal(28,10) NULL,
    [TCTyle] decimal(28,10) NULL,
    [DiemRL] nvarchar(512) NULL,
    [ThoiHoc] nvarchar(128) NULL,
    [NgungHoc] nvarchar(128) NULL,
    [SoKyNgung] nvarchar(128) NULL,
    [IDCodeHoc] nvarchar(128) NULL,
    [DungHoc] nvarchar(128) NULL,
    CONSTRAINT [pk_HocVu] PRIMARY KEY CLUSTERED ([ID])
);

CREATE TABLE [dbo].[HoSoCanBo] (
    [ID] bigint IDENTITY(1,1) NOT NULL,
    [MaHS] nvarchar(128) NULL,
    [DonViID] bigint NULL,
    [HoTen] nvarchar(512) NULL,
    [MaChucVu] nvarchar(128) NULL,
    [Email] nvarchar(254) NULL,
    [HuuTri] nvarchar(128) NULL,
    [HetHL] nvarchar(128) NULL,
    [ThuocDV] nvarchar(128) NULL,
    [MaBP] nvarchar(128) NULL,
    CONSTRAINT [pk_HoSoCanBo] PRIMARY KEY CLUSTERED ([ID])
);

CREATE TABLE [dbo].[HocPhan] (
    [ID] bigint IDENTITY(1,1) NOT NULL,
    [MaHP] nvarchar(128) NULL,
    [TenHP] nvarchar(512) NULL,
    [SoTC] decimal(28,10) NULL,
    [DoAn] decimal(28,10) NULL,
    [ThucTap] nvarchar(128) NULL,
    [MaCongThuc] nvarchar(128) NULL,
    [LoaiHP] nvarchar(128) NULL,
    [CapDT] nvarchar(128) NULL,
    [HPao] nvarchar(128) NULL,
    [DATN] nvarchar(128) NULL,
    CONSTRAINT [pk_HocPhan] PRIMARY KEY CLUSTERED ([ID])
);

CREATE TABLE [dbo].[KhungCT] (
    [ID] bigint IDENTITY(1,1) NOT NULL,
    [MaKhung] nvarchar(128) NULL,
    [HocPhanID] bigint NOT NULL,
    [HocPhanDuocThayTheID] bigint NOT NULL,
    [HocKy] smallint NULL,
    [TuChon] nvarchar(128) NULL,
    [TQDA] nvarchar(128) NULL,
    [DATN] nvarchar(128) NULL,
    CONSTRAINT [pk_KhungCT] PRIMARY KEY CLUSTERED ([ID])
);

CREATE TABLE [dbo].[DiemKyHoc] (
    [ID] bigint IDENTITY(1,1) NOT NULL,
    [KeHoachID] bigint NOT NULL,
    [HoSoSVID] bigint NOT NULL,
    [HocPhanID] bigint NOT NULL,
    [HocPhanTuongDuongID] bigint NOT NULL,
    [Diem] decimal(28,10) NULL,
    [Note] nvarchar(2000) NULL,
    [HocLai] nvarchar(128) NULL,
    [IsChecked] nvarchar(128) NULL,
    [HocPhi] decimal(28,10) NULL,
    [NoHphi] nvarchar(128) NULL,
    [SoCT] nvarchar(128) NULL,
    [MaHS_MaHP] nvarchar(512) NULL,
    CONSTRAINT [pk_DiemKyHoc] PRIMARY KEY CLUSTERED ([ID])
);

CREATE TABLE [dbo].[DiemMien] (
    [ID] bigint IDENTITY(1,1) NOT NULL,
    [KeHoachID] bigint NOT NULL,
    [HocPhanID] bigint NOT NULL,
    [HoSoSVID] bigint NOT NULL,
    [Diem] decimal(28,10) NULL,
    [LoaiDiem] nvarchar(128) NULL,
    [NgayCN] date NULL,
    [GhiChu] nvarchar(2000) NULL,
    [HocPhanTuongDuongID] bigint NOT NULL,
    [HocLai] nvarchar(128) NULL,
    CONSTRAINT [pk_DiemMien] PRIMARY KEY CLUSTERED ([ID])
);

CREATE TABLE [dbo].[TieuThuc] (
    [ID] bigint IDENTITY(1,1) NOT NULL,
    [Ma] nvarchar(128) NULL,
    [Ten] nvarchar(512) NULL,
    [GiaTri] nvarchar(512) NULL,
    [Note] nvarchar(2000) NULL,
    CONSTRAINT [pk_TieuThuc] PRIMARY KEY CLUSTERED ([ID])
);

CREATE TABLE [dbo].[PhongHoc] (
    [ID] int IDENTITY(1,1) NOT NULL,
    [SoPhong] varchar(128) NOT NULL,
    [MaGD] varchar(128) NULL,
    [NoUse] smallint NULL,
    [Lock] smallint NULL,
    CONSTRAINT [pk_PhongHoc] PRIMARY KEY CLUSTERED ([ID])
);

CREATE TABLE [dbo].[LichSuDungPhong] (
    [ID] int IDENTITY(1,1) NOT NULL,
    [MalopHP] varchar(128) NULL,
    [Phong] varchar(128) NULL,
    [Ngay] date NULL,
    [TietDau] smallint NULL,
    [TietCuoi] smallint NULL,
    [MaLT] varchar(128) NULL,
    [MaNB] varchar(128) NULL,
    [KoShow] smallint NULL,
    [HocOnline] smallint NULL,
    [NgayLe] smallint NULL,
    [LichThi] smallint NULL,
    CONSTRAINT [pk_LichSuDungPhong] PRIMARY KEY CLUSTERED ([ID])
);

CREATE TABLE [dbo].[KhoaPhong] (
    [ID] int IDENTITY(1,1) NOT NULL,
    [PhieuKhoa] varchar(128) NULL,
    [TuNgay] date NULL,
    [DenNgay] date NULL,
    [KhoaT7] smallint NULL,
    [KhoaCN] smallint NULL,
    CONSTRAINT [pk_KhoaPhong] PRIMARY KEY CLUSTERED ([ID])
);

CREATE TABLE [dbo].[KhoaPhongChiTiet] (
    [ID] int IDENTITY(1,1) NOT NULL,
    [PhieuCT] varchar(128) NULL,
    [TuNgayCT] date NULL,
    [DenNgayCT] date NULL,
    [Phong] varchar(128) NULL,
    [Sang] smallint NULL,
    [Chieu] smallint NULL,
    [Toi] smallint NULL,
    CONSTRAINT [pk_KhoaPhongChiTiet] PRIMARY KEY CLUSTERED ([ID])
);

CREATE TABLE [dbo].[PhanLopRenLuyen] (
    [ID] bigint IDENTITY(1,1) NOT NULL,
    [KeHoachID] bigint NOT NULL,
    [HoSoSVID] bigint NOT NULL,
    [LopRenLuyenID] bigint NOT NULL,
    CONSTRAINT [pk_PhanLopRenLuyen] PRIMARY KEY CLUSTERED ([ID])
);

CREATE TABLE [dbo].[GhepLopSinhHoat] (
    [ID] bigint IDENTITY(1,1) NOT NULL,
    [KeHoachID] bigint NOT NULL,
    [LopGocID] bigint NOT NULL,
    [LopNhanID] bigint NOT NULL,
    CONSTRAINT [pk_GhepLopSinhHoat] PRIMARY KEY CLUSTERED ([ID])
);

CREATE TABLE [dbo].[TienDoSinhVien] (
    [ID] bigint IDENTITY(1,1) NOT NULL,
    [KeHoachID] bigint NOT NULL,
    [HoSoSVID] bigint NOT NULL,
    [NganhID] bigint NOT NULL,
    [HocKyHienTai] smallint NULL,
    CONSTRAINT [pk_TienDoSinhVien] PRIMARY KEY CLUSTERED ([ID])
);

CREATE UNIQUE NONCLUSTERED INDEX [uq_HoSoSV_MaSV] ON [dbo].[HoSoSV] ([MaSV]);
CREATE UNIQUE NONCLUSTERED INDEX [uq_DanToc_MaDanToc] ON [dbo].[DanToc] ([MaDanToc]);
CREATE UNIQUE NONCLUSTERED INDEX [uq_QuocTich_MaQuocTich] ON [dbo].[QuocTich] ([MaQuocTich]);
CREATE UNIQUE NONCLUSTERED INDEX [uq_DiaPhuong_MaDP] ON [dbo].[DiaPhuong] ([MaDP]);
CREATE UNIQUE NONCLUSTERED INDEX [uq_DonVi_MaDV] ON [dbo].[DonVi] ([MaDV]);
CREATE UNIQUE NONCLUSTERED INDEX [uq_KeHoach_MaKH] ON [dbo].[KeHoach] ([MaKH]);
CREATE UNIQUE NONCLUSTERED INDEX [uq_KhoaHoc_MaKH] ON [dbo].[KhoaHoc] ([MaKH]);
CREATE UNIQUE NONCLUSTERED INDEX [uq_NganhQG_MaNganhQG] ON [dbo].[NganhQG] ([MaNganhQG]);
CREATE UNIQUE NONCLUSTERED INDEX [uq_NganhTQ_MaNganhTQ] ON [dbo].[NganhTQ] ([MaNganhTQ]);
CREATE UNIQUE NONCLUSTERED INDEX [uq_Nganh_MaNganh] ON [dbo].[Nganh] ([MaNganh]);
CREATE UNIQUE NONCLUSTERED INDEX [uq_Lop_MaLop] ON [dbo].[Lop] ([MaLop]);
CREATE UNIQUE NONCLUSTERED INDEX [uq_PhongHoc_SoPhong] ON [dbo].[PhongHoc] ([SoPhong]);
CREATE UNIQUE NONCLUSTERED INDEX [uq_PhanLopRenLuyen_KeHoachID_HoSoSVID_LopRenLuyenID] ON [dbo].[PhanLopRenLuyen] ([KeHoachID], [HoSoSVID], [LopRenLuyenID]);
CREATE UNIQUE NONCLUSTERED INDEX [uq_GhepLopSinhHoat_KeHoachID_LopGocID_LopNhanID] ON [dbo].[GhepLopSinhHoat] ([KeHoachID], [LopGocID], [LopNhanID]);
CREATE UNIQUE NONCLUSTERED INDEX [uq_TienDoSinhVien_KeHoachID_HoSoSVID] ON [dbo].[TienDoSinhVien] ([KeHoachID], [HoSoSVID]);
ALTER TABLE [dbo].[HoSoSV] WITH CHECK ADD CONSTRAINT [fk_HoSoSV_NoiSinhID] FOREIGN KEY ([NoiSinhID]) REFERENCES [dbo].[DiaPhuong] ([DiaPhuongID]) ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE [dbo].[HoSoSV] WITH CHECK ADD CONSTRAINT [fk_HoSoSV_DanTocID] FOREIGN KEY ([DanTocID]) REFERENCES [dbo].[DanToc] ([DanTocID]) ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE [dbo].[HoSoSV] WITH CHECK ADD CONSTRAINT [fk_HoSoSV_QuocTichID] FOREIGN KEY ([QuocTichID]) REFERENCES [dbo].[QuocTich] ([QuocTichID]) ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE [dbo].[HoSoSV] WITH CHECK ADD CONSTRAINT [fk_HoSoSV_LopID] FOREIGN KEY ([LopID]) REFERENCES [dbo].[Lop] ([ID]) ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE [dbo].[HoSoSV] WITH CHECK ADD CONSTRAINT [fk_HoSoSV_KhoaQLID] FOREIGN KEY ([KhoaQLID]) REFERENCES [dbo].[DonVi] ([ID]) ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE [dbo].[HoSoSV] WITH CHECK ADD CONSTRAINT [fk_HoSoSV_KhoaHocID] FOREIGN KEY ([KhoaHocID]) REFERENCES [dbo].[KhoaHoc] ([ID]) ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE [dbo].[HoSoSV] WITH CHECK ADD CONSTRAINT [fk_HoSoSV_NganhID] FOREIGN KEY ([NganhID]) REFERENCES [dbo].[Nganh] ([ID]) ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE [dbo].[HoSoSV] WITH CHECK ADD CONSTRAINT [fk_HoSoSV_HocKyApDungID] FOREIGN KEY ([HocKyApDungID]) REFERENCES [dbo].[KeHoach] ([ID]) ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE [dbo].[HoSoSV] WITH CHECK ADD CONSTRAINT [fk_HoSoSV_HocKyBDID] FOREIGN KEY ([HocKyBDID]) REFERENCES [dbo].[KeHoach] ([ID]) ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE [dbo].[HoSoSV] WITH CHECK ADD CONSTRAINT [fk_HoSoSV_HocKyKTID] FOREIGN KEY ([HocKyKTID]) REFERENCES [dbo].[KeHoach] ([ID]) ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE [dbo].[HoSoSV] WITH CHECK ADD CONSTRAINT [fk_HoSoSV_HocKyHanHocID] FOREIGN KEY ([HocKyHanHocID]) REFERENCES [dbo].[KeHoach] ([ID]) ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE [dbo].[NganhTQ] WITH CHECK ADD CONSTRAINT [fk_NganhTQ_NganhQGID] FOREIGN KEY ([NganhQGID]) REFERENCES [dbo].[NganhQG] ([ID]) ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE [dbo].[Nganh] WITH CHECK ADD CONSTRAINT [fk_Nganh_NganhTQID] FOREIGN KEY ([NganhTQID]) REFERENCES [dbo].[NganhTQ] ([ID]) ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE [dbo].[Nganh] WITH CHECK ADD CONSTRAINT [fk_Nganh_NganhQGID] FOREIGN KEY ([NganhQGID]) REFERENCES [dbo].[NganhQG] ([ID]) ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE [dbo].[Nganh] WITH CHECK ADD CONSTRAINT [fk_Nganh_KyBDauID] FOREIGN KEY ([KyBDauID]) REFERENCES [dbo].[KeHoach] ([ID]) ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE [dbo].[Nganh] WITH CHECK ADD CONSTRAINT [fk_Nganh_KyKThucID] FOREIGN KEY ([KyKThucID]) REFERENCES [dbo].[KeHoach] ([ID]) ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE [dbo].[Lop] WITH CHECK ADD CONSTRAINT [fk_Lop_NganhID] FOREIGN KEY ([NganhID]) REFERENCES [dbo].[Nganh] ([ID]) ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE [dbo].[Lop] WITH CHECK ADD CONSTRAINT [fk_Lop_KhoaQLID] FOREIGN KEY ([KhoaQLID]) REFERENCES [dbo].[DonVi] ([ID]) ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE [dbo].[Lop] WITH CHECK ADD CONSTRAINT [fk_Lop_KhoaHocID] FOREIGN KEY ([KhoaHocID]) REFERENCES [dbo].[KhoaHoc] ([ID]) ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE [dbo].[CVHT_GV] WITH CHECK ADD CONSTRAINT [fk_CVHT_GV_LopID] FOREIGN KEY ([LopID]) REFERENCES [dbo].[Lop] ([ID]) ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE [dbo].[CVHT_GV] WITH CHECK ADD CONSTRAINT [fk_CVHT_GV_KeHoachID] FOREIGN KEY ([KeHoachID]) REFERENCES [dbo].[KeHoach] ([ID]) ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE [dbo].[CVHT_GV] WITH CHECK ADD CONSTRAINT [fk_CVHT_GV_HoSoCanBoID] FOREIGN KEY ([HoSoCanBoID]) REFERENCES [dbo].[HoSoCanBo] ([ID]) ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE [dbo].[CVHT_GV] WITH CHECK ADD CONSTRAINT [fk_CVHT_GV_HoSoGVCNID] FOREIGN KEY ([HoSoGVCNID]) REFERENCES [dbo].[HoSoCanBo] ([ID]) ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE [dbo].[HoSoCanBo] WITH CHECK ADD CONSTRAINT [fk_HoSoCanBo_DonViID] FOREIGN KEY ([DonViID]) REFERENCES [dbo].[DonVi] ([ID]) ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE [dbo].[HocVu] WITH CHECK ADD CONSTRAINT [fk_HocVu_HoSoSVID] FOREIGN KEY ([HoSoSVID]) REFERENCES [dbo].[HoSoSV] ([ID]) ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE [dbo].[HocVu] WITH CHECK ADD CONSTRAINT [fk_HocVu_KeHoachID] FOREIGN KEY ([KeHoachID]) REFERENCES [dbo].[KeHoach] ([ID]) ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE [dbo].[HocVu] WITH CHECK ADD CONSTRAINT [fk_HocVu_NganhID] FOREIGN KEY ([NganhID]) REFERENCES [dbo].[Nganh] ([ID]) ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE [dbo].[KhungCT] WITH CHECK ADD CONSTRAINT [fk_KhungCT_HocPhanID] FOREIGN KEY ([HocPhanID]) REFERENCES [dbo].[HocPhan] ([ID]) ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE [dbo].[KhungCT] WITH CHECK ADD CONSTRAINT [fk_KhungCT_HocPhanDuocThayTheID] FOREIGN KEY ([HocPhanDuocThayTheID]) REFERENCES [dbo].[HocPhan] ([ID]) ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE [dbo].[DiemKyHoc] WITH CHECK ADD CONSTRAINT [fk_DiemKyHoc_KeHoachID] FOREIGN KEY ([KeHoachID]) REFERENCES [dbo].[KeHoach] ([ID]) ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE [dbo].[DiemKyHoc] WITH CHECK ADD CONSTRAINT [fk_DiemKyHoc_HoSoSVID] FOREIGN KEY ([HoSoSVID]) REFERENCES [dbo].[HoSoSV] ([ID]) ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE [dbo].[DiemKyHoc] WITH CHECK ADD CONSTRAINT [fk_DiemKyHoc_HocPhanID] FOREIGN KEY ([HocPhanID]) REFERENCES [dbo].[HocPhan] ([ID]) ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE [dbo].[DiemKyHoc] WITH CHECK ADD CONSTRAINT [fk_DiemKyHoc_HocPhanTuongDuongID] FOREIGN KEY ([HocPhanTuongDuongID]) REFERENCES [dbo].[HocPhan] ([ID]) ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE [dbo].[DiemMien] WITH CHECK ADD CONSTRAINT [fk_DiemMien_KeHoachID] FOREIGN KEY ([KeHoachID]) REFERENCES [dbo].[KeHoach] ([ID]) ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE [dbo].[DiemMien] WITH CHECK ADD CONSTRAINT [fk_DiemMien_HocPhanID] FOREIGN KEY ([HocPhanID]) REFERENCES [dbo].[HocPhan] ([ID]) ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE [dbo].[DiemMien] WITH CHECK ADD CONSTRAINT [fk_DiemMien_HoSoSVID] FOREIGN KEY ([HoSoSVID]) REFERENCES [dbo].[HoSoSV] ([ID]) ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE [dbo].[DiemMien] WITH CHECK ADD CONSTRAINT [fk_DiemMien_HocPhanTuongDuongID] FOREIGN KEY ([HocPhanTuongDuongID]) REFERENCES [dbo].[HocPhan] ([ID]) ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE [dbo].[PhanLopRenLuyen] WITH CHECK ADD CONSTRAINT [fk_PhanLopRenLuyen_KeHoachID] FOREIGN KEY ([KeHoachID]) REFERENCES [dbo].[KeHoach] ([ID]) ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE [dbo].[PhanLopRenLuyen] WITH CHECK ADD CONSTRAINT [fk_PhanLopRenLuyen_HoSoSVID] FOREIGN KEY ([HoSoSVID]) REFERENCES [dbo].[HoSoSV] ([ID]) ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE [dbo].[PhanLopRenLuyen] WITH CHECK ADD CONSTRAINT [fk_PhanLopRenLuyen_LopRenLuyenID] FOREIGN KEY ([LopRenLuyenID]) REFERENCES [dbo].[Lop] ([ID]) ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE [dbo].[GhepLopSinhHoat] WITH CHECK ADD CONSTRAINT [fk_GhepLopSinhHoat_KeHoachID] FOREIGN KEY ([KeHoachID]) REFERENCES [dbo].[KeHoach] ([ID]) ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE [dbo].[GhepLopSinhHoat] WITH CHECK ADD CONSTRAINT [fk_GhepLopSinhHoat_LopGocID] FOREIGN KEY ([LopGocID]) REFERENCES [dbo].[Lop] ([ID]) ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE [dbo].[GhepLopSinhHoat] WITH CHECK ADD CONSTRAINT [fk_GhepLopSinhHoat_LopNhanID] FOREIGN KEY ([LopNhanID]) REFERENCES [dbo].[Lop] ([ID]) ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE [dbo].[TienDoSinhVien] WITH CHECK ADD CONSTRAINT [fk_TienDoSinhVien_KeHoachID] FOREIGN KEY ([KeHoachID]) REFERENCES [dbo].[KeHoach] ([ID]) ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE [dbo].[TienDoSinhVien] WITH CHECK ADD CONSTRAINT [fk_TienDoSinhVien_HoSoSVID] FOREIGN KEY ([HoSoSVID]) REFERENCES [dbo].[HoSoSV] ([ID]) ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE [dbo].[TienDoSinhVien] WITH CHECK ADD CONSTRAINT [fk_TienDoSinhVien_NganhID] FOREIGN KEY ([NganhID]) REFERENCES [dbo].[Nganh] ([ID]) ON DELETE NO ACTION ON UPDATE NO ACTION;
CREATE NONCLUSTERED INDEX [ix_HoSoSV_NoiSinhID] ON [dbo].[HoSoSV] ([NoiSinhID]);
CREATE NONCLUSTERED INDEX [ix_HoSoSV_DanTocID] ON [dbo].[HoSoSV] ([DanTocID]);
CREATE NONCLUSTERED INDEX [ix_HoSoSV_QuocTichID] ON [dbo].[HoSoSV] ([QuocTichID]);
CREATE NONCLUSTERED INDEX [ix_HoSoSV_LopID] ON [dbo].[HoSoSV] ([LopID]);
CREATE NONCLUSTERED INDEX [ix_HoSoSV_KhoaQLID] ON [dbo].[HoSoSV] ([KhoaQLID]);
CREATE NONCLUSTERED INDEX [ix_HoSoSV_KhoaHocID] ON [dbo].[HoSoSV] ([KhoaHocID]);
CREATE NONCLUSTERED INDEX [ix_HoSoSV_NganhID] ON [dbo].[HoSoSV] ([NganhID]);
CREATE NONCLUSTERED INDEX [ix_HoSoSV_HocKyApDungID] ON [dbo].[HoSoSV] ([HocKyApDungID]);
CREATE NONCLUSTERED INDEX [ix_HoSoSV_HocKyBDID] ON [dbo].[HoSoSV] ([HocKyBDID]);
CREATE NONCLUSTERED INDEX [ix_HoSoSV_HocKyKTID] ON [dbo].[HoSoSV] ([HocKyKTID]);
CREATE NONCLUSTERED INDEX [ix_HoSoSV_HocKyHanHocID] ON [dbo].[HoSoSV] ([HocKyHanHocID]);
CREATE NONCLUSTERED INDEX [ix_NganhTQ_NganhQGID] ON [dbo].[NganhTQ] ([NganhQGID]);
CREATE NONCLUSTERED INDEX [ix_Nganh_NganhTQID] ON [dbo].[Nganh] ([NganhTQID]);
CREATE NONCLUSTERED INDEX [ix_Nganh_NganhQGID] ON [dbo].[Nganh] ([NganhQGID]);
CREATE NONCLUSTERED INDEX [ix_Nganh_KyBDauID] ON [dbo].[Nganh] ([KyBDauID]);
CREATE NONCLUSTERED INDEX [ix_Nganh_KyKThucID] ON [dbo].[Nganh] ([KyKThucID]);
CREATE NONCLUSTERED INDEX [ix_Lop_NganhID] ON [dbo].[Lop] ([NganhID]);
CREATE NONCLUSTERED INDEX [ix_Lop_KhoaQLID] ON [dbo].[Lop] ([KhoaQLID]);
CREATE NONCLUSTERED INDEX [ix_Lop_KhoaHocID] ON [dbo].[Lop] ([KhoaHocID]);
CREATE NONCLUSTERED INDEX [ix_CVHT_GV_LopID] ON [dbo].[CVHT_GV] ([LopID]);
CREATE NONCLUSTERED INDEX [ix_CVHT_GV_KeHoachID] ON [dbo].[CVHT_GV] ([KeHoachID]);
CREATE NONCLUSTERED INDEX [ix_CVHT_GV_HoSoCanBoID] ON [dbo].[CVHT_GV] ([HoSoCanBoID]);
CREATE NONCLUSTERED INDEX [ix_CVHT_GV_HoSoGVCNID] ON [dbo].[CVHT_GV] ([HoSoGVCNID]);
CREATE NONCLUSTERED INDEX [ix_HoSoCanBo_DonViID] ON [dbo].[HoSoCanBo] ([DonViID]);
CREATE NONCLUSTERED INDEX [ix_HocVu_HoSoSVID] ON [dbo].[HocVu] ([HoSoSVID]);
CREATE NONCLUSTERED INDEX [ix_HocVu_KeHoachID] ON [dbo].[HocVu] ([KeHoachID]);
CREATE NONCLUSTERED INDEX [ix_HocVu_NganhID] ON [dbo].[HocVu] ([NganhID]);
CREATE NONCLUSTERED INDEX [ix_KhungCT_HocPhanID] ON [dbo].[KhungCT] ([HocPhanID]);
CREATE NONCLUSTERED INDEX [ix_KhungCT_HocPhanDuocThayTheID] ON [dbo].[KhungCT] ([HocPhanDuocThayTheID]);
CREATE NONCLUSTERED INDEX [ix_DiemKyHoc_KeHoachID] ON [dbo].[DiemKyHoc] ([KeHoachID]);
CREATE NONCLUSTERED INDEX [ix_DiemKyHoc_HoSoSVID] ON [dbo].[DiemKyHoc] ([HoSoSVID]);
CREATE NONCLUSTERED INDEX [ix_DiemKyHoc_HocPhanID] ON [dbo].[DiemKyHoc] ([HocPhanID]);
CREATE NONCLUSTERED INDEX [ix_DiemKyHoc_HocPhanTuongDuongID] ON [dbo].[DiemKyHoc] ([HocPhanTuongDuongID]);
CREATE NONCLUSTERED INDEX [ix_DiemMien_KeHoachID] ON [dbo].[DiemMien] ([KeHoachID]);
CREATE NONCLUSTERED INDEX [ix_DiemMien_HocPhanID] ON [dbo].[DiemMien] ([HocPhanID]);
CREATE NONCLUSTERED INDEX [ix_DiemMien_HoSoSVID] ON [dbo].[DiemMien] ([HoSoSVID]);
CREATE NONCLUSTERED INDEX [ix_DiemMien_HocPhanTuongDuongID] ON [dbo].[DiemMien] ([HocPhanTuongDuongID]);
CREATE NONCLUSTERED INDEX [ix_PhanLopRenLuyen_HoSoSVID] ON [dbo].[PhanLopRenLuyen] ([HoSoSVID]);
CREATE NONCLUSTERED INDEX [ix_PhanLopRenLuyen_LopRenLuyenID] ON [dbo].[PhanLopRenLuyen] ([LopRenLuyenID]);
CREATE NONCLUSTERED INDEX [ix_GhepLopSinhHoat_LopGocID] ON [dbo].[GhepLopSinhHoat] ([LopGocID]);
CREATE NONCLUSTERED INDEX [ix_GhepLopSinhHoat_LopNhanID] ON [dbo].[GhepLopSinhHoat] ([LopNhanID]);
CREATE NONCLUSTERED INDEX [ix_TienDoSinhVien_HoSoSVID] ON [dbo].[TienDoSinhVien] ([HoSoSVID]);
CREATE NONCLUSTERED INDEX [ix_TienDoSinhVien_NganhID] ON [dbo].[TienDoSinhVien] ([NganhID]);
EXEC (N'CREATE TRIGGER [dbo].[tr_Nganh_same_major] ON [dbo].[Nganh] AFTER INSERT, UPDATE AS
BEGIN
SET NOCOUNT ON;
IF EXISTS (SELECT 1 FROM [dbo].[Nganh] n WITH (UPDLOCK, HOLDLOCK) JOIN [dbo].[NganhTQ] b WITH (UPDLOCK, HOLDLOCK) ON b.ID=n.NganhTQID WHERE n.NganhQGID IS NOT NULL AND b.NganhQGID IS NOT NULL AND n.NganhQGID<>b.NganhQGID) THROW 51010, N''Inconsistent national-major references.'', 1;
END;');
EXEC (N'CREATE TRIGGER [dbo].[tr_NganhTQ_same_major] ON [dbo].[NganhTQ] AFTER INSERT, UPDATE AS
BEGIN
SET NOCOUNT ON;
IF EXISTS (SELECT 1 FROM [dbo].[Nganh] n WITH (UPDLOCK, HOLDLOCK) JOIN [dbo].[NganhTQ] b WITH (UPDLOCK, HOLDLOCK) ON b.ID=n.NganhTQID WHERE n.NganhQGID IS NOT NULL AND b.NganhQGID IS NOT NULL AND n.NganhQGID<>b.NganhQGID) THROW 51010, N''Inconsistent national-major references.'', 1;
END;');
