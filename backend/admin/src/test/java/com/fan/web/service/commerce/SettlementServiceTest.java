package com.fan.web.service.commerce;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import com.fan.common.exception.ServiceException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SettlementServiceTest {
 private static final String LOCK="SELECT receivable,received,state,version FROM fan_settlement WHERE id=? FOR UPDATE";
 @Test void rejectsOverpaymentWithoutWriting(){
  JdbcTemplate jdbc=mock(JdbcTemplate.class);when(jdbc.queryForList(LOCK,7L)).thenReturn(List.of(Map.of("receivable",new BigDecimal("100.00"),"received",new BigDecimal("60.00"),"state","PARTIAL","version",2L)));
  var service=new SettlementService(jdbc,mock(OrderSummaryService.class));
  assertThrows(ServiceException.class,()->service.recordReceipt(7,5,new SettlementService.Receipt(new BigDecimal("40.01"),"R1",null,2)));
  verify(jdbc,never()).update(anyString(),any(Object[].class));
 }
 @Test void rejectsStaleVersion(){
  JdbcTemplate jdbc=mock(JdbcTemplate.class);when(jdbc.queryForList(LOCK,7L)).thenReturn(List.of(Map.of("receivable",new BigDecimal("100.00"),"received",BigDecimal.ZERO,"state","OPEN","version",3L)));
  var service=new SettlementService(jdbc,mock(OrderSummaryService.class));
  assertThrows(ServiceException.class,()->service.recordReceipt(7,5,new SettlementService.Receipt(new BigDecimal("20.00"),"R1",null,2)));
  verify(jdbc,never()).update(anyString(),any(Object[].class));
 }
 @Test void recordsFinalReceiptAndMarksSettled(){
  JdbcTemplate jdbc=mock(JdbcTemplate.class);when(jdbc.queryForList(LOCK,7L)).thenReturn(List.of(Map.of("receivable",new BigDecimal("100.00"),"received",new BigDecimal("60.00"),"state","PARTIAL","version",2L)));
  var service=new SettlementService(jdbc,mock(OrderSummaryService.class));
  service.recordReceipt(7,5,new SettlementService.Receipt(new BigDecimal("40.00"),"R1","mock",2));
  verify(jdbc).update("UPDATE fan_settlement SET received=?,state=?,version=version+1 WHERE id=?",new BigDecimal("100.00"),"SETTLED",7L);
 }
}
