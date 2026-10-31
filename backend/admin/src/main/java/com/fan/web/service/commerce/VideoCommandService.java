package com.fan.web.service.commerce;

import java.net.URI;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fan.common.exception.ServiceException;

@Service
public class VideoCommandService {
    private final JdbcTemplate jdbc;
    public VideoCommandService(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    public record CreateVideo(long orderItemId,long productLinkId,String externalVideoId) { }
    public record PublishVideo(String platform,String externalPublishId,String publishUrl,LocalDateTime publishedAt) { }

    @Transactional
    public long create(CreateVideo in) {
        if (in.orderItemId()<=0 || in.productLinkId()<=0 || in.externalVideoId()==null || in.externalVideoId().isBlank() || in.externalVideoId().length()>160) throw new ServiceException("视频信息无效");
        var rows=jdbc.queryForList("""
            SELECT i.product_id,l.url FROM fan_order_item i
            JOIN fan_cooperation_order o ON o.id=i.order_id
            JOIN fan_merchant m ON m.id=o.merchant_id
            JOIN fan_product_link l ON l.id=? AND l.product_id=i.product_id
            WHERE i.id=? AND o.status='IN_PROGRESS' AND m.qualification_status='APPROVED' FOR UPDATE
            """,in.productLinkId(),in.orderItemId());
        if (rows.size()!=1) throw new ServiceException("订单未执行、商家未审核或商品链接不匹配");
        long productId=((Number)rows.get(0).get("product_id")).longValue();
        String link=(String)rows.get(0).get("url");
        KeyHolder key=new GeneratedKeyHolder();
        jdbc.update(c->{PreparedStatement ps=c.prepareStatement("INSERT INTO fan_video(order_item_id,product_id,product_link_id,link_snapshot,external_video_id) VALUES (?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS);ps.setLong(1,in.orderItemId());ps.setLong(2,productId);ps.setLong(3,in.productLinkId());ps.setString(4,link);ps.setString(5,in.externalVideoId().trim());return ps;},key);
        if(key.getKey()==null)throw new ServiceException("视频创建失败");
        return key.getKey().longValue();
    }
    @Transactional
    public long publish(long videoId,PublishVideo in) {
        if(in.platform()==null||in.platform().isBlank()||in.platform().length()>80||in.externalPublishId()==null||in.externalPublishId().isBlank()||in.externalPublishId().length()>160||in.publishedAt()==null||in.publishUrl()==null||in.publishUrl().length()>2048) throw new ServiceException("发布信息无效");
        try { URI url=URI.create(in.publishUrl()); if(!"https".equalsIgnoreCase(url.getScheme()) || url.getHost()==null) throw new IllegalArgumentException(); } catch(IllegalArgumentException ex) { throw new ServiceException("发布链接必须是有效的 HTTPS 地址"); }
        var rows=jdbc.queryForList("SELECT v.id FROM fan_video v JOIN fan_order_item i ON i.id=v.order_item_id JOIN fan_cooperation_order o ON o.id=i.order_id JOIN fan_merchant m ON m.id=o.merchant_id WHERE v.id=? AND o.status='IN_PROGRESS' AND m.qualification_status='APPROVED' FOR UPDATE",videoId);
        if(rows.size()!=1)throw new ServiceException("视频不存在或订单不在执行中");
        KeyHolder key=new GeneratedKeyHolder();
        jdbc.update(c->{PreparedStatement ps=c.prepareStatement("INSERT INTO fan_video_publish(video_id,platform,external_publish_id,publish_url,published_at,status) VALUES (?,?,?,?,?,'PUBLISHED')",Statement.RETURN_GENERATED_KEYS);ps.setLong(1,videoId);ps.setString(2,in.platform().trim());ps.setString(3,in.externalPublishId().trim());ps.setString(4,in.publishUrl());ps.setObject(5,in.publishedAt());return ps;},key);
        if(key.getKey()==null)throw new ServiceException("发布记录创建失败");
        return key.getKey().longValue();
    }
}
