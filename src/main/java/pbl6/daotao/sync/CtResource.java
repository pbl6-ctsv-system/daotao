package pbl6.daotao.sync;
public record CtResource(String resource,String url,String table,String primaryKey,String sqlKeyType,Profile profile) {
 public record Profile(int defaultLimit,int maxLimit,long maxRows,long maxBytes,int deadlineSeconds) {
  public Profile {if(defaultLimit<1||maxLimit<defaultLimit||maxRows<1||maxBytes<1||deadlineSeconds<1)throw new IllegalArgumentException("Invalid export profile");}
 }
 public CtResource {if(resource==null||!resource.matches("[a-z]+(?:-[a-z]+)*")||!table.matches("[A-Za-z][A-Za-z0-9_]*")||!primaryKey.matches("[A-Za-z][A-Za-z0-9_]*")||!url.startsWith("/api/dao-tao/")||profile==null)throw new IllegalArgumentException("Invalid approved resource");}
}
