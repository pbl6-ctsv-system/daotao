package pbl6.p02;
import org.springframework.boot.SpringApplication;import org.springframework.context.annotation.*;import pbl6.daotao.DaotaoApplication;import pbl6.daotao.sync.*;import com.fasterxml.jackson.databind.ObjectMapper;
/** Test-only mapper; exercises production HTTP/security/reader/store without implementing P02-03. */
public final class CtFixtureProvider {
 public static void main(String[] args){SpringApplication.run(new Class<?>[]{DaotaoApplication.class,Fixture.class},args);}
 @Configuration(proxyBeanMethods=false) public static class Fixture {
  @Bean CtBinding fixtureBinding(){return new CtBinding(new CtCatalog().resource("ethnicities"),row->{var n=new ObjectMapper().createObjectNode();if(row.getString("MaDanToc")==null)n.putNull("code");else n.put("code",row.getString("MaDanToc"));if(row.getString("TenDanToc")==null)n.putNull("name");else n.put("name",row.getString("TenDanToc"));return n;});}
 }
}
