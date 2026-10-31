package com.fan.web.controller.commerce;
import java.time.LocalDateTime;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.fan.common.core.domain.AjaxResult;
import com.fan.common.utils.SecurityUtils;
import com.fan.web.service.asyncimport.AsyncImportService;
@RestController
@RequestMapping("/fan/imports/async")
@ConditionalOnProperty(name="fan.import.enabled",havingValue="true")
@PreAuthorize("@ss.hasPermi('fan:gmv:import')")
public class AsyncImportController {
 private final AsyncImportService service;
 public AsyncImportController(AsyncImportService service){this.service=service;}
 @PostMapping public AjaxResult create(@RequestParam String source,@RequestParam LocalDateTime snapshotAt,@RequestParam long sourceVersion,@RequestParam("file") MultipartFile file) throws Exception{return AjaxResult.success(service.create(SecurityUtils.getUserId(),source,snapshotAt,sourceVersion,file));}
 @GetMapping("/{id}") public AjaxResult status(@PathVariable long id){return AjaxResult.success(service.status(SecurityUtils.getUserId(),id));}
 @PostMapping("/{id}/confirm") public AjaxResult confirm(@PathVariable long id,@RequestBody Version request){service.confirm(SecurityUtils.getUserId(),id,request.version());return AjaxResult.success();}
 @PostMapping("/{id}/retry") public AjaxResult retry(@PathVariable long id,@RequestBody Version request){service.retry(SecurityUtils.getUserId(),id,request.version());return AjaxResult.success();}
 public record Version(long version){}
}
