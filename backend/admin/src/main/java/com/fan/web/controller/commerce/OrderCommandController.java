package com.fan.web.controller.commerce;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.fan.common.core.domain.AjaxResult;
import com.fan.common.utils.SecurityUtils;
import com.fan.web.service.commerce.OrderCommandService;

@RestController
@RequestMapping("/fan/orders")
public class OrderCommandController {
    private final OrderCommandService service;
    public OrderCommandController(OrderCommandService service) { this.service=service; }
    @GetMapping("/mine")
    public AjaxResult mine() { return AjaxResult.success(service.ownOrders(SecurityUtils.getUserId())); }
    @GetMapping("/{id}/mine")
    public AjaxResult detail(@PathVariable long id) { return AjaxResult.success(service.ownDetail(SecurityUtils.getUserId(),id)); }
    @PreAuthorize("@ss.hasPermi('fan:order:add')")
    @PostMapping
    public AjaxResult create(@RequestBody OrderCommandService.CreateOrder request) { return AjaxResult.success(service.create(request)); }
    @PostMapping("/{id}/confirm")
    public AjaxResult confirm(@PathVariable long id,@RequestBody ConfirmRequest request) {
        service.confirm(id,SecurityUtils.getUserId(),request.version());
        return AjaxResult.success();
    }
    public record ConfirmRequest(long version) { }
}
