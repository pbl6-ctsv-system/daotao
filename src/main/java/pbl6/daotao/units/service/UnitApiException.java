package pbl6.daotao.units.service;
public final class UnitApiException extends RuntimeException {
    private final int status; private final String code; private final String field;
    public UnitApiException(int status,String code,String field) {
        super(code);this.status=status;this.code=code;this.field=field;
    }
    public int status(){return status;}
    public String code(){return code;}
    public String field(){return field;}
    public static UnitApiException invalid(String field){return new UnitApiException(400,"INVALID_REQUEST",field);}
}

