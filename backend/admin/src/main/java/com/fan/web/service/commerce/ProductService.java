package com.fan.web.service.commerce;

import java.net.URI;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.fan.common.exception.ServiceException;

/** Merchant products, with server-side ownership and qualification checks. */
@Service
public class ProductService {
    private final JdbcTemplate jdbc;
    private final org.springframework.beans.factory.ObjectProvider<com.fan.web.service.storage.PrivateObjectStorage> storage;
    public ProductService(JdbcTemplate jdbc,org.springframework.beans.factory.ObjectProvider<com.fan.web.service.storage.PrivateObjectStorage> storage) { this.jdbc=jdbc;this.storage=storage; }
    public record ProductInput(String name,String description,String url,String platform) { }
    private long approvedMerchant(long userId) {
        List<Long> ids=jdbc.queryForList("SELECT m.id FROM fan_merchant m JOIN fan_merchant_user u ON u.merchant_id=m.id WHERE u.user_id=? AND m.qualification_status='APPROVED'",Long.class,userId);
        if(ids.size()!=1) throw new ServiceException("商家资质尚未审核通过");
        return ids.get(0);
    }
    @Transactional
    public long create(long userId,ProductInput in) {
        if(in.name()==null||in.name().isBlank()||in.name().length()>200||in.description()==null||in.description().isBlank()||in.description().length()>10000||in.platform()==null||in.platform().isBlank()||in.platform().length()>80||in.url()==null||in.url().length()>2048) throw new ServiceException("商品资料不完整或过长");
        try {URI url=URI.create(in.url());if(!"https".equalsIgnoreCase(url.getScheme())||url.getHost()==null)throw new IllegalArgumentException();}catch(IllegalArgumentException e){throw new ServiceException("商品链接必须为有效 HTTPS 地址");}
        long merchantId=approvedMerchant(userId);
        KeyHolder key=new GeneratedKeyHolder();
        jdbc.update(c->{PreparedStatement ps=c.prepareStatement("INSERT INTO fan_product(merchant_id,name,description,status) VALUES (?,?,?,'DRAFT')",Statement.RETURN_GENERATED_KEYS);ps.setLong(1,merchantId);ps.setString(2,in.name().trim());ps.setString(3,in.description().trim());return ps;},key);
        if(key.getKey()==null)throw new ServiceException("创建商品失败");
        long id=key.getKey().longValue();
        jdbc.update("INSERT INTO fan_product_link(product_id,url,platform) VALUES (?,?,?)",id,in.url(),in.platform().trim());
        return id;
    }
    public List<Map<String,Object>> own(long userId) {
        long merchantId=approvedMerchant(userId);
        return jdbc.queryForList("SELECT id,name,description,status,version,created_at FROM fan_product WHERE merchant_id=? ORDER BY id DESC LIMIT 200",merchantId);
    }
    @Transactional(rollbackFor=Exception.class)
    public void upload(long userId,long productId,MultipartFile file) throws java.io.IOException {
        if(file==null||file.isEmpty()||file.getSize()>2*1024*1024)throw new ServiceException("商品图片应小于 2MB");
        byte[] data=file.getBytes();
        boolean png=data.length>=8&&data[0]==(byte)0x89&&data[1]==0x50&&data[2]==0x4e&&data[3]==0x47&&data[4]==0x0d&&data[5]==0x0a&&data[6]==0x1a&&data[7]==0x0a;
        boolean jpeg=data.length>=3&&data[0]==(byte)0xff&&data[1]==(byte)0xd8&&data[2]==(byte)0xff;
        if(!png&&!jpeg)throw new ServiceException("仅支持 PNG 或 JPEG 图片");
        long merchantId=approvedMerchant(userId);
        var products=jdbc.queryForList("SELECT id FROM fan_product WHERE id=? AND merchant_id=? AND status IN ('DRAFT','REJECTED') FOR UPDATE",productId,merchantId);
        if(products.size()!=1)throw new ServiceException("商品不存在或当前不可修改");
        String media=png?"image/png":"image/jpeg";var store=storage.getIfAvailable();String key=store==null?null:store.put(data,media);
        jdbc.update("INSERT INTO fan_product_asset(product_id,content,media_type,uploaded_by,object_key) VALUES (?,?,?,?,?)",productId,key==null?data:null,media,userId,key);
        jdbc.update("UPDATE fan_product SET version=version+1 WHERE id=?",productId);
    }
    @Transactional
    public void submit(long userId,long productId,long expectedVersion) {
        long merchantId=approvedMerchant(userId);
        int n=jdbc.update("UPDATE fan_product SET status='PENDING',version=version+1 WHERE id=? AND merchant_id=? AND status IN ('DRAFT','REJECTED') AND version=? AND EXISTS (SELECT 1 FROM fan_product_asset a WHERE a.product_id=fan_product.id)",productId,merchantId,expectedVersion);
        if(n!=1)throw new ServiceException("无法提交：资料缺失、状态或版本已变更");
    }
    @Transactional
    public void review(long productId,long reviewer,long version,boolean approved,String reason) {
        if(reason!=null&&reason.length()>1000)throw new ServiceException("原因过长");
        if(!approved&&(reason==null||reason.isBlank()))throw new ServiceException("驳回必须填写原因");
        int n=jdbc.update("UPDATE fan_product p JOIN fan_merchant m ON m.id=p.merchant_id SET p.status=?,p.version=p.version+1 WHERE p.id=? AND p.status='PENDING' AND p.version=? AND m.qualification_status='APPROVED'",approved?"APPROVED":"REJECTED",productId,version);
        if(n!=1)throw new ServiceException("商品状态、资质或版本不匹配");
        jdbc.update("INSERT INTO fan_product_review(product_id,reviewer_id,decision,reason) VALUES (?,?,?,?)",productId,reviewer,approved?"APPROVED":"REJECTED",reason);
    }
    public List<Map<String,Object>> assetsForOwner(long userId,long productId) {
        long merchantId=approvedMerchant(userId);
        return jdbc.queryForList("SELECT a.id,a.media_type,a.uploaded_at FROM fan_product_asset a JOIN fan_product p ON p.id=a.product_id WHERE p.id=? AND p.merchant_id=? ORDER BY a.id",productId,merchantId);
    }
    public List<Map<String,Object>> assetsForReview(long productId) {
        return jdbc.queryForList("SELECT id,media_type,uploaded_at FROM fan_product_asset WHERE product_id=? ORDER BY id",productId);
    }
    public byte[] assetForReview(long assetId) {
        var rows=jdbc.queryForList("SELECT content,object_key FROM fan_product_asset WHERE id=?",assetId);
        if(rows.size()!=1)throw new ServiceException("素材不存在");
        return readAsset(rows.get(0));
    }
    private byte[] readAsset(Map<String,Object> row){String key=(String)row.get("object_key");if(key==null)return (byte[])row.get("content");var store=storage.getIfAvailable();if(store==null)throw new ServiceException("对象存储未配置");return store.get(key);}
    public byte[] assetForOwner(long userId,long assetId) {
        long merchantId=approvedMerchant(userId);
        var rows=jdbc.queryForList("SELECT a.content,a.object_key FROM fan_product_asset a JOIN fan_product p ON p.id=a.product_id WHERE a.id=? AND p.merchant_id=?",assetId,merchantId);
        if(rows.size()!=1)throw new ServiceException("素材不存在或无权访问");
        return readAsset(rows.get(0));
    }
}
