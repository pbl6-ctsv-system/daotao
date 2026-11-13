package pbl6.daotao.sync;
import java.sql.*;
import java.util.*;
import java.util.stream.*;
import org.junit.jupiter.api.*;
import com.fasterxml.jackson.databind.*;
import pbl6.p01.*;
import static org.assertj.core.api.Assertions.*;

class SisMappingIT {
    static final ObjectMapper JSON=new ObjectMapper();
    static List<JsonNode> specs()throws Exception{
        var result=new ArrayList<JsonNode>();for(var s:JSON.readTree(SisMappingIT.class.getResourceAsStream("/contracts/foundation-resources.json")))result.add(s);return result;
    }
    @BeforeAll static void prepare()throws Exception{FaultMigrationRunner.main(new String[0]);}
    @TestFactory Stream<DynamicTest> sqlRepresentationsMatchPinnedSchemaForEveryP02Resource()throws Exception{
        return specs().stream().map(spec->DynamicTest.dynamicTest(spec.path("resource").asText(),()->{
            String resource=spec.path("resource").asText();var expr=new ArrayList<String>();
            for(var f:spec.path("fields")){
                String type=f.path("sqlType").asText();String literal=type.equals("date")?"'2026-11-12'":type.startsWith("decimal")?"'123456789012345678.1234567890'":type.matches("bigint|int|smallint")?"7":"N'RAW'";
                expr.add("CAST("+literal+" AS "+type+") AS ["+f.path("sql").asText()+"]");
            }
            try(var c=FaultDatabase.open();var s=c.createStatement();var row=s.executeQuery("SELECT "+String.join(",",expr))){
                assertThat(row.next()).isTrue();var data=SisResourceMapper.binding(resource).mapper().data(row);
                var page=JSON.createObjectNode().put("mode","full").put("source_epoch","11111111-1111-4111-8111-111111111111").put("export_id",UUID.randomUUID().toString()).put("to_version","0").put("as_of","2026-11-12T00:00:00Z");page.putNull("from_version");page.putNull("next_cursor");var item=page.putArray("items").addObject().put("source_id","7").put("operation","upsert");item.putNull("change_version");item.set("data",data);new CtSchema(resource).validate(page);
                if(resource.equals("courses"))assertThat(data.path("project_credits").asText()).isEqualTo("123456789012345678.1234567890");
                if(resource.equals("rooms"))assertThat(data.path("lock_flag_source").isIntegralNumber()).isTrue();
                if(resource.equals("students"))assertThat(data.path("birth_date").asText()).isEqualTo("2026-11-12");
            }
        }));
    }
    @Test void runtimeSourceColumnsMatchDeclaredTypesAndTracking()throws Exception{
        try(var c=FaultDatabase.open()){
            for(var spec:specs()){
                var resource=new CtCatalog().resource(spec.path("resource").asText());CtBootstrap.verifyKey(c,resource);
                for(var f:spec.path("fields"))try(var s=c.prepareStatement("SELECT TYPE_NAME(system_type_id),max_length,precision,scale,is_nullable FROM sys.columns WHERE object_id=OBJECT_ID(?) AND name=?")){
                    s.setString(1,"dbo."+resource.table());s.setString(2,f.path("sql").asText());try(var r=s.executeQuery()){
                        assertThat(r.next()).as(resource.table()+"."+f.path("sql").asText()).isTrue();String type=r.getString(1);
                        if(type.equals("nvarchar")||type.equals("varchar"))type+="("+(r.getInt(2)/(type.equals("nvarchar")?2:1))+")";
                        if(type.equals("decimal"))type+="("+r.getInt(3)+","+r.getInt(4)+")";
                        assertThat(type).isEqualTo(f.path("sqlType").asText());assertThat(r.getBoolean(5)).isEqualTo(f.path("nullable").asBoolean());
                    }
                }
            }
            assertThat(FaultDatabase.number("SELECT COUNT(*) FROM sys.change_tracking_tables")).isEqualTo(24);
        }
    }
    @Test void replacedCourseAndOriginalCourseAreDifferentSourceFields()throws Exception{
        try(var c=FaultDatabase.open();var s=c.createStatement();var row=s.executeQuery("SELECT CAST(11 AS bigint) HocPhanID,CAST(12 AS bigint) HocPhanDuocThayTheID,CAST(NULL AS nvarchar(128)) MaKhung,CAST(NULL AS smallint) HocKy,CAST(NULL AS nvarchar(128)) TuChon,CAST(NULL AS nvarchar(128)) TQDA,CAST(NULL AS nvarchar(128)) DATN")){
            assertThat(row.next()).isTrue();var data=SisResourceMapper.binding("curriculum-items").mapper().data(row);
            assertThat(data.path("course_source_id").asText()).isEqualTo("11");assertThat(data.path("replaced_course_source_id").asText()).isEqualTo("12");
        }
    }
}
