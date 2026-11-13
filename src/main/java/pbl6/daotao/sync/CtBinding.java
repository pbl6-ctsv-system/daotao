package pbl6.daotao.sync;
import java.sql.*;import com.fasterxml.jackson.databind.JsonNode;
/** Only a resource slice supplies a mapper. Catalog entries alone never activate APIs. */
public record CtBinding(CtResource resource,Mapper mapper) {
 @FunctionalInterface public interface Mapper {JsonNode data(ResultSet row)throws SQLException;}
 public CtBinding {if(resource.resource().equals("units"))throw new IllegalArgumentException("Units keeps its approved engine/budget");}
}
