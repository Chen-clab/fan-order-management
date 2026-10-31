package com.fan.web.controller.commerce;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.fan.common.core.domain.AjaxResult;
import com.fan.common.utils.SecurityUtils;
import com.fan.web.service.commerce.ProductService;
@RestController
@RequestMapping("/fan/products")
public class ProductController {
 private final ProductService service;
 public ProductController(ProductService service){this.service=service;}
 @GetMapping("/mine") public AjaxResult mine(){return AjaxResult.success(service.own(SecurityUtils.getUserId()));}
 @PostMapping public AjaxResult create(@RequestBody ProductService.ProductInput in){return AjaxResult.success(service.create(SecurityUtils.getUserId(),in));}
 @PostMapping("/{id}/assets") public AjaxResult upload(@PathVariable long id,@RequestParam("file") MultipartFile file) throws java.io.IOException {service.upload(SecurityUtils.getUserId(),id,file);return AjaxResult.success();}
 @PostMapping("/{id}/submit") public AjaxResult submit(@PathVariable long id,@RequestBody Version v){service.submit(SecurityUtils.getUserId(),id,v.version());return AjaxResult.success();}
 @PreAuthorize("@ss.hasPermi('fan:product:review')") @PostMapping("/{id}/review")
 public AjaxResult review(@PathVariable long id,@RequestBody Review r){service.review(id,SecurityUtils.getUserId(),r.version(),r.approved(),r.reason());return AjaxResult.success();}
 @GetMapping("/{id}/assets") public AjaxResult assets(@PathVariable long id){return AjaxResult.success(service.assetsForOwner(SecurityUtils.getUserId(),id));}
 @PreAuthorize("@ss.hasPermi('fan:product:review')") @GetMapping("/review/{id}/assets") public AjaxResult reviewAssets(@PathVariable long id){return AjaxResult.success(service.assetsForReview(id));}
 @PreAuthorize("@ss.hasPermi('fan:product:review')") @GetMapping("/review/assets/{id}") public org.springframework.http.ResponseEntity<byte[]> reviewAsset(@PathVariable long id){return org.springframework.http.ResponseEntity.ok().header("Content-Disposition","attachment; filename=product-asset").header("X-Content-Type-Options","nosniff").contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM).body(service.assetForReview(id));}
 @GetMapping("/assets/{id}") public org.springframework.http.ResponseEntity<byte[]> asset(@PathVariable long id){return org.springframework.http.ResponseEntity.ok().header("Content-Disposition","attachment; filename=product-asset").header("X-Content-Type-Options","nosniff").contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM).body(service.assetForOwner(SecurityUtils.getUserId(),id));}
 public record Version(long version){} public record Review(long version,boolean approved,String reason){}
}
