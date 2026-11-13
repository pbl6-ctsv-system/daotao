package pbl6.daotao.sync;

import java.sql.*;
import java.time.ZoneOffset;
import java.util.*;
import com.fasterxml.jackson.databind.*;

/** Whitelisted SQL columns and representations copied from the current SIS contracts/DDL. */
public final class SisResourceMapper {
    private static final ObjectMapper JSON=new ObjectMapper();
    private static final Map<String,JsonNode> SPECS=load();
    private SisResourceMapper(){}

    private static Map<String,JsonNode> load(){
        try(var in=SisResourceMapper.class.getResourceAsStream("/contracts/foundation-resources.json")){
            var result=new LinkedHashMap<String,JsonNode>();
            for(var row:JSON.readTree(in)) {
                if(result.put(row.path("resource").asText(),row)!=null)throw new IllegalStateException("Duplicate resource");
            }
            return Collections.unmodifiableMap(result);
        }catch(java.io.IOException e){throw new IllegalStateException("Pinned mappings unavailable",e);}
    }

    public static CtBinding binding(String resource){
        JsonNode spec=Objects.requireNonNull(SPECS.get(resource),"Unknown P02 resource");
        return new CtBinding(new CtCatalog().resource(resource),row->{
            var data=JSON.createObjectNode();
            for(var field:spec.path("fields")){
                String name=field.path("json").asText(),column=field.path("sql").asText(),type=field.path("sqlType").asText();
                if(name.equals("source_id"))continue;
                if(row.getObject(column)==null){data.putNull(name);continue;}
                if(type.equals("bigint"))data.put(name,Long.toString(row.getLong(column)));
                else if(type.equals("int")||type.equals("smallint"))data.put(name,row.getInt(column));
                else if(type.startsWith("decimal"))data.put(name,row.getBigDecimal(column).toPlainString());
                else if(type.equals("date"))data.put(name,row.getDate(column).toLocalDate().toString());
                else if(type.startsWith("datetimeoffset"))data.put(name,row.getObject(column,java.time.OffsetDateTime.class).withOffsetSameInstant(ZoneOffset.UTC).toString());
                else data.put(name,row.getString(column));
            }
            return data;
        });
    }
}
