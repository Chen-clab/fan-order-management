package com.fan.web.service.commerce;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fan.common.exception.ServiceException;

@Service
public class OrderCommandService {
    private final JdbcTemplate jdbc;
    public OrderCommandService(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    public record Item(long productId, BigDecimal commissionRate) { }
    public record CreateOrder(long merchantId, int agreedPublishCount, BigDecimal quotedFee, List<Item> items) { }

    @Transactional(rollbackFor=Exception.class)
    public long create(CreateOrder in) {
        if (in.merchantId() <= 0 || in.agreedPublishCount() <= 0 || in.quotedFee()==null || in.quotedFee().signum()<0 || in.quotedFee().scale()>2 || in.items()==null || in.items().isEmpty() || in.items().size()>100) throw new ServiceException("订单商品、发布数量或报价无效");
        var status=jdbc.queryForList("SELECT qualification_status FROM fan_merchant WHERE id=? FOR UPDATE",String.class,in.merchantId());
        if (status.size()!=1 || !"APPROVED".equals(status.get(0))) throw new ServiceException("商家资质尚未审核通过");
        var ids=new java.util.HashSet<Long>();
        for (Item item:in.items()) {
            if (item==null || item.productId()<=0 || !ids.add(item.productId()) || item.commissionRate()==null || item.commissionRate().signum()<0 || item.commissionRate().compareTo(BigDecimal.ONE)>0 || item.commissionRate().scale()>6) throw new ServiceException("商品重复或佣金比例无效");
        }
        KeyHolder key=new GeneratedKeyHolder();
        jdbc.update(c -> {
            PreparedStatement ps=c.prepareStatement("INSERT INTO fan_cooperation_order(order_no,merchant_id,status,agreed_publish_count,quoted_fee,currency) VALUES (?,?,'PENDING_CONFIRMATION',?,?,'USD')",Statement.RETURN_GENERATED_KEYS);
            ps.setString(1,"FAN-"+UUID.randomUUID().toString().replace("-", "")); ps.setLong(2,in.merchantId()); ps.setInt(3,in.agreedPublishCount()); ps.setBigDecimal(4,in.quotedFee()); return ps;
        },key);
        if (key.getKey()==null) throw new ServiceException("创建订单失败");
        long orderId=key.getKey().longValue();
        for (Item item:in.items()) {
            int n=jdbc.update("INSERT INTO fan_order_item(order_id,merchant_id,product_id,product_name_snapshot,commission_rate) SELECT ?,merchant_id,id,name,? FROM fan_product WHERE id=? AND merchant_id=? AND status='APPROVED'",orderId,item.commissionRate(),item.productId(),in.merchantId());
            if (n!=1) throw new ServiceException("商品不存在、未审核或不属于该商家");
        }
        return orderId;
    }
    public java.util.List<java.util.Map<String,Object>> ownOrders(long userId) {
        return jdbc.queryForList("SELECT o.id,o.order_no,o.status,o.agreed_publish_count,o.quoted_fee,o.currency,o.version FROM fan_cooperation_order o JOIN fan_merchant m ON m.id=o.merchant_id JOIN fan_merchant_user u ON u.merchant_id=m.id WHERE u.user_id=? AND m.qualification_status='APPROVED' ORDER BY o.id DESC LIMIT 200",userId);
    }
    public java.util.Map<String,Object> ownDetail(long userId,long orderId) {
        var orders=jdbc.queryForList("SELECT o.id,o.order_no,o.status,o.agreed_publish_count,o.quoted_fee,o.currency,o.version FROM fan_cooperation_order o JOIN fan_merchant m ON m.id=o.merchant_id JOIN fan_merchant_user u ON u.merchant_id=m.id WHERE u.user_id=? AND m.qualification_status='APPROVED' AND o.id=?",userId,orderId);
        if(orders.size()!=1)throw new ServiceException("订单不存在或无权访问");
        var result=new java.util.LinkedHashMap<String,Object>(orders.get(0));
        result.put("items",jdbc.queryForList("SELECT product_name_snapshot,commission_rate FROM fan_order_item WHERE order_id=? ORDER BY id",orderId));
        return result;
    }
    @Transactional
    public void confirm(long orderId,long userId,long version) {
        int n=jdbc.update("""
            UPDATE fan_cooperation_order o
            JOIN fan_merchant m ON m.id=o.merchant_id
            JOIN fan_merchant_user u ON u.merchant_id=m.id AND u.user_id=?
            SET o.status='IN_PROGRESS',o.confirmed_version=o.version,o.confirmed_by=?,o.confirmed_at=UTC_TIMESTAMP(3),o.version=o.version+1
            WHERE o.id=? AND o.status='PENDING_CONFIRMATION' AND o.version=? AND m.qualification_status='APPROVED'
            """,userId,userId,orderId,version);
        if(n!=1) throw new ServiceException("无法确认订单：权限、资质、状态或版本不匹配");
    }
}
