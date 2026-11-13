package pbl6.daotao.units.dto;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record UnitDataDto(String unitCode,String unitName,String departmentCode,String updatedAt,
        boolean isActive,String archiveStatus,String unitClassification) {}

