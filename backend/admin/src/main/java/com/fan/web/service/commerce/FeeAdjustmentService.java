package com.fan.web.service.commerce;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fan.common.exception.ServiceException;

@Service
public class FeeAdjustmentService {
    private final JdbcTemplate jdbc;
    public FeeAdjustmentService(JdbcTemplate jdbc){this.jdbc=jdbc;}
    public record Proposal(BigDecimal actualFee,String reason){}
    @Transactional
    public long submit(long orderId,long userId,Proposal p){
        if(p.actualFee()==null || p.actualFee().signum()<0 || p.actualFee().scale()>2 || p.reason()==null || p.reason().isBlank() || p.reason().length()>1000) throw new ServiceException("实际费用或调整原因无效");
        var orders=jdbc.queryForList("SELECT o.id FROM fan_cooperation_order o JOIN fan_merchant m ON m.id=o.merchant_id WHERE o.id=? AND o.status IN ('IN_PROGRESS','PENDING_SETTLEMENT') AND m.qualification_status='APPROVED' FOR UPDATE",orderId);
        if(orders.size()!=1)throw new ServiceException("订单状态或商家资质不允许调整费用");
        Long settled=jdbc.queryForObject("SELECT COUNT(*) FROM fan_settlement WHERE order_id=?",Long.class,orderId);
        if(settled!=null && settled>0)throw new ServiceException("已有结算记录，暂不支持更改费用");
        Long pending=jdbc.queryForObject("SELECT COUNT(*) FROM fan_fee_adjustment WHERE order_id=? AND status='PENDING'",Long.class,orderId);
        if(pending!=null && pending>0)throw new ServiceException("已有待商家确认的调整");
        KeyHolder key=new GeneratedKeyHolder();
        jdbc.update(c->{PreparedStatement ps=c.prepareStatement("INSERT INTO fan_fee_adjustment(order_id,actual_fee,reason,created_by) VALUES (?,?,?,?)",Statement.RETURN_GENERATED_KEYS);ps.setLong(1,orderId);ps.setBigDecimal(2,p.actualFee());ps.setString(3,p.reason().trim());ps.setLong(4,userId);return ps;},key);
        if(key.getKey()==null)throw new ServiceException("创建调整失败");return key.getKey().longValue();
    }
    public java.util.List<java.util.Map<String,Object>> ownPending(long userId) {
        return jdbc.queryForList("SELECT a.id,a.order_id,o.order_no,a.actual_fee,a.reason,a.version FROM fan_fee_adjustment a JOIN fan_cooperation_order o ON o.id=a.order_id JOIN fan_merchant m ON m.id=o.merchant_id JOIN fan_merchant_user u ON u.merchant_id=m.id WHERE u.user_id=? AND m.qualification_status='APPROVED' AND a.status='PENDING' ORDER BY a.id DESC LIMIT 200",userId);
    }
    @Transactional
    public void decide(long id,long userId,long version,boolean approve,String note){
        if(!approve && (note==null || note.isBlank() || note.length()>1000))throw new ServiceException("拒绝时需填写原因");
        if(note!=null && note.length()>1000)throw new ServiceException("备注过长");
        int n=jdbc.update("""
          UPDATE fan_fee_adjustment a
          JOIN fan_cooperation_order o ON o.id=a.order_id
          JOIN fan_merchant m ON m.id=o.merchant_id
          JOIN fan_merchant_user u ON u.merchant_id=m.id AND u.user_id=?
          SET a.status=?,a.decided_by=?,a.decided_at=UTC_TIMESTAMP(3),a.decision_note=?,a.version=a.version+1
          WHERE a.id=? AND a.status='PENDING' AND a.version=? AND m.qualification_status='APPROVED'
          """,userId,approve?"CONFIRMED":"REJECTED",userId,note,id,version);
        if(n!=1)throw new ServiceException("无法确认费用：权限、资质、状态或版本不匹配");
    }
}
