package pbl6.daotao.units;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pbl6.daotao.units.dto.UnitExportRequest;
import pbl6.daotao.units.service.UnitApiException;
import pbl6.daotao.units.validation.UnitContractValidator;
import static org.assertj.core.api.Assertions.*;

class UnitContractValidatorTest {
    private final ObjectMapper mapper=new ObjectMapper();
    private final UnitContractValidator validator=new UnitContractValidator();
    ObjectNode valid() throws Exception {return (ObjectNode)mapper.readTree("""
        {"mode":"full","source_epoch":"11111111-2222-3333-4444-555555555555",
         "export_id":"22222222-2222-3333-4444-555555555555","from_version":null,
         "to_version":"123","as_of":"2026-11-11T00:00:00Z","next_cursor":null,
         "items":[{"source_id":"9007199254740993","operation":"upsert","change_version":null,
          "data":{"unit_code":"DV","unit_name":null,"department_code":null,"updated_at":"2026-11-11T00:00:00Z",
          "is_active":true,"archive_status":null,"unit_classification":"raw"}}]}
        """);}
    @Test void sa041_fullRawNullAndLargeBigintAreValid() throws Exception {validator.validate(valid());}
    @ParameterizedTest @ValueSource(strings={"extra","missing","numberId","overflow","negative","badUtc","badUuid","fullVersion","nullUpsert","badType","duplicate","order","rawMissing"})
    void sa042_044_rejectInvalidResponseBeforeDatabaseWrite(String variant) throws Exception {
        ObjectNode n=valid(),item=(ObjectNode)n.path("items").get(0),data=(ObjectNode)item.path("data");
        switch(variant){
            case "extra"->n.put("unapproved",true);
            case "missing"->n.remove("export_id");
            case "numberId"->item.put("source_id",9007199254740993L);
            case "overflow"->item.put("source_id","9223372036854775808");
            case "negative"->item.put("source_id","-1");
            case "badUtc"->n.put("as_of","2026-11-11T07:00:00+07:00");
            case "badUuid"->n.put("source_epoch","1-1-1-1-1");
            case "fullVersion"->item.put("change_version","123");
            case "nullUpsert"->item.putNull("data");
            case "badType"->data.put("is_active","true");
            case "duplicate"->((ArrayNode)n.path("items")).add(item.deepCopy());
            case "order"->{ObjectNode next=item.deepCopy();next.put("source_id","1");((ArrayNode)n.path("items")).add(next);}
            case "rawMissing"->data.remove("archive_status");
        }
        assertThatThrownBy(()->validator.validate(n)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void sa043_countsUtf16NotOnlyCodePoints() throws Exception {
        ObjectNode n=valid();ObjectNode data=(ObjectNode)n.path("items").get(0).path("data");
        data.put("archive_status","\uD83D\uDE00".repeat(64));validator.validate(n);
        data.put("archive_status","\uD83D\uDE00".repeat(65));
        assertThatThrownBy(()->validator.validate(n)).isInstanceOf(IllegalArgumentException.class);
    }
    @ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"unit_code,128","department_code,128","unit_name,512","archive_status,128","unit_classification,128"})
    void sa043_allTextBoundariesAreCountedInUtf16(String field,int maximum)throws Exception {
        ObjectNode n=valid(),data=(ObjectNode)n.path("items").get(0).path("data");
        data.put(field,"ệ".repeat(maximum));validator.validate(n);
        data.put(field,"ệ".repeat(maximum+1));assertThatThrownBy(()->validator.validate(n)).isInstanceOf(IllegalArgumentException.class);
        data.put(field,"\uD83D\uDE00".repeat(maximum/2));validator.validate(n);
        data.put(field,"\uD83D\uDE00".repeat(maximum/2)+"x");assertThatThrownBy(()->validator.validate(n)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void sae007_oldStrictDataShapeRejectsNewFieldsWhileApprovedCoordinatedShapeAccepts()throws Exception {
        ObjectNode n=valid();validator.validate(n);
        var current=(ObjectNode)mapper.readTree(getClass().getResourceAsStream("/contracts/sis-units.schema.json"));
        // Exact pre-delta data shape: five original required properties, additionalProperties=false.
        ObjectNode data=(ObjectNode)current.path("$defs").path("data");
        ((ObjectNode)data.path("properties")).remove(List.of("archive_status","unit_classification"));
        var required=(ArrayNode)data.path("required");required.removeAll();
        List.of("unit_code","unit_name","department_code","updated_at","is_active").forEach(required::add);
        var old=com.networknt.schema.JsonSchemaFactory.getInstance(com.networknt.schema.SpecVersion.VersionFlag.V202012).getSchema(current);
        assertThat(old.validate(n)).isNotEmpty();
        var payload=(ObjectNode)n.path("items").get(0).path("data");payload.remove(List.of("archive_status","unit_classification"));
        assertThat(old.validate(n)).isEmpty();
        assertThatThrownBy(()->validator.validate(n)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void sa044_deltaChecksWindowAndDeleteShape() throws Exception {
        ObjectNode n=valid();n.put("mode","incremental");n.put("from_version","120");
        ObjectNode item=(ObjectNode)n.path("items").get(0);item.put("operation","delete");item.put("change_version","121");item.putNull("data");
        validator.validate(n);
        item.put("change_version","120");
        assertThatThrownBy(()->validator.validate(n)).isInstanceOf(IllegalArgumentException.class);
        item.put("change_version","124");
        assertThatThrownBy(()->validator.validate(n)).isInstanceOf(IllegalArgumentException.class);
    }
    @ParameterizedTest @ValueSource(strings={"body","unknown","negative","overflow","epochOnly","sinceOnly","limit0","limit501","duplicate","cursorCombination"})
    void sa038_rejectInvalidQuery(String variant) {
        Map<String,String[]> p=new HashMap<>();
        switch(variant){
            case "unknown"->p.put("updated_since",new String[]{"x"});
            case "negative"->p.put("since_version",new String[]{"-1"});
            case "overflow"->p.put("since_version",new String[]{"9223372036854775808"});
            case "epochOnly"->p.put("source_epoch",new String[]{"11111111-2222-3333-4444-555555555555"});
            case "sinceOnly"->p.put("since_version",new String[]{"1"});
            case "limit0"->p.put("limit",new String[]{"0"});
            case "limit501"->p.put("limit",new String[]{"501"});
            case "duplicate"->p.put("limit",new String[]{"1","2"});
            case "cursorCombination"->{p.put("cursor",new String[]{UUID.randomUUID().toString()});p.put("limit",new String[]{"1"});}
        }
        assertThatThrownBy(()->UnitExportRequest.parse(p,variant.equals("body"))).isInstanceOf(UnitApiException.class);
    }
    @Test void sa037_defaultAndIncrementalRequests() {
        assertThat(UnitExportRequest.parse(Map.of(),false).limit()).isEqualTo(100);
        var delta=UnitExportRequest.parse(Map.of("since_version",new String[]{"0"},"source_epoch",new String[]{"11111111-2222-3333-4444-555555555555"},"limit",new String[]{"500"}),false);
        assertThat(delta.sinceVersion()).isZero();assertThat(delta.limit()).isEqualTo(500);
    }
}

