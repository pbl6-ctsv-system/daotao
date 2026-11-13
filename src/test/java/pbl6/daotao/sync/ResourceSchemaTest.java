package pbl6.daotao.sync;
import java.util.*;
import java.util.stream.*;
import org.junit.jupiter.api.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import static org.assertj.core.api.Assertions.*;

/** Strict contract and SQL representation, not a runtime SQL/HTTP claim. */
class ResourceSchemaTest {
    static final ObjectMapper JSON=new ObjectMapper();
    static ObjectNode fixture(String resource)throws Exception{
        return (ObjectNode)JSON.readTree(ResourceSchemaTest.class.getResourceAsStream("/fixtures/p02/wire-data.json")).path("data").path(resource).deepCopy();
    }
    static ObjectNode page(ObjectNode data){
        var p=JSON.createObjectNode().put("mode","full").put("source_epoch","11111111-1111-4111-8111-111111111111").put("export_id",UUID.randomUUID().toString()).put("to_version","12").put("as_of","2026-11-12T00:00:00Z");p.putNull("from_version");p.putNull("next_cursor");var item=p.putArray("items").addObject().put("source_id","1").put("operation","upsert");item.putNull("change_version");item.set("data",data);return p;
    }
    static List<String> resources()throws Exception {var result=new ArrayList<String>();JSON.readTree(ResourceSchemaTest.class.getResourceAsStream("/fixtures/p02/wire-data.json")).path("data").fieldNames().forEachRemaining(result::add);return result;}
    @TestFactory Stream<DynamicTest> perResourceContractChecks()throws Exception {
        var definitions=JSON.readTree(getClass().getResourceAsStream("/contracts/sis-ct26.schema.json")).path("$defs");
        return resources().stream().flatMap(resource->Stream.of(
            DynamicTest.dynamicTest(resource+": valid, empty and nullable",()->{
                var validator=new CtSchema(resource);var p=page(fixture(resource));validator.validate(p);p.withArray("items").removeAll();validator.validate(p);
                var data=fixture(resource);var props=definitions.path(resource+"_ct_data").path("properties");
                for(var name:new ArrayList<>(data.properties().stream().map(Map.Entry::getKey).toList()))if(props.path(name).path("anyOf").isArray())data.putNull(name);
                validator.validate(page(data));
            }),
            DynamicTest.dynamicTest(resource+": missing, extra and wrong shape",()->{
                var validator=new CtSchema(resource);var d=fixture(resource);
                for(var key:new ArrayList<>(d.properties().stream().map(Map.Entry::getKey).toList())){var bad=d.deepCopy();bad.remove(key);assertThatThrownBy(()->validator.validate(page(bad))).isInstanceOf(IllegalArgumentException.class);}
                assertThatThrownBy(()->validator.validate(page(d.deepCopy().put("unexpected",1)))).isInstanceOf(IllegalArgumentException.class);
                var bad=page(d);bad.put("to_version","9223372036854775808");assertThatThrownBy(()->validator.validate(bad)).isInstanceOf(RuntimeException.class);
            }),
            DynamicTest.dynamicTest(resource+": delta/delete/window",()->{
                var validator=new CtSchema(resource);var p=page(fixture(resource)).put("mode","incremental").put("from_version","10");var item=(ObjectNode)p.path("items").get(0);item.put("change_version","11");validator.validate(p);item.put("operation","delete").putNull("data");validator.validate(p);
                item.put("change_version","10");assertThatThrownBy(()->validator.validate(p)).isInstanceOf(IllegalArgumentException.class);
            }),
            DynamicTest.dynamicTest(resource+": SQL text/number boundary",()->{
                var props=definitions.path(resource+"_ct_data").path("properties");String field=null;int max=0;
                for(var entry:props.properties())for(var branch:entry.getValue().path("anyOf"))if(branch.has("maxLength")){field=entry.getKey();max=branch.path("maxLength").asInt();break;}
                var d=fixture(resource);
                if(field!=null)d.put(field,"😀".repeat(max/2+1));
                else {String first=d.fieldNames().next();d.put(first,true);}
                var validator=new CtSchema(resource);
                assertThatThrownBy(()->validator.validate(page(d))).isInstanceOf(IllegalArgumentException.class);
            })
        ));
    }
}
