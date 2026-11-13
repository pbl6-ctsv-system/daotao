package pbl6.daotao.sync;
import com.fasterxml.jackson.databind.*;import com.fasterxml.jackson.databind.node.*;import java.util.UUID;
public record CtMetadata(String mode,UUID epoch,UUID exportId,String fromVersion,String toVersion,String asOf) {
 public ObjectNode page(ArrayNode items,String cursor){var n=new ObjectMapper().createObjectNode();n.put("mode",mode);n.put("source_epoch",epoch.toString());n.put("export_id",exportId.toString());if(fromVersion==null)n.putNull("from_version");else n.put("from_version",fromVersion);n.put("to_version",toVersion);n.put("as_of",asOf);n.set("items",items);if(cursor==null)n.putNull("next_cursor");else n.put("next_cursor",cursor);return n;}
}
