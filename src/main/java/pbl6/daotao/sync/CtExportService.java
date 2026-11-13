package pbl6.daotao.sync;
import java.util.*;import com.fasterxml.jackson.databind.node.ObjectNode;
public final class CtExportService {
 private final Map<String,CtBinding> bindings;private final CtSnapshotReader reader;private final DiskExportStore store;private final UUID epoch;
 public CtExportService(List<CtBinding> bindings,CtSnapshotReader reader,DiskExportStore store,UUID epoch){var map=new LinkedHashMap<String,CtBinding>();for(var b:bindings)if(map.put(b.resource().url(),b)!=null)throw new IllegalArgumentException("Duplicate active URL");this.bindings=Map.copyOf(map);this.reader=reader;this.store=store;this.epoch=epoch;}
 public CtBinding binding(String path){return bindings.get(path);}
 public ObjectNode export(String path,Map<String,String[]> query,boolean body,String caller){
  var b=bindings.get(path);if(b==null)throw new IllegalArgumentException("No implemented resource binding");var request=CtRequest.parse(query,body,b.resource().profile());
  if(request.cursor()!=null)return store.next(request.cursor(),caller,b.resource().resource(),epoch);
  return store.create(b.resource(),caller,request,(meta,sink)->reader.read(b,request,meta,sink));
 }
}
