package com.fan.web.service.commerce;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import org.apache.poi.ss.usermodel.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.fan.common.exception.ServiceException;

/** Small synchronous import for local development. Larger workloads require a durable queue. */
@Service
public class VideoGmvImportService {
    private final JdbcTemplate jdbc;
    public VideoGmvImportService(JdbcTemplate jdbc){this.jdbc=jdbc;}
    private record Entry(String videoId,BigDecimal amount){}
    public record Result(int inserted,int skipped,String sha256){}

    @Transactional(rollbackFor=Exception.class)
    public Result importFile(long actor,String source,LocalDateTime snapshotAt,long sourceVersion,MultipartFile file) throws Exception {
        if(source==null||!source.matches("[A-Za-z0-9_-]{1,80}")||snapshotAt==null||sourceVersion<1)throw new ServiceException("来源、统计时间或版本无效");
        if(file==null||file.isEmpty()||file.getSize()>8*1024*1024||file.getOriginalFilename()==null||!file.getOriginalFilename().toLowerCase(java.util.Locale.ROOT).endsWith(".xlsx"))throw new ServiceException("请选择小于 8MB 的 XLSX 文件");
        byte[] bytes=file.getBytes();
        if(bytes.length<4||bytes[0]!='P'||bytes[1]!='K')throw new ServiceException("文件格式无效");
        String sha=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        List<Entry> entries=new ArrayList<>();Set<String> seen=new HashSet<>();
        try(Workbook book=WorkbookFactory.create(new ByteArrayInputStream(bytes))){
            if(book.getNumberOfSheets()<1)throw new ServiceException("工作表为空");
            Sheet sheet=book.getSheetAt(0);
            if(sheet.getLastRowNum()>10000)throw new ServiceException("单次最多导入 10000 行");
            Row header=sheet.getRow(0);
            if(header==null||!"视频编号".equals(text(header.getCell(0)))||!"最终GMV(USD)".equals(text(header.getCell(1)))) throw new ServiceException("表头必须为 视频编号 和 最终GMV(USD)");
            for(int i=1;i<=sheet.getLastRowNum();i++){
                Row row=sheet.getRow(i);if(row==null)continue;
                Cell idCell=row.getCell(0),valueCell=row.getCell(1);
                if((idCell==null||idCell.getCellType()==CellType.BLANK)&&(valueCell==null||valueCell.getCellType()==CellType.BLANK))continue;
                String id=text(idCell);
                if(id.isBlank()||id.length()>160||valueCell==null||valueCell.getCellType()!=CellType.NUMERIC)throw new ServiceException("第 "+(i+1)+" 行视频编号或 GMV 无效");
                if(!seen.add(id))throw new ServiceException("第 "+(i+1)+" 行视频编号重复，不能将发布明细当作视频 GMV");
                BigDecimal value=BigDecimal.valueOf(valueCell.getNumericCellValue()).stripTrailingZeros();
                if(value.signum()<0||value.scale()>2||value.precision()-value.scale()>16)throw new ServiceException("第 "+(i+1)+" 行 GMV 须为非负美元金额且最多两位小数");
                entries.add(new Entry(id,value));
            }
        } catch(ServiceException ex){throw ex;} catch(Exception ex){throw new ServiceException("XLSX 解析失败，请检查文件");}
        if(entries.isEmpty())throw new ServiceException("没有可导入的视频数据");
        int inserted=0,skipped=0;
        for(Entry e:entries){
            var ids=jdbc.queryForList("SELECT id FROM fan_video WHERE external_video_id=?",Long.class,e.videoId());
            if(ids.size()!=1)throw new ServiceException("视频编号不存在："+e.videoId());
            long video=ids.get(0);
            var existing=jdbc.queryForList("SELECT final_gmv FROM fan_video_gmv_snapshot WHERE source=? AND video_id=? AND snapshot_at=? AND source_version=?",BigDecimal.class,source,video,snapshotAt,sourceVersion);
            if(!existing.isEmpty()){
                if(existing.get(0).compareTo(e.amount())!=0)throw new ServiceException("同一来源、视频、时间和版本已有不同金额："+e.videoId());
                skipped++;continue;
            }
            jdbc.update("INSERT INTO fan_video_gmv_snapshot(video_id,source,snapshot_at,source_version,currency,final_gmv) VALUES (?,?,?,?,'USD',?)",video,source,snapshotAt,sourceVersion,e.amount());inserted++;
        }
        jdbc.update("INSERT INTO fan_import_batch(source,snapshot_at,source_version,file_sha256,imported_by,inserted_count,skipped_count) VALUES (?,?,?,?,?,?,?)",source,snapshotAt,sourceVersion,sha,actor,inserted,skipped);
        return new Result(inserted,skipped,sha);
    }
    private static String text(Cell c){
        if(c==null||c.getCellType()!=CellType.STRING)return "";
        return c.getStringCellValue().trim();
    }
}
