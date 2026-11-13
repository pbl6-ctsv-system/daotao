package pbl6.daotao.sync;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.*;
class CtCatalogTest {
 @Test void approvedCatalogAndStrictSchemaAreAvailableWithoutEnablingResourceEndpoints()throws Exception {
  try(var input=getClass().getResourceAsStream("/contracts/ct26-resources.json")){
   assertThat(input).as("P02-28 approved26resource catalog").isNotNull();
   var rows=new ObjectMapper().readTree(input);assertThat(rows.size()).isEqualTo(26);
   assertThat(rows.findValuesAsText("resource")).doesNotHaveDuplicates().contains("units","ethnicities","student-progress");
   for(var row:rows){assertThat(row.path("primaryKey").asText()).isNotBlank();assertThat(row.path("url").asText()).startsWith("/api/dao-tao/");}
  }
  assertThat(getClass().getResource("/contracts/sis-ct26.schema.json")).isNotNull();
 }
}
