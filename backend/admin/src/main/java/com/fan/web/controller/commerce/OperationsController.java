package com.fan.web.controller.commerce;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.fan.common.core.domain.AjaxResult;
import com.fan.common.utils.SecurityUtils;

/** Administration lists are restricted to the system administrator until data scopes are implemented. */
@RestController
@RequestMapping("/fan/ops")
@PreAuthorize("@ss.hasRole('admin')")
public class OperationsController {
 private final JdbcTemplate db;
 public OperationsController(JdbcTemplate db){this.db=db;}
 @GetMapping("/merchants") public AjaxResult merchants(){return AjaxResult.success(db.queryForList("SELECT m.id,m.name,m.contact_name,m.contact_phone,m.qualification_status,m.version,(SELECT COUNT(*) FROM fan_merchant_document d WHERE d.merchant_id=m.id) AS document_count FROM fan_merchant m ORDER BY m.id DESC LIMIT 200"));}
 @GetMapping("/products") public AjaxResult products(){return AjaxResult.success(db.queryForList("SELECT p.id,p.merchant_id,m.name merchant_name,p.name,p.description,p.status,p.version FROM fan_product p JOIN fan_merchant m ON m.id=p.merchant_id ORDER BY p.id DESC LIMIT 200"));}
 @GetMapping("/orders") public AjaxResult orders(){return AjaxResult.success(db.queryForList("SELECT o.id,o.order_no,o.merchant_id,m.name merchant_name,o.status,o.agreed_publish_count,o.quoted_fee,o.currency,o.version,o.created_at FROM fan_cooperation_order o JOIN fan_merchant m ON m.id=o.merchant_id ORDER BY o.id DESC LIMIT 200"));}
 @GetMapping("/orders/{id}/items") public AjaxResult items(@PathVariable long id){return AjaxResult.success(db.queryForList("SELECT i.id,i.product_id,i.product_name_snapshot,i.commission_rate,l.id link_id,l.url FROM fan_order_item i JOIN fan_product_link l ON l.product_id=i.product_id WHERE i.order_id=? ORDER BY i.id,l.id LIMIT 200",id));}
 @GetMapping("/orders/{id}/videos") public AjaxResult videos(@PathVariable long id){return AjaxResult.success(db.queryForList("SELECT v.id,v.order_item_id,v.external_video_id,v.link_snapshot,i.product_name_snapshot,(SELECT COUNT(*) FROM fan_video_publish p WHERE p.video_id=v.id AND p.status='PUBLISHED') published_count FROM fan_video v JOIN fan_order_item i ON i.id=v.order_item_id WHERE i.order_id=? ORDER BY v.id DESC LIMIT 200",id));}
 @GetMapping("/imports") public AjaxResult imports(){return AjaxResult.success(db.queryForList("SELECT id,source,snapshot_at,state,total_rows,version,error_message,created_at FROM fan_async_import WHERE requested_by=? ORDER BY id DESC LIMIT 100",SecurityUtils.getUserId()));}
 @GetMapping("/settlements") public AjaxResult settlements(){return AjaxResult.success(db.queryForList("SELECT s.id,s.order_id,o.order_no,s.source,s.final_gmv,s.commission,s.service_fee,s.receivable,s.received,s.state,s.version FROM fan_settlement s JOIN fan_cooperation_order o ON o.id=s.order_id ORDER BY s.id DESC LIMIT 200"));}
}
