package pbl6.daotao.units.dto;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record UnitChangeDto(String sourceId,String operation,String changeVersion,UnitDataDto data) {}

