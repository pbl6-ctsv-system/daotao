package pbl6.daotao.sync;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record CtRequest(Long sinceVersion,UUID sourceEpoch,int limit,String cursor) {
    public static CtRequest parse(Map<String,String[]> params,boolean hasBody,CtResource.Profile profile) {
        if(hasBody) throw CtApiException.invalid("body");
        for(var entry:params.entrySet()) {
            if(!Set.of("since_version","source_epoch","limit","cursor").contains(entry.getKey())
                    || entry.getValue()==null || entry.getValue().length!=1
                    || entry.getValue()[0]==null || entry.getValue()[0].isEmpty())
                throw CtApiException.invalid(entry.getKey());
        }
        if(params.containsKey("cursor")) {
            if(params.size()!=1) throw CtApiException.invalid("cursor");
            return new CtRequest(null,null,profile.defaultLimit(),params.get("cursor")[0]);
        }
        Long since=null; UUID epoch=null; int limit=profile.defaultLimit();
        if(params.containsKey("since_version") != params.containsKey("source_epoch"))
            throw CtApiException.invalid("source_epoch");
        if(params.containsKey("since_version")) {
            String value=params.get("since_version")[0];
            try {
                if(!value.matches("[0-9]+")) throw new NumberFormatException();
                since=Long.parseLong(value);
            } catch(NumberFormatException e){throw CtApiException.invalid("since_version");}
            String valueEpoch=params.get("source_epoch")[0];
            try {
                if(!valueEpoch.matches("[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}"))
                    throw new IllegalArgumentException();
                epoch=UUID.fromString(valueEpoch);
            }catch(IllegalArgumentException e){throw CtApiException.invalid("source_epoch");}
        }
        if(params.containsKey("limit")) {
            try {
                String value=params.get("limit")[0];
                if(!value.matches("[0-9]+")) throw new NumberFormatException();
                limit=Integer.parseInt(value);
                if(limit<1 || limit>profile.maxLimit()) throw new NumberFormatException();
            }catch(NumberFormatException e){throw CtApiException.invalid("limit");}
        }
        return new CtRequest(since,epoch,limit,null);
    }
}

