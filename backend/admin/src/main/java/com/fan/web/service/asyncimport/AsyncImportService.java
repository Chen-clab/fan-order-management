package com.fan.web.service.asyncimport;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.fan.common.exception.ServiceException;
import com.fan.web.service.commerce.VideoGmvImportService;

@Service
public class AsyncImportService {
 private final JdbcTemplate jdbc; private final VideoGmvImportService importer; private final ObjectMapper json;
 public AsyncImportService(JdbcTemplate jdbc,VideoGmvImportService importer,ObjectMapper json){this.jdbc=jdbc;this.importer=importer;this.json=json;}
 @Transactional(rollbackFor=Exception.class)
 public long create(long actor,String source,LocalDateTime at,long version,MultipartFile file) throws Exception {
  if(source==null||!source.matches("[A-Za-z0-9_-]{1,80}")||at==null||version<1)throw new ServiceException("来源、时间或版本无效");
  if(file==null||file.isEmpty()||file.getSize()>8*1024*1024||file.getOriginalFilename()==null||!file.getOriginalFilename().toLowerCase(Locale.ROOT).endsWith(".xlsx"))throw new ServiceException("请选择小于 8MB 的 XLSX 文件");
  byte[] bytes=file.getBytes();if(bytes.length<4||bytes[0]!='P'||bytes[1]!='K')throw new ServiceException("文件格式无效");
  String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
  KeyHolder key=new GeneratedKeyHolder();
  jdbc.update(c->{PreparedStatement p=c.prepareStatement("INSERT INTO fan_async_import(source,snapshot_at,source_version,requested_by,file_name,file_data,file_sha256,state) VALUES (?,?,?,?,?,?,?,'PREVIEW_QUEUED')",Statement.RETURN_GENERATED_KEYS);p.setString(1,source);p.setObject(2,at);p.setLong(3,version);p.setLong(4,actor);p.setString(5,"input.xlsx");p.setBytes(6,bytes);p.setString(7,hash);return p;},key);
  if(key.getKey()==null)throw new ServiceException("创建任务失败");long id=key.getKey().longValue();
  jdbc.update("INSERT INTO fan_import_outbox(job_id,event_type) VALUES (?,'PREVIEW')",id);return id;
 }
 public Map<String,Object> status(long actor,long jobId){var r=jdbc.queryForList("SELECT id,source,snapshot_at,source_version,state,total_rows,preview_json,result_json,error_message,version,created_at,updated_at FROM fan_async_import WHERE id=? AND requested_by=?",jobId,actor);if(r.size()!=1)throw new ServiceException("任务不存在或无权访问");return r.get(0);}
 @Transactional
 public void confirm(long actor,long id,long version){
  int n=jdbc.update("UPDATE fan_async_import SET state='IMPORT_QUEUED',version=version+1 WHERE id=? AND requested_by=? AND state='PREVIEW_READY' AND version=?",id,actor,version);
  if(n!=1)throw new ServiceException("任务状态或版本不匹配");
  jdbc.update("INSERT INTO fan_import_outbox(job_id,event_type) VALUES (?,'IMPORT')",id);
 }
 @Transactional
 public void retry(long actor,long id,long version){
  var jobs=jdbc.queryForList("SELECT state,version,preview_json FROM fan_async_import WHERE id=? AND requested_by=? FOR UPDATE",id,actor);
  if(jobs.size()!=1||!"FAILED".equals(jobs.get(0).get("state"))||((Number)jobs.get(0).get("version")).longValue()!=version)throw new ServiceException("任务状态或版本不匹配");
  String kind=jobs.get(0).get("preview_json")==null?"PREVIEW":"IMPORT";
  int outbox=jdbc.update("UPDATE fan_import_outbox SET state='NEW',sent_at=NULL WHERE job_id=? AND event_type=?",id,kind);
  if(outbox!=1)throw new ServiceException("任务消息记录不存在");
  jdbc.update("UPDATE fan_async_import SET state=?,error_message=NULL,version=version+1 WHERE id=?",kind.equals("PREVIEW")?"PREVIEW_QUEUED":"IMPORT_QUEUED",id);
 }
 @Transactional(rollbackFor=Exception.class)
 public void process(long id,String kind) throws Exception {
  var jobs=jdbc.queryForList("SELECT * FROM fan_async_import WHERE id=? FOR UPDATE",id);if(jobs.size()!=1)return;var job=jobs.get(0);String state=(String)job.get("state");
  if("PREVIEW".equals(kind)&&"PREVIEW_QUEUED".equals(state)){
   byte[] bytes=(byte[])job.get("file_data");List<Map<String,Object>> sample=new ArrayList<>();Set<String> seen=new HashSet<>();int count=0;
   try(Workbook book=WorkbookFactory.create(new ByteArrayInputStream(bytes))){
    Sheet sheet=book.getSheetAt(0);if(sheet.getLastRowNum()>10000)throw new ServiceException("单次最多导入 10000 行");Row header=sheet.getRow(0);
    if(header==null||!"视频编号".equals(text(header.getCell(0)))||!"最终GMV(USD)".equals(text(header.getCell(1))))throw new ServiceException("表头必须为 视频编号 和 最终GMV(USD)");
    for(int i=1;i<=sheet.getLastRowNum();i++){Row row=sheet.getRow(i);if(row==null)continue;Cell a=row.getCell(0),b=row.getCell(1);if((a==null||a.getCellType()==CellType.BLANK)&&(b==null||b.getCellType()==CellType.BLANK))continue;
     String video=text(a);if(video.isBlank()||video.length()>160||b==null||b.getCellType()!=CellType.NUMERIC)throw new ServiceException("第 "+(i+1)+" 行数据无效");
     if(!seen.add(video))throw new ServiceException("第 "+(i+1)+" 行视频编号重复");BigDecimal amount=BigDecimal.valueOf(b.getNumericCellValue()).stripTrailingZeros();
     if(amount.signum()<0||amount.scale()>2||amount.precision()-amount.scale()>16)throw new ServiceException("第 "+(i+1)+" 行 GMV 无效");
     var matched=jdbc.queryForList("SELECT id FROM fan_video WHERE external_video_id=?",Long.class,video);if(matched.size()!=1)throw new ServiceException("第 "+(i+1)+" 行视频编号不存在");
     var existing=jdbc.queryForList("SELECT final_gmv FROM fan_video_gmv_snapshot WHERE source=? AND video_id=? AND snapshot_at=? AND source_version=?",BigDecimal.class,job.get("source"),matched.get(0),job.get("snapshot_at"),job.get("source_version"));
     if(!existing.isEmpty() && existing.get(0).compareTo(amount)!=0)throw new ServiceException("第 "+(i+1)+" 行数据与已有记录冲突");
     count++;if(sample.size()<100)sample.add(Map.of("videoId",video,"finalGmv",amount));
    }
   }
   if(count==0)throw new ServiceException("文件没有数据");jdbc.update("UPDATE fan_async_import SET state='PREVIEW_READY',total_rows=?,preview_json=?,version=version+1 WHERE id=?",count,json.writeValueAsString(sample),id);
  }else if("IMPORT".equals(kind)&&"IMPORT_QUEUED".equals(state)){
   var result=importer.importFile(((Number)job.get("requested_by")).longValue(),(String)job.get("source"),toLocalDateTime(job.get("snapshot_at")),((Number)job.get("source_version")).longValue(),new StagedMultipart((byte[])job.get("file_data"),(String)job.get("file_name")));
   jdbc.update("UPDATE fan_async_import SET state='COMPLETED',result_json=?,version=version+1 WHERE id=?",json.writeValueAsString(result),id);
  }
 }
 public void fail(long id,String kind,Exception e){String m=e instanceof ServiceException?e.getMessage():"处理失败，请联系管理员检查日志";if(m==null)m="处理失败";if(m.length()>1000)m=m.substring(0,1000);jdbc.update("UPDATE fan_async_import SET state='FAILED',error_message=?,version=version+1 WHERE id=? AND state=?",m,id,"PREVIEW".equals(kind)?"PREVIEW_QUEUED":"IMPORT_QUEUED");}
 private static LocalDateTime toLocalDateTime(Object value){
  if(value instanceof LocalDateTime time)return time;
  if(value instanceof java.sql.Timestamp timestamp)return timestamp.toLocalDateTime();
  throw new IllegalStateException("Unsupported timestamp type");
 }
 private static String text(Cell c){return c!=null&&c.getCellType()==CellType.STRING?c.getStringCellValue().trim():"";}
}
