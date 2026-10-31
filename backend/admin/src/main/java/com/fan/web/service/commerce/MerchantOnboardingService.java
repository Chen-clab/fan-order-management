package com.fan.web.service.commerce;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fan.common.core.domain.entity.SysUser;
import com.fan.common.core.domain.model.RegisterBody;
import com.fan.common.exception.ServiceException;
import com.fan.framework.web.service.SysRegisterService;
import com.fan.system.service.ISysUserService;

/** Merchant account creation and qualification decisions. */
@Service
public class MerchantOnboardingService {
    private final JdbcTemplate jdbc;
    private final SysRegisterService registration;
    private final ISysUserService users;
    private final org.springframework.beans.factory.ObjectProvider<com.fan.web.service.storage.PrivateObjectStorage> storage;

    public MerchantOnboardingService(JdbcTemplate jdbc, SysRegisterService registration, ISysUserService users, org.springframework.beans.factory.ObjectProvider<com.fan.web.service.storage.PrivateObjectStorage> storage) {
        this.jdbc = jdbc;
        this.registration = registration;
        this.users = users;
        this.storage = storage;
    }

    @Transactional(rollbackFor = Exception.class)
    public long register(MerchantApplication request) {
        if (request.getName() == null || request.getName().isBlank() || request.getName().length() > 200
                || request.getCreditCode() == null || request.getCreditCode().isBlank() || request.getCreditCode().length() > 64
                || request.getContactName() == null || request.getContactName().isBlank() || request.getContactName().length() > 100
                || request.getContactPhone() == null || request.getContactPhone().isBlank() || request.getContactPhone().length() > 40
                || (request.getEmail() != null && request.getEmail().length() > 200)) {
            throw new ServiceException("请填写有效的商家名称、信用代码和联系人信息");
        }
        String error = registration.register(request);
        if (!error.isEmpty()) throw new ServiceException(error);
        SysUser user = users.selectUserByUserName(request.getUsername());
        if (user == null) throw new ServiceException("创建商家账号失败");
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO fan_merchant(name, credit_code, contact_name, contact_phone, email, qualification_status) VALUES (?,?,?,?,?,'PENDING')",
                Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, request.getName().trim());
            ps.setString(2, request.getCreditCode().trim());
            ps.setString(3, request.getContactName().trim());
            ps.setString(4, request.getContactPhone().trim());
            ps.setString(5, request.getEmail());
            return ps;
        }, key);
        if (key.getKey() == null) throw new ServiceException("创建商家资料失败");
        long merchantId = key.getKey().longValue();
        jdbc.update("INSERT INTO fan_merchant_user(merchant_id,user_id) VALUES (?,?)", merchantId, user.getUserId());
        return merchantId;
    }

    public Map<String, Object> ownProfile(long userId) {
        var rows = jdbc.queryForList("SELECT m.id,m.name,m.credit_code,m.contact_name,m.contact_phone,m.email,m.qualification_status,m.version FROM fan_merchant m JOIN fan_merchant_user u ON u.merchant_id=m.id WHERE u.user_id=?", userId);
        if (rows.size() != 1) throw new ServiceException("未找到商家资料");
        return rows.get(0);
    }

    @Transactional
    public void review(long id, long expectedVersion, boolean approved, long reviewerId, String reason) {
        if (reason != null && reason.length() > 1000) throw new ServiceException("审核备注过长");
        if (!approved && (reason == null || reason.isBlank())) throw new ServiceException("驳回时必须填写原因");
        if (approved && jdbc.queryForObject("SELECT COUNT(*) FROM fan_merchant_document WHERE merchant_id=?", Long.class, id) == 0) throw new ServiceException("请先提交资质材料");
        int changed = jdbc.update("UPDATE fan_merchant SET qualification_status=?,version=version+1 WHERE id=? AND qualification_status='PENDING' AND version=?", approved ? "APPROVED" : "REJECTED", id, expectedVersion);
        if (changed != 1) throw new ServiceException("审核状态已改变，请刷新后重试");
        jdbc.update("INSERT INTO fan_merchant_review(merchant_id,reviewer_id,decision,reason) VALUES (?,?,?,?)", id, reviewerId, approved ? "APPROVED" : "REJECTED", reason);
    }

    /** Local development implementation: qualification content remains private in the database. */
    @Transactional(rollbackFor = Exception.class)
    public void uploadQualification(long userId, org.springframework.web.multipart.MultipartFile file) throws java.io.IOException {
        if (file == null || file.isEmpty() || file.getSize() > 2 * 1024 * 1024) throw new ServiceException("资质图片大小必须在 2MB 以内");
        byte[] data = file.getBytes();
        boolean png = data.length >= 8 && data[0] == (byte) 0x89 && data[1] == 0x50 && data[2] == 0x4e && data[3] == 0x47 && data[4] == 0x0d && data[5] == 0x0a && data[6] == 0x1a && data[7] == 0x0a;
        boolean jpeg = data.length >= 3 && data[0] == (byte) 0xff && data[1] == (byte) 0xd8 && data[2] == (byte) 0xff;
        if (!png && !jpeg) throw new ServiceException("仅支持 PNG 或 JPEG 格式的资质图片");
        var merchants = jdbc.queryForList("SELECT m.id,m.qualification_status FROM fan_merchant m JOIN fan_merchant_user u ON u.merchant_id=m.id WHERE u.user_id=? FOR UPDATE", userId);
        if (merchants.size() != 1) throw new ServiceException("未找到商家资料");
        var merchant = merchants.get(0);
        String status = (String) merchant.get("qualification_status");
        if (!"PENDING".equals(status) && !"REJECTED".equals(status)) throw new ServiceException("当前状态不能提交资质材料");
        long merchantId = ((Number) merchant.get("id")).longValue();
        String media=png ? "image/png" : "image/jpeg";
        var store=storage.getIfAvailable();
        String key=store==null?null:store.put(data,media);
        jdbc.update("INSERT INTO fan_merchant_document(merchant_id,media_type,content,uploaded_by,object_key) VALUES (?,?,?,?,?)", merchantId,media,key==null?data:null,userId,key);
        jdbc.update("UPDATE fan_merchant SET qualification_status='PENDING',version=version+1 WHERE id=?", merchantId);
    }

    public byte[] qualificationForReview(long merchantId) {
        var docs = jdbc.queryForList("SELECT content,object_key FROM fan_merchant_document WHERE merchant_id=? ORDER BY id DESC LIMIT 1",merchantId);
        if (docs.size() != 1) throw new ServiceException("未找到资质材料");
        var doc=docs.get(0);String key=(String)doc.get("object_key");
        if(key==null)return (byte[])doc.get("content");
        var store=storage.getIfAvailable();if(store==null)throw new ServiceException("对象存储未配置");return store.get(key);
    }

    public static class MerchantApplication extends RegisterBody {
        private String name, creditCode, contactName, contactPhone, email;
        public String getName() { return name; } public void setName(String v) { name=v; }
        public String getCreditCode() { return creditCode; } public void setCreditCode(String v) { creditCode=v; }
        public String getContactName() { return contactName; } public void setContactName(String v) { contactName=v; }
        public String getContactPhone() { return contactPhone; } public void setContactPhone(String v) { contactPhone=v; }
        public String getEmail() { return email; } public void setEmail(String v) { email=v; }
    }
}
