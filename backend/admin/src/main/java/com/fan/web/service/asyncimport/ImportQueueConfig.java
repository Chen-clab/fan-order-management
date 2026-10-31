package com.fan.web.service.asyncimport;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
@EnableRabbit
@EnableScheduling
@ConditionalOnProperty(name="fan.import.enabled",havingValue="true")
class ImportQueueConfig {
 static final String EXCHANGE="fan.import.v1", QUEUE="fan.import.v1", ROUTING="import";
 @Bean DirectExchange fanImportExchange(){return new DirectExchange(EXCHANGE,true,false);}
 @Bean Queue fanImportQueue(){return new Queue(QUEUE,true,false,false);}
 @Bean Binding fanImportBinding(Queue fanImportQueue,DirectExchange fanImportExchange){return BindingBuilder.bind(fanImportQueue).to(fanImportExchange).with(ROUTING);}
}

@Component
@ConditionalOnProperty(name="fan.import.enabled",havingValue="true")
class ImportQueueWorker {
 private static final Logger log=LoggerFactory.getLogger(ImportQueueWorker.class);
 private final JdbcTemplate jdbc;private final RabbitTemplate rabbit;private final TransactionTemplate tx;private final AsyncImportService service;
 ImportQueueWorker(JdbcTemplate jdbc,RabbitTemplate rabbit,TransactionTemplate tx,AsyncImportService service){this.jdbc=jdbc;this.rabbit=rabbit;this.tx=tx;this.service=service;this.rabbit.setMandatory(true);}
 @Scheduled(fixedDelay=1500)
 public void publish(){
  try {tx.executeWithoutResult(s->{
   var rows=jdbc.queryForList("SELECT id,job_id,event_type FROM fan_import_outbox WHERE state='NEW' ORDER BY id LIMIT 1 FOR UPDATE SKIP LOCKED");
   if(rows.isEmpty())return;var row=rows.get(0);long id=((Number)row.get("id")).longValue(),job=((Number)row.get("job_id")).longValue();String type=(String)row.get("event_type");
   CorrelationData cd=new CorrelationData(UUID.randomUUID().toString());
   rabbit.convertAndSend(ImportQueueConfig.EXCHANGE,ImportQueueConfig.ROUTING,job+":"+type,m->{m.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);m.getMessageProperties().setMessageId("fan-outbox-"+id);return m;},cd);
   try {var confirm=cd.getFuture().get(5,TimeUnit.SECONDS);if(!confirm.isAck()||cd.getReturned()!=null)throw new IllegalStateException("message not accepted or routed");}catch(Exception e){throw new IllegalStateException("message confirmation failed",e);}
   jdbc.update("UPDATE fan_import_outbox SET state='SENT',sent_at=UTC_TIMESTAMP(3) WHERE id=? AND state='NEW'",id);
  });}catch(Exception e){log.warn("Import outbox send failed; it will be retried: {}",e.getClass().getSimpleName());}
 }
 @RabbitListener(queues=ImportQueueConfig.QUEUE, concurrency="1")
 public void receive(String message){
  String[] parts=message.split(":",-1);if(parts.length!=2||(!"PREVIEW".equals(parts[1])&&!"IMPORT".equals(parts[1]))) {log.warn("Invalid import message");return;}
  long id;try{id=Long.parseLong(parts[0]);}catch(NumberFormatException e){log.warn("Invalid import job ID");return;}
  try{service.process(id,parts[1]);}catch(Exception e){log.warn("Import job {} failed: {}",id,e.getClass().getSimpleName());service.fail(id,parts[1],e);}
 }
}
