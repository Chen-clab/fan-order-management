package com.fan.web.controller.commerce;

import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.fan.common.annotation.Anonymous;
import com.fan.common.core.domain.AjaxResult;
import com.fan.common.utils.SecurityUtils;
import com.fan.web.service.commerce.MerchantOnboardingService;
import com.fan.web.service.commerce.MerchantOnboardingService.MerchantApplication;

@RestController
@RequestMapping("/fan/merchant")
public class MerchantOnboardingController {
    private final MerchantOnboardingService service;
    public MerchantOnboardingController(MerchantOnboardingService service) { this.service = service; }

    @Anonymous
    @PostMapping("/register")
    public AjaxResult register(@RequestBody MerchantApplication request) {
        service.register(request);
        return AjaxResult.success("注册成功，请等待资质审核");
    }

    @GetMapping("/me")
    public AjaxResult me() { return AjaxResult.success(service.ownProfile(SecurityUtils.getUserId())); }

    @PostMapping("/me/qualification")
    public AjaxResult uploadQualification(@RequestParam("file") org.springframework.web.multipart.MultipartFile file) throws java.io.IOException {
        service.uploadQualification(SecurityUtils.getUserId(), file);
        return AjaxResult.success("资质材料已提交，请等待审核");
    }

    @PreAuthorize("@ss.hasPermi('fan:merchant:review')")
    @GetMapping("/{id}/qualification")
    public org.springframework.http.ResponseEntity<byte[]> qualification(@PathVariable long id) {
        return org.springframework.http.ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=qualification")
            .header("X-Content-Type-Options", "nosniff")
            .contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)
            .body(service.qualificationForReview(id));
    }

    @PreAuthorize("@ss.hasPermi('fan:merchant:review')")
    @PostMapping("/{id}/review")
    public AjaxResult review(@PathVariable long id, @RequestBody ReviewRequest request) {
        service.review(id, request.version(), request.approved(), SecurityUtils.getUserId(), request.reason());
        return AjaxResult.success();
    }

    public record ReviewRequest(long version, boolean approved, String reason) { }
}
