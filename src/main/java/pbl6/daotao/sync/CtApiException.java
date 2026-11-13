package pbl6.daotao.sync;
public final class CtApiException extends RuntimeException {
 public final int status;public final String code,field;
 public CtApiException(int status,String code){this(status,code,null);}
 public CtApiException(int status,String code,String field){super(code);this.status=status;this.code=code;this.field=field;}
 public static CtApiException invalid(String field){return new CtApiException(400,"INVALID_REQUEST",field);}
}
