package pbl6.daotao.units.dto;
import java.util.List;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record UnitExportResponse(String mode,String sourceEpoch,String exportId,String fromVersion,
        String toVersion,String asOf,List<UnitChangeDto> items,String nextCursor) {
    public UnitExportResponse {items=List.copyOf(items);}
}

