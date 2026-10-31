package com.fan.web.controller.commerce;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.fan.common.core.domain.AjaxResult;
import com.fan.common.utils.SecurityUtils;
import com.fan.web.service.commerce.FeeAdjustmentService;
@RestController
@RequestMapping("/fan")
public class FeeAdjustmentController {
 private final FeeAdjustmentService service;
 public FeeAdjustmentController(FeeAdjustmentService service){this.service=service;}
 @GetMapping("/fee-adjustments/mine") public AjaxResult mine(){return AjaxResult.success(service.ownPending(SecurityUtils.getUserId()));}
 @PreAuthorize("@ss.hasPermi('fan:fee:adjust')")
 @PostMapping("/orders/{id}/fee-adjustments")
 public AjaxResult submit(@PathVariable long id,@RequestBody FeeAdjustmentService.Proposal proposal){return AjaxResult.success(service.submit(id,SecurityUtils.getUserId(),proposal));}
 @PostMapping("/fee-adjustments/{id}/decision")
 public AjaxResult decide(@PathVariable long id,@RequestBody Decision decision){service.decide(id,SecurityUtils.getUserId(),decision.version(),decision.approve(),decision.note());return AjaxResult.success();}
 public record Decision(long version,boolean approve,String note){}
}
