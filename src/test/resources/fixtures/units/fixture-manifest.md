# Fixtures Slice A — SIS

Nguồn: nghiepvu/08-kiem-thu/daotao/slice-a-test-cases.md (F0–F9,66SA). **SYNTHETIC**, không CSV/PII.

| Fixture | Test / phạm vi | Setup và cleanup |
|---|---|---|
| F1/F3 | MigrationIT, UnitChangeTrackingIT, UnitSnapshotIsolationIT | INSERT/UPDATE/DELETE trên SQL Server thật; mã P01-CT-/P01-SNAP-UUID. Cleanup chỉ ID/mã do ca tạo. |
| F4/F5 | UnitExportBudgetIT, UnitSnapshotIsolationIT | Phân trang, snapshot và hai connection có barrier. Không khóa ghi SIS chủ động. |
| F6 | UnitContractValidatorTest, UnitExportWebTest, UnitExportHttpIT | JSON lỗi/MockMvc hoặc HTTP source-unready; không là CT thật. |
| F9 | UnitExportBudgetIT, UnitReadDeadlineIT | Ngưỡng mặc định, Clock test cho TTL, deadline thật30s, Xmx512MiB. Mã P01-BUDGET-/P01-DEADLINE-UUID. |
| F7/F8 | UnitFaultBootstrapFT + CTSV UnitFaultRecoveryFT | Được phép12/11: tạo/migrate `p01_sis_fault_20261111`, retention cleanup/backup/restore/reset thật có guard; không reset/restore/ghi hai DB gốc. |
| Manual | setup.sql / cleanup.sql | Tùy chọn, guard exact test DB+DEV2022; chỉ hai mã P01-MANUAL-A/B. Chưa dùng làm evidence tự động. |

MigrationIT giữ fixture P01-BASELINE-UPGRADE để kiểm dữ liệu nâng V1. Không xóa nó khi chạy quota.
Test dùng CT version engine, không timestamp hay version giả. Các Resource/Deadline JSON tại target ghi số đo, không suy RAM từ JSON bytes.
Manifest66SA và trạng thái từng biến thể giữ trong nghiepvu/09-trien-khai/slice-a-execution-report.md.

FaultDatabase/FaultMigrationRunner chỉ nhận exact fault DB/DEV2022/owner/marker và opt-in `-Dp01.fault.enabled=true`, profile test; dùng V1/V2/V3 đã duyệt, không sửa checksum hay repair. Build SIS JAR bằng `-Dtest=UnitExportPropertiesTest -Dit.test=UnitFaultBootstrapFT`, sau đó chạy FT CTSV theo nghiepvu/09-trien-khai/slice-a-g8-runbook.md. Không chạy các IT cũ trong lượt chỉ cho ghi fault DB.

Suite fault dùng SQL thật và HTTP tới SIS JAR thật; sáu bảng projection CTSV là fixture cùng một DB vật lý. Chỉ G8 smoke fault khởi động thêm CTSV JAR thật; các ca recovery gọi production consumer service trong JUnit. Không gán nhãn hai DB vật lý cho môi trường này; evidence baseline hai DB được giữ riêng.
