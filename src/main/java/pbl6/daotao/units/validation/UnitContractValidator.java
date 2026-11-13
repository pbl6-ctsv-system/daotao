package pbl6.daotao.units.validation;
import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.*;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
public final class UnitContractValidator {
    private final JsonSchema schema;
    public UnitContractValidator() {
        try(var input=getClass().getResourceAsStream("/contracts/sis-units.schema.json")) {
            if(input==null) throw new IllegalStateException("Approved units schema is missing");
            var config=new SchemaValidatorsConfig();
            config.setFormatAssertionsEnabled(true);
            schema=JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012).getSchema(input,config);
        }catch(java.io.IOException e){throw new IllegalStateException("Cannot load units schema",e);}
    }
    public void validate(JsonNode node) {
        if(!schema.validate(node).isEmpty()) invalid();
        uuid(node.path("source_epoch").asText()); uuid(node.path("export_id").asText());
        utc(node.path("as_of").asText());
        long upper=number(node.path("to_version"));
        boolean full=node.path("mode").asText().equals("full");
        long lower=full?0:number(node.path("from_version"));
        if(lower>upper) invalid();
        long previous=0;
        for(var item:node.path("items")) {
            long id=number(item.path("source_id"));
            if(id<=previous) invalid(); previous=id;
            if(!full){long version=number(item.path("change_version"));if(version<=lower||version>upper)invalid();}
            if(item.path("operation").asText().equals("upsert")) {
                var data=item.path("data");
                length(data.path("unit_code"),128);length(data.path("unit_name"),512);
                length(data.path("department_code"),128);length(data.path("archive_status"),128);
                length(data.path("unit_classification"),128);utc(data.path("updated_at").asText());
            }
        }
    }
    private static long number(JsonNode value) {
        try {long n=Long.parseLong(value.textValue());if(n<0)invalid();return n;}
        catch(RuntimeException e){throw new IllegalArgumentException("Invalid units numeric value");}
    }
    private static void uuid(String value) {
        try {if(value.length()!=36 || !UUID.fromString(value).toString().equalsIgnoreCase(value))invalid();}
        catch(RuntimeException e){throw new IllegalArgumentException("Invalid units identity");}
    }
    private static void utc(String value) {
        try {if(!OffsetDateTime.parse(value).getOffset().equals(ZoneOffset.UTC))invalid();}
        catch(RuntimeException e){throw new IllegalArgumentException("Invalid units UTC timestamp");}
    }
    private static void length(JsonNode value,int max){if(!value.isNull()&&value.asText().length()>max)invalid();}
    private static void invalid(){throw new IllegalArgumentException("Response violates the approved units contract");}
}

