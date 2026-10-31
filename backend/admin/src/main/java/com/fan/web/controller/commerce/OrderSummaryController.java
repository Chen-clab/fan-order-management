package com.fan.web.controller.commerce;

import java.time.LocalDateTime;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.fan.common.core.domain.AjaxResult;
import com.fan.web.service.commerce.OrderSummaryService;

@RestController
@RequestMapping("/fan/orders")
public class OrderSummaryController {
    private final OrderSummaryService service;
    public OrderSummaryController(OrderSummaryService service) { this.service=service; }
    @PreAuthorize("@ss.hasPermi('fan:order:query')")
    @GetMapping("/{id}/summary")
    public AjaxResult summary(@PathVariable long id, @RequestParam String source, @RequestParam LocalDateTime asOf) {
        if (source.isBlank() || source.length()>80) throw new IllegalArgumentException("无效的数据来源");
        return AjaxResult.success(service.summary(id,source,asOf));
    }
}
