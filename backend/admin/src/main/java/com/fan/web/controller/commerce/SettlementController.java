package com.fan.web.controller.commerce;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.fan.common.core.domain.AjaxResult;
import com.fan.common.utils.SecurityUtils;
import com.fan.web.service.commerce.SettlementService;
@RestController
@RequestMapping("/fan/settlements")
public class SettlementController {
 private final SettlementService service;
 public SettlementController(SettlementService service){this.service=service;}
 @PreAuthorize("@ss.hasPermi('fan:settlement:add')") @PostMapping("/order/{orderId}") public AjaxResult create(@PathVariable long orderId,@RequestBody SettlementService.Draft d){return AjaxResult.success(service.create(orderId,SecurityUtils.getUserId(),d));}
 @PreAuthorize("@ss.hasPermi('fan:settlement:review')") @PostMapping("/{id}/approve") public AjaxResult approve(@PathVariable long id,@RequestBody Version v){service.approve(id,SecurityUtils.getUserId(),v.version());return AjaxResult.success();}
 @PreAuthorize("@ss.hasPermi('fan:settlement:receipt')") @PostMapping("/{id}/receipts") public AjaxResult receipt(@PathVariable long id,@RequestBody SettlementService.Receipt r){service.recordReceipt(id,SecurityUtils.getUserId(),r);return AjaxResult.success();}
 @PreAuthorize("@ss.hasPermi('fan:settlement:query')") @GetMapping("/{id}") public AjaxResult detail(@PathVariable long id){return AjaxResult.success(service.detail(id));}
 @GetMapping("/{id}/mine") public AjaxResult mine(@PathVariable long id){return AjaxResult.success(service.merchantDetail(SecurityUtils.getUserId(),id));}
 public record Version(long version){}
}
