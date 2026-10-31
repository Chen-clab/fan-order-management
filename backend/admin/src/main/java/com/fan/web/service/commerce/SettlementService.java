package com.fan.web.service.commerce;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fan.common.exception.ServiceException;

/** Records settlement amounts only; never initiates a payment. */
@Service
public class SettlementService {
 private final JdbcTemplate jdbc;private final OrderSummaryService summaries;
 public SettlementService(JdbcTemplate jdbc,OrderSummaryService summaries){this.jdbc=jdbc;this.summaries=summaries;}
 public record Draft(String source,LocalDateTime snapshotAt){}
 public record Receipt(BigDecimal amount,String reference,String note,long version){}
 @Transactional(rollbackFor=Exception.class)
 public long create(long orderId,long actor,Draft input){
  if(input.source()==null||!input.source().matches("[A-Za-z0-9_-]{1,80}")||input.snapshotAt()==null)throw new ServiceException("统计来源或截止时间无效");
  var orders=jdbc.queryForList("SELECT o.quoted_fee,o.agreed_publish_count FROM fan_cooperation_order o JOIN fan_merchant m ON m.id=o.merchant_id WHERE o.id=? AND o.status IN ('IN_PROGRESS','PENDING_SETTLEMENT') AND m.qualification_status='APPROVED' FOR UPDATE",orderId);
  if(orders.size()!=1)throw new ServiceException("订单状态或商家资质不允许结算");
  Long already=jdbc.queryForObject("SELECT COUNT(*) FROM fan_settlement WHERE order_id=?",Long.class,orderId);if(already!=null&&already>0)throw new ServiceException("订单已有结算记录");
  Long pending=jdbc.queryForObject("SELECT COUNT(*) FROM fan_fee_adjustment WHERE order_id=? AND status='PENDING'",Long.class,orderId);if(pending!=null&&pending>0)throw new ServiceException("有待商家确认的费用调整");
  var adjustments=jdbc.queryForList("SELECT actual_fee FROM fan_fee_adjustment WHERE order_id=? AND status='CONFIRMED' ORDER BY id DESC LIMIT 1",BigDecimal.class,orderId);
  Map<String,Object> summary=summaries.summary(orderId,input.source(),input.snapshotAt());
  @SuppressWarnings("unchecked") var items=(List<Map<String,Object>>)summary.get("items");
  long videos=items.stream().mapToLong(i->((Number)i.get("video_count")).longValue()).sum();
  if(videos==0||!Boolean.TRUE.equals(summary.get("complete")))throw new ServiceException("视频 GMV 数据不完整，不能建立结算");
  BigDecimal fee;
  if(!adjustments.isEmpty())fee=adjustments.get(0);
  else {long published=((Number)summary.get("publishedCount")).longValue(),agreed=((Number)orders.get(0).get("agreed_publish_count")).longValue();if(published<agreed)throw new ServiceException("发布次数未达约定，请先提交费用调整并由商家确认");fee=(BigDecimal)orders.get(0).get("quoted_fee");}
  BigDecimal gmv=(BigDecimal)summary.get("knownGmv"),commission=(BigDecimal)summary.get("knownCommission"),receivable=fee.add(commission);
  if(receivable.precision()-receivable.scale()>16)throw new ServiceException("结算金额过大");
  KeyHolder key=new GeneratedKeyHolder();jdbc.update(c->{PreparedStatement ps=c.prepareStatement("INSERT INTO fan_settlement(order_id,source,snapshot_at,currency,final_gmv,commission,service_fee,receivable,created_by) VALUES (?,?,?,'USD',?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS);ps.setLong(1,orderId);ps.setString(2,input.source());ps.setObject(3,input.snapshotAt());ps.setBigDecimal(4,gmv);ps.setBigDecimal(5,commission);ps.setBigDecimal(6,fee);ps.setBigDecimal(7,receivable);ps.setLong(8,actor);return ps;},key);
  if(key.getKey()==null)throw new ServiceException("创建结算失败");return key.getKey().longValue();
 }
 @Transactional
 public void approve(long id,long actor,long version){int n=jdbc.update("UPDATE fan_settlement SET state=IF(receivable=0,'SETTLED','OPEN'),approved_by=?,approved_at=UTC_TIMESTAMP(3),version=version+1 WHERE id=? AND state='DRAFT' AND version=?",actor,id,version);if(n!=1)throw new ServiceException("结算状态或版本不匹配");}
 @Transactional
 public void recordReceipt(long id,long actor,Receipt input){
  if(input.amount()==null||input.amount().signum()<=0||input.amount().scale()>2||input.reference()==null||input.reference().isBlank()||input.reference().length()>100||(input.note()!=null&&input.note().length()>1000))throw new ServiceException("收款金额或凭证编号无效");
  var rows=jdbc.queryForList("SELECT receivable,received,state,version FROM fan_settlement WHERE id=? FOR UPDATE",id);if(rows.size()!=1)throw new ServiceException("结算不存在");var row=rows.get(0);String state=(String)row.get("state");
  if(!("OPEN".equals(state)||"PARTIAL".equals(state))||((Number)row.get("version")).longValue()!=input.version())throw new ServiceException("结算状态或版本不匹配");
  BigDecimal total=(BigDecimal)row.get("receivable"),next=((BigDecimal)row.get("received")).add(input.amount());if(next.compareTo(total)>0)throw new ServiceException("收款金额超过未收金额");
  jdbc.update("INSERT INTO fan_settlement_receipt(settlement_id,reference_no,amount,note,recorded_by) VALUES (?,?,?,?,?)",id,input.reference().trim(),input.amount(),input.note(),actor);
  jdbc.update("UPDATE fan_settlement SET received=?,state=?,version=version+1 WHERE id=?",next,next.compareTo(total)==0?"SETTLED":"PARTIAL",id);
 }
 public Map<String,Object> detail(long id){var rows=jdbc.queryForList("SELECT id,order_id,source,snapshot_at,currency,final_gmv,commission,service_fee,receivable,received,state,version,created_by,approved_by,approved_at FROM fan_settlement WHERE id=?",id);if(rows.size()!=1)throw new ServiceException("结算不存在");var result=new java.util.LinkedHashMap<String,Object>(rows.get(0));result.put("receipts",jdbc.queryForList("SELECT id,reference_no,amount,note,recorded_by,recorded_at FROM fan_settlement_receipt WHERE settlement_id=? ORDER BY id",id));return result;}
 public Map<String,Object> merchantDetail(long userId,long id){Long n=jdbc.queryForObject("SELECT COUNT(*) FROM fan_settlement s JOIN fan_cooperation_order o ON o.id=s.order_id JOIN fan_merchant_user u ON u.merchant_id=o.merchant_id WHERE s.id=? AND u.user_id=?",Long.class,id,userId);if(n==null||n!=1)throw new ServiceException("结算不存在或无权访问");return detail(id);}
}
