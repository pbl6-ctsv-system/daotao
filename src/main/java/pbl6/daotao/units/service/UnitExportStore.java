package pbl6.daotao.units.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.*;
import java.util.*;
import pbl6.daotao.units.config.UnitExportProperties;
import pbl6.daotao.units.dto.*;
import pbl6.daotao.units.repository.UnitChangeTrackingRepository.Metadata;

/** Bounded immutable exports; item JSON bytes are retained once. */
public final class UnitExportStore {
    private final UnitExportProperties properties;
    private final ObjectMapper mapper;
    private final Clock clock;
    private final Map<String,Binding> cursors=new HashMap<>();
    private final Set<Reservation> retained=new HashSet<>();
    private long reservedBytes;
    private int active;
    private record Binding(Reservation batch,int offset) {}
    public UnitExportStore(UnitExportProperties properties,ObjectMapper mapper,Clock clock) {
        this.properties=properties;this.mapper=mapper;this.clock=clock;
    }
    public synchronized Reservation reserve(UnitExportRequest request,String caller) {
        prune();
        if(active>=properties.maxActive()||reservedBytes+properties.maxJsonBytes()>properties.maxRetainedBytes())
            throw new UnitApiException(503,"EXPORT_CAPACITY_EXCEEDED",null);
        active++;reservedBytes+=properties.maxJsonBytes();
        return new Reservation(request,caller);
    }
    public synchronized UnitExportResponse page(String cursor,String caller) {
        prune();
        try{if(cursor.length()!=36||!UUID.fromString(cursor).toString().equalsIgnoreCase(cursor))throw new IllegalArgumentException();}
        catch(RuntimeException e){throw UnitApiException.invalid("cursor");}
        Binding binding=cursors.get(cursor);
        if(binding==null)throw new UnitApiException(409,"EXPORT_EXPIRED",null);
        if(!binding.batch.caller.equals(caller))throw new UnitApiException(403,"SERVICE_FORBIDDEN",null);
        return binding.batch.page(binding.offset);
    }
    public synchronized void prune() {
        var expired=retained.stream().filter(r->!clock.instant().isBefore(r.expiresAt)).toList();
        for(var batch:expired)release(batch);
    }
    public synchronized long retainedBytes(){return reservedBytes;}
    public synchronized int activeExports(){return active;}
    private void release(Reservation r) {
        if(r.released)return;
        r.released=true;active--;reservedBytes-=r.budget;
        retained.remove(r);cursors.entrySet().removeIf(e->e.getValue().batch==r);r.rows.clear();
    }
    public final class Reservation implements AutoCloseable {
        private final UnitExportRequest request;
        private final String caller,exportId=UUID.randomUUID().toString(),firstCursor=UUID.randomUUID().toString();
        private final Instant expiresAt=clock.instant().plus(properties.ttl());
        private final List<byte[]> rows=new ArrayList<>();
        private final Map<Integer,String> offsets=new HashMap<>();
        private Metadata metadata;
        private long itemBytes,headerBytes,budget=properties.maxJsonBytes();
        private boolean published,released;
        private Reservation(UnitExportRequest request,String caller){this.request=request;this.caller=caller;}
        public void start(Metadata metadata) {
            this.metadata=metadata;headerBytes=bytes(response(List.of(),null)).length;
        }
        public void append(UnitChangeDto item) {
            if(rows.size()>=properties.maxRows())throw new UnitApiException(503,"EXPORT_LIMIT_EXCEEDED",null);
            byte[] json=bytes(item);int count=rows.size()+1;
            long size=headerBytes+itemBytes+json.length+Math.max(0,count-1)+(count>request.limit()?34:0);
            if(size>properties.maxJsonBytes())throw new UnitApiException(503,"EXPORT_LIMIT_EXCEEDED",null);
            rows.add(json);itemBytes+=json.length;
        }
        /** Only publish after the source SNAPSHOT transaction commits. */
        public UnitExportResponse publish() {
            synchronized(UnitExportStore.this) {
                if(metadata==null||released)throw new UnitApiException(503,"SOURCE_UNAVAILABLE",null);
                if(!clock.instant().isBefore(expiresAt))throw new UnitApiException(409,"EXPORT_EXPIRED",null);
                if(rows.size()>request.limit()) {
                    long actual=headerBytes+itemBytes+Math.max(0,rows.size()-1)+34;
                    reservedBytes-=budget-actual;budget=actual;retained.add(this);
                    offsets.put(request.limit(),firstCursor);cursors.put(firstCursor,new Binding(this,request.limit()));
                    try{UnitExportResponse result=page(0);published=true;return result;}
                    catch(RuntimeException e){release(this);throw e;}
                }
                UnitExportResponse result=page(0);release(this);return result;
            }
        }
        private UnitExportResponse response(List<UnitChangeDto> items,String cursor) {
            return new UnitExportResponse(metadata.mode(),properties.sourceEpoch().toString(),exportId,
                    metadata.fromVersion(),metadata.toVersion(),metadata.asOf(),items,cursor);
        }
        private UnitExportResponse page(int offset) {
            int end=Math.min(offset+request.limit(),rows.size());String next=null;
            if(end<rows.size()) {
                next=offsets.computeIfAbsent(end,k->UUID.randomUUID().toString());
                cursors.putIfAbsent(next,new Binding(this,end));
            }
            List<UnitChangeDto> items=new ArrayList<>(end-offset);
            try{for(int i=offset;i<end;i++)items.add(mapper.readValue(rows.get(i),UnitChangeDto.class));}
            catch(java.io.IOException e){throw new UnitApiException(503,"SOURCE_UNAVAILABLE",null);}
            return response(items,next);
        }
        @Override public void close() {
            synchronized(UnitExportStore.this){if(!published)release(this);}
        }
    }
    private byte[] bytes(Object value) {
        try{return mapper.writeValueAsBytes(value);}
        catch(java.io.IOException e){throw new UnitApiException(503,"SOURCE_UNAVAILABLE",null);}
    }
}

