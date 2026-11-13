package pbl6.daotao.sync;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import java.util.function.Consumer;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;

/** Private indexed disk spool; only one requested page is materialized in heap. */
public class DiskExportStore implements AutoCloseable {
    @FunctionalInterface public interface Producer {void read(Consumer<CtMetadata> metadata,Consumer<JsonNode> rows);}
    private final ObjectMapper json=new ObjectMapper();
    private final Path root;
    private final Clock clock;
    private final int maxSlots;
    private final long maxBytes;
    private final Duration ttl;
    private FileChannel lockChannel;
    private FileLock directoryLock;
    private java.util.concurrent.ScheduledExecutorService janitor;
    private final Map<UUID,Batch> batches=new HashMap<>();
    private int slots;private long reserved;
    private record Batch(CtResource resource,String caller,CtMetadata metadata,int limit,long rows,long bytes,Instant expires,byte[] key,Path data,Path index) {}
    public DiskExportStore(Path root,Clock clock){this(root,clock,2,256L*1048576,Duration.ofMinutes(15));}
    public DiskExportStore(Path root,Clock clock,int maxSlots,long maxBytes,Duration ttl){
        if(maxSlots<1||maxBytes<1||ttl.isNegative()||ttl.isZero())throw new IllegalArgumentException("Invalid spool limits");
        this.root=root.toAbsolutePath().normalize();this.clock=clock;this.maxSlots=maxSlots;this.maxBytes=maxBytes;this.ttl=ttl;
        try{
            if(Files.isSymbolicLink(this.root))throw new IOException("Spool symlink refused");
            Files.createDirectories(this.root);
            Path marker=this.root.resolve(".ct26-owner");
            if(!Files.exists(marker)){try(var files=Files.list(this.root)){if(files.findAny().isPresent())throw new IOException("Unowned spool directory");}Files.writeString(marker,"CT26_PRIVATE_SPOOL");}
            if(!Files.readString(marker).equals("CT26_PRIVATE_SPOOL"))throw new IOException("Wrong spool owner marker");
            restrict(this.root,true);
            lockChannel=openLockChannel(this.root.resolve(".lock"));
            directoryLock=lockChannel.tryLock();if(directoryLock==null)throw new IOException("Spool already owned by a provider");
            try(var files=Files.list(this.root)){for(Path file:files.toList())if(file.getFileName().toString().matches("ct-[0-9a-f-]{36}\\.(data|index)"))Files.delete(file);}
            janitor=java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"ct26-spool-cleanup");t.setDaemon(true);return t;});
            janitor.scheduleAtFixedRate(()->{synchronized(this){try{prune();}catch(RuntimeException ignored){/* Keep failed deletion charged and retry; never publish stale data. */}}},1000,1000,java.util.concurrent.TimeUnit.MILLISECONDS);
        }catch(IOException|RuntimeException e){
            try{if(directoryLock!=null&&directoryLock.isValid())directoryLock.release();}catch(IOException ignored){}
            try{if(lockChannel!=null)lockChannel.close();}catch(IOException ignored){}
            throw new IllegalStateException("Cannot initialize private spool",e);
        }
    }
    protected FileChannel openLockChannel(Path path)throws IOException{return FileChannel.open(path,StandardOpenOption.CREATE,StandardOpenOption.WRITE);}
    private static void restrict(Path path,boolean directory)throws IOException {
        var posix=Files.getFileAttributeView(path,PosixFileAttributeView.class);
        if(posix!=null){posix.setPermissions(PosixFilePermissions.fromString(directory?"rwx------":"rw-------"));return;}
        var acl=Files.getFileAttributeView(path,AclFileAttributeView.class);
        if(acl==null)throw new IOException("Private spool requires filesystem permissions");
        var builder=AclEntry.newBuilder().setType(AclEntryType.ALLOW).setPrincipal(Files.getOwner(path)).setPermissions(EnumSet.allOf(AclEntryPermission.class));
        if(directory)builder.setFlags(AclEntryFlag.DIRECTORY_INHERIT,AclEntryFlag.FILE_INHERIT);
        acl.setAcl(List.of(builder.build()));
    }
    public ObjectNode create(CtResource resource,String caller,CtRequest request,Producer producer){
        if(resource.resource().equals("units"))throw new IllegalArgumentException("Units has a separate approved store");
        synchronized(this){prune();if(slots>=maxSlots)throw new CtApiException(503,"EXPORT_CAPACITY_EXCEEDED");slots++;}
        UUID id=UUID.randomUUID();Path data=root.resolve("ct-"+id+".data"),index=root.resolve("ct-"+id+".index");
        final long[] rows={0},charge={0},previous={0};final CtMetadata[] metadata={null};byte[] key=new byte[32];new SecureRandom().nextBytes(key);
        var validator=new CtSchema(resource.resource());
        try(var output=new RandomAccessFile(data.toFile(),"rw");var offsets=new DataOutputStream(Files.newOutputStream(index,StandardOpenOption.CREATE_NEW))){
            restrict(data,false);restrict(index,false);
            producer.read(m->{if(metadata[0]!=null)throw new IllegalStateException("Duplicate metadata");metadata[0]=new CtMetadata(m.mode(),m.epoch(),id,m.fromVersion(),m.toVersion(),m.asOf());},row->{
                try{
                    if(metadata[0]==null)throw new IllegalStateException("Metadata required before rows");
                    var one=json.createArrayNode().add(row);validator.validate(metadata[0].page(one,null));
                    long current=CtSchema.number(row.path("source_id"));if(current<=previous[0])throw new CtApiException(503,"SYNC_NOT_READY");previous[0]=current;
                    if(++rows[0]>resource.profile().maxRows())throw new CtApiException(503,"EXPORT_LIMIT_EXCEEDED");
                    byte[] bytes=json.writeValueAsBytes(row);reserve(bytes.length,charge,resource);offsets.writeLong(output.getFilePointer());output.write(bytes);output.write('\n');afterRecordStored(rows[0]);
                }catch(IOException e){throw new CtApiException(503,"SOURCE_UNAVAILABLE");}
            });
            if(metadata[0]==null)throw new CtApiException(503,"SYNC_NOT_READY");
            validator.validate(metadata[0].page(json.createArrayNode(),null));
            // Charge actual wrapper, commas and next-cursor for every page, not only data strings.
            long pages=Math.max(1,(rows[0]+request.limit()-1)/request.limit());
            for(long page=0;page<pages;page++){
                long count=Math.min(request.limit(),rows[0]-page*request.limit());
                String cursor=page+1<pages?cursor(id,(page+1)*request.limit(),key):null;
                long overhead=json.writeValueAsBytes(metadata[0].page(json.createArrayNode(),cursor)).length+Math.max(0,count-1);
                reserve(overhead,charge,resource);
            }
            Batch batch=new Batch(resource,caller,metadata[0],request.limit(),rows[0],charge[0],clock.instant().plus(ttl),key,data,index);
            // Producer returns only after SQL SNAPSHOT commit. No published manifest on read/commit failure.
            synchronized(this){batches.put(id,batch);}
        }catch(Exception e){
            synchronized(this){slots--;reserved-=charge[0];batches.remove(id);}
            delete(data);delete(index);
            if(e instanceof CtApiException api)throw api;
            if(e instanceof IllegalArgumentException)throw new CtApiException(503,"SYNC_NOT_READY");
            throw new CtApiException(503,"SOURCE_UNAVAILABLE");
        }
        return page(id,0,caller,resource.resource());
    }
    protected void afterRecordStored(long rows)throws IOException {}
    private synchronized void reserve(long amount,long[] charge,CtResource r){
        if(amount>r.profile().maxBytes()-charge[0])throw new CtApiException(503,"EXPORT_LIMIT_EXCEEDED");
        if(amount>maxBytes-reserved)throw new CtApiException(503,"EXPORT_CAPACITY_EXCEEDED");
        charge[0]+=amount;reserved+=amount;
    }
    public ObjectNode next(String token,String caller,String resource,UUID epoch){
        try{
            String[] parts=new String(Base64.getUrlDecoder().decode(token),StandardCharsets.US_ASCII).split(":",-1);
            if(parts.length!=3||parts[0].length()!=36||!parts[1].matches("[1-9][0-9]*"))throw CtApiException.invalid("cursor");
            UUID id=UUID.fromString(parts[0]);long offset=Long.parseLong(parts[1]);
            synchronized(this){
                prune();var b=batches.get(id);if(b==null)throw new CtApiException(409,"EXPORT_EXPIRED");
                if(!b.metadata.epoch().equals(epoch))throw new CtApiException(409,"SOURCE_EPOCH_CHANGED");
                if(!MessageDigest.isEqual(token.getBytes(StandardCharsets.US_ASCII),cursor(id,offset,b.key).getBytes(StandardCharsets.US_ASCII))||offset%b.limit!=0||offset>=b.rows)throw CtApiException.invalid("cursor");
                return page(id,offset,caller,resource);
            }
        }catch(CtApiException e){throw e;}catch(RuntimeException e){throw CtApiException.invalid("cursor");}
    }
    private synchronized ObjectNode page(UUID id,long start,String caller,String resource){
        prune();Batch b=batches.get(id);if(b==null)throw new CtApiException(409,"EXPORT_EXPIRED");
        if(!b.caller.equals(caller)||!b.resource.resource().equals(resource))throw CtApiException.invalid("cursor");
        var items=json.createArrayNode();
        try(var offsets=new RandomAccessFile(b.index.toFile(),"r");var input=new RandomAccessFile(b.data.toFile(),"r")){
            long end=Math.min(b.rows,start+b.limit);
            for(long i=start;i<end;i++){
                offsets.seek(i*8);long from=offsets.readLong();long to=i+1<b.rows?offsets.readLong():input.length();
                if(to-from>Integer.MAX_VALUE)throw new IOException("Record too large");
                byte[] bytes=new byte[(int)(to-from-1)];input.seek(from);input.readFully(bytes);items.add(json.readTree(bytes));
            }
            return b.metadata.page(items,end<b.rows?cursor(id,end,b.key):null);
        }catch(IOException e){remove(id,b);throw new CtApiException(503,"SOURCE_UNAVAILABLE");}
    }
    private String cursor(UUID id,long offset,byte[] key){
        try{String body=id+":"+offset;var mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(key,"HmacSHA256"));String signature=Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(body.getBytes(StandardCharsets.US_ASCII)));return Base64.getUrlEncoder().withoutPadding().encodeToString((body+":"+signature).getBytes(StandardCharsets.US_ASCII));}
        catch(GeneralSecurityException e){throw new IllegalStateException(e);}
    }
    private void prune(){for(var entry:new ArrayList<>(batches.entrySet()))if(!clock.instant().isBefore(entry.getValue().expires))remove(entry.getKey(),entry.getValue());}
    private void remove(UUID id,Batch b){delete(b.data);delete(b.index);batches.remove(id);slots--;reserved-=b.bytes;}
    private static void delete(Path file){try{Files.deleteIfExists(file);}catch(IOException e){throw new IllegalStateException("Spool cleanup failed",e);}}
    public synchronized int active(){prune();return slots;}
    public synchronized long bytes(){prune();return reserved;}
    @Override public synchronized void close(){if(janitor!=null)janitor.shutdownNow();try{for(var e:new ArrayList<>(batches.entrySet()))remove(e.getKey(),e.getValue());}finally{try{directoryLock.release();lockChannel.close();}catch(IOException e){throw new IllegalStateException(e);}}}
}
