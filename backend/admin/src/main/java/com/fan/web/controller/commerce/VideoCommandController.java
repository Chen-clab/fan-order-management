package com.fan.web.controller.commerce;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.fan.common.core.domain.AjaxResult;
import com.fan.web.service.commerce.VideoCommandService;
@RestController
@RequestMapping("/fan/videos")
public class VideoCommandController {
    private final VideoCommandService service;
    public VideoCommandController(VideoCommandService service){this.service=service;}
    @PreAuthorize("@ss.hasPermi('fan:video:add')")
    @PostMapping public AjaxResult create(@RequestBody VideoCommandService.CreateVideo request){return AjaxResult.success(service.create(request));}
    @PreAuthorize("@ss.hasPermi('fan:video:publish')")
    @PostMapping("/{id}/publications") public AjaxResult publish(@PathVariable long id,@RequestBody VideoCommandService.PublishVideo request){return AjaxResult.success(service.publish(id,request));}
}
