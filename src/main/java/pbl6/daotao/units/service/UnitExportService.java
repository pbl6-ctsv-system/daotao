package pbl6.daotao.units.service;
import pbl6.daotao.units.dto.*;
import pbl6.daotao.units.repository.UnitChangeTrackingRepository;
/** Complete materialized exports or immutable cursor pages. */
public class UnitExportService {
    private final UnitChangeTrackingRepository repository;
    private final UnitExportStore store;
    public UnitExportService(){this.repository=null;this.store=null;}
    public UnitExportService(UnitChangeTrackingRepository repository,UnitExportStore store){this.repository=repository;this.store=store;}
    public UnitExportResponse export(UnitExportRequest request,String caller) {
        if(repository==null||store==null)throw new UnitApiException(503,"SYNC_NOT_READY",null);
        if(caller==null)throw new UnitApiException(401,"INVALID_SERVICE_KEY",null);
        if(request.cursor()!=null)return store.page(request.cursor(),caller);
        try(var batch=store.reserve(request,caller)) {
            repository.read(request,batch::start,batch::append);
            return batch.publish();
        }
    }
}

