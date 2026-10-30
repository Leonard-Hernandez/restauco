package com.lsoftware.restauco.infrastructure.report.controller;

import com.lsoftware.restauco.entity.report.model.ReportPeriod;
import com.lsoftware.restauco.infrastructure.report.dto.SalesReportResponse;
import com.lsoftware.restauco.usecase.report.GetSalesReportUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/panel/admin/reports")
@RequiredArgsConstructor
@Tag(name = "Reports (owner)", description = "Sales and kitchen statistics")
@SecurityRequirement(name = "bearerAuth")
public class AdminReportController {

    private final GetSalesReportUseCase salesReport;

    @GetMapping("/sales")
    @Operation(summary = "Sales, best sellers and kitchen times: today, last 7 or last 30 days")
    public SalesReportResponse sales(@RequestParam(defaultValue = "TODAY") ReportPeriod period) {
        return SalesReportResponse.from(salesReport.execute(period));
    }
}
