package pbl6.daotao.sync;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
public final class CtCatalog {
 private final Map<String,CtResource> resources;
 public CtCatalog(){try(var input=getClass().getResourceAsStream("/contracts/ct26-resources.json")){
  List<CtResource> rows=new ObjectMapper().readValue(input,new TypeReference<>(){});var map=new LinkedHashMap<String,CtResource>();
  for(var row:rows)if(map.put(row.resource(),row)!=null)throw new IllegalStateException("Duplicate resource");resources=Map.copyOf(map);
 }catch(java.io.IOException e){throw new IllegalStateException("Approved catalog unavailable",e);}}
 public CtResource resource(String name){var result=resources.get(name);if(result==null)throw new IllegalArgumentException("Unknown SIS resource");return result;}
 public Collection<CtResource> all(){return resources.values();}
}
