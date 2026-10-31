package com.fan.web.service.commerce;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** Summarises the newest cumulative snapshot for each video within one source. */
@Service
public class OrderSummaryService {
    private final JdbcTemplate jdbc;
    public OrderSummaryService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public Map<String,Object> summary(long orderId, String source, LocalDateTime asOf) {
        List<Map<String,Object>> order = jdbc.queryForList("SELECT id,order_no,currency,status,quoted_fee,agreed_publish_count FROM fan_cooperation_order WHERE id=?", orderId);
        if (order.size() != 1) throw new IllegalArgumentException("订单不存在");
        String sql = """
            WITH ranked AS (
              SELECT video_id, final_gmv,
                ROW_NUMBER() OVER (PARTITION BY video_id ORDER BY snapshot_at DESC,source_version DESC,id DESC) AS rn
              FROM fan_video_gmv_snapshot WHERE source=? AND snapshot_at<=?
            )
            SELECT i.id AS item_id,i.product_name_snapshot,i.commission_rate,
              COUNT(v.id) AS video_count,COUNT(r.video_id) AS videos_with_gmv,
              COALESCE(SUM(r.final_gmv),0) AS known_gmv
            FROM fan_order_item i
            LEFT JOIN fan_video v ON v.order_item_id=i.id
            LEFT JOIN ranked r ON r.video_id=v.id AND r.rn=1
            WHERE i.order_id=?
            GROUP BY i.id,i.product_name_snapshot,i.commission_rate ORDER BY i.id
            """;
        List<Map<String,Object>> items=jdbc.queryForList(sql,source,asOf,orderId);
        BigDecimal total=BigDecimal.ZERO, commission=BigDecimal.ZERO;
        boolean complete=true;
        for (Map<String,Object> item:items) {
            BigDecimal amount=(BigDecimal)item.get("known_gmv");
            BigDecimal rate=(BigDecimal)item.get("commission_rate");
            BigDecimal itemCommission=amount.multiply(rate).setScale(2,RoundingMode.HALF_UP);
            item.put("known_commission",itemCommission);
            total=total.add(amount); commission=commission.add(itemCommission);
            complete &= ((Number)item.get("video_count")).longValue()==((Number)item.get("videos_with_gmv")).longValue();
        }
        var count=jdbc.queryForObject("SELECT COUNT(*) FROM fan_video_publish p JOIN fan_video v ON v.id=p.video_id JOIN fan_order_item i ON i.id=v.order_item_id WHERE i.order_id=? AND p.status='PUBLISHED'",Long.class,orderId);
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("order",order.get(0)); result.put("source",source); result.put("asOf",asOf.toString());
        result.put("items",items); result.put("publishedCount",count);
        result.put("knownGmv",total); result.put("knownCommission",commission);
        result.put("complete",complete); result.put("status","待核对");
        return result;
    }
}
