package pbl6.daotao.sync;
import com.fasterxml.jackson.databind.*;import com.fasterxml.jackson.databind.node.ObjectNode;import com.networknt.schema.*;
import java.time.*;import java.util.*;
/** Pinned approved wire, plus SQL bigint/version/UTF-16 semantics. */
public final class CtSchema {
 private final JsonSchema schema;private final JsonNode properties;
 public CtSchema(String resource){try(var in=getClass().getResourceAsStream("/contracts/sis-ct26.schema.json")){
  var root=(ObjectNode)new ObjectMapper().readTree(in);String ref=resource+"_ct_response";
  if(!root.path("$defs").has(ref))throw new IllegalArgumentException("Unknown approved CT schema");root.put("$ref","#/$defs/"+ref);
  var cfg=new SchemaValidatorsConfig();cfg.setFormatAssertionsEnabled(true);schema=JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012).getSchema(root,cfg);
  properties=root.path("$defs").path(resource+"_ct_data").path("properties");
 }catch(java.io.IOException e){throw new IllegalStateException("Pinned CT schema unavailable",e);}}
 public void validate(JsonNode page){
  if(!schema.validate(page).isEmpty())throw new IllegalArgumentException("CT response schema mismatch");
  for(String key:List.of("source_epoch","export_id")){String v=page.path(key).asText();if(v.length()!=36||!UUID.fromString(v).toString().equalsIgnoreCase(v))throw new IllegalArgumentException("Invalid CT UUID");}
  if(!OffsetDateTime.parse(page.path("as_of").asText()).getOffset().equals(ZoneOffset.UTC))throw new IllegalArgumentException("UTC required");
  long u=number(page.path("to_version")),l=page.path("from_version").isNull()?0:number(page.path("from_version"));if(l>u)throw new IllegalArgumentException("Invalid CT window");
  long previous=0;for(var item:page.path("items")){
   long id=number(item.path("source_id"));if(id<=previous)throw new IllegalArgumentException("IDs must be unique/numerically ordered");previous=id;
   if(page.path("mode").asText().equals("incremental")){long v=number(item.path("change_version"));if(v<=l||v>u)throw new IllegalArgumentException("Change outside CT window");}
   item.path("data").fields().forEachRemaining(e->{int max=sqlTextLimit(properties.path(e.getKey()));if(e.getValue().isTextual()&&max>=0&&e.getValue().textValue().length()>max)throw new IllegalArgumentException("SQL UTF-16 length exceeded");});
  }
 }
 private static int sqlTextLimit(JsonNode property){
  if(property.path("maxLength").isInt())return property.path("maxLength").asInt();
  for(var branch:property.path("anyOf")){int value=sqlTextLimit(branch);if(value>=0)return value;}return -1;
 }
 public static long number(JsonNode value){if(!value.isTextual()||!value.textValue().matches("0|[1-9][0-9]{0,18}"))throw new IllegalArgumentException("Bigint string required");long n=Long.parseLong(value.textValue());if(n<0)throw new IllegalArgumentException("Negative bigint");return n;}
}
