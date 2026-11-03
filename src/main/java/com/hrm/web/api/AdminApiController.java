package com.hrm.web.api;

import com.hrm.service.AuditService;
import com.hrm.service.DataQualityService;
import com.hrm.web.BaseController;
import com.hrm.web.Router;

public class AdminApiController extends BaseController {

    private static final long serialVersionUID = 1L;

    private final transient DataQualityService dataQuality;
    private final transient AuditService audit;

    public AdminApiController(DataQualityService dataQuality, AuditService audit) {
        this.dataQuality = dataQuality;
        this.audit = audit;
    }

    @Override
    protected void routes(Router r) {
        r.get("/data-validation", ctx -> ctx.json(dataQuality.run()))
         .get("/audit-logs", ctx -> ctx.json(audit.search(ctx.param("keyword"), ctx.param("action"),
                 ctx.dateParam("from"), ctx.dateParam("to"), ctx.pageRequest(20))));
    }
}
