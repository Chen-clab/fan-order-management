package com.fan.web.service.storage;

import java.io.ByteArrayInputStream;
import java.util.UUID;
import io.minio.*;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.fan.common.exception.ServiceException;

/** Private S3-compatible object storage. The development bucket must not be used for real data. */
@Service
@ConditionalOnProperty(name="fan.storage.enabled",havingValue="true")
public class PrivateObjectStorage {
 private static final Logger log=LoggerFactory.getLogger(PrivateObjectStorage.class);
 private final MinioClient client;private final String bucket;
 public PrivateObjectStorage(@Value("${fan.storage.endpoint}") String endpoint,@Value("${fan.storage.access-key}") String key,@Value("${fan.storage.secret-key}") String secret,@Value("${fan.storage.bucket:fan-private}") String bucket){this.client=MinioClient.builder().endpoint(endpoint).credentials(key,secret).build();this.bucket=bucket;}
 @PostConstruct public void initialize(){try{if(!client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build()))client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());}catch(Exception e){throw new IllegalStateException("Private object storage is unavailable",e);}}
 public String put(byte[] data,String contentType){
  String object="assets/"+UUID.randomUUID();
  try{client.putObject(PutObjectArgs.builder().bucket(bucket).object(object).stream(new ByteArrayInputStream(data),data.length,-1).contentType(contentType).build());}
  catch(Exception e){throw new ServiceException("文件存储失败");}
  if(TransactionSynchronizationManager.isSynchronizationActive())TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void afterCompletion(int status){if(status!=STATUS_COMMITTED)removeQuietly(object);}});
  return object;
 }
 public byte[] get(String object){try(var stream=client.getObject(GetObjectArgs.builder().bucket(bucket).object(object).build())){return stream.readAllBytes();}catch(Exception e){throw new ServiceException("文件读取失败");}}
 private void removeQuietly(String object){try{client.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(object).build());}catch(Exception e){log.warn("Could not remove orphaned asset after rollback");}}
}
