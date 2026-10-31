package com.fan.web.controller.commerce;
import java.time.LocalDateTime;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.fan.common.core.domain.AjaxResult;
import com.fan.common.utils.SecurityUtils;
import com.fan.web.service.commerce.VideoGmvImportService;
@RestController
@RequestMapping("/fan/imports")
public class VideoGmvImportController {
 private final VideoGmvImportService service;
 public VideoGmvImportController(VideoGmvImportService service){this.service=service;}
 @PreAuthorize("@ss.hasPermi('fan:gmv:import')") @PostMapping("/video-gmv")
 public AjaxResult upload(@RequestParam String source,@RequestParam LocalDateTime snapshotAt,@RequestParam long sourceVersion,@RequestParam("file") MultipartFile file) throws Exception {
  return AjaxResult.success(service.importFile(SecurityUtils.getUserId(),source,snapshotAt,sourceVersion,file));
 }
}
