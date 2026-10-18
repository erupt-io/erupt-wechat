package xyz.erupt.wechat.handler;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.erupt.core.config.GsonFactory;
import xyz.erupt.jpa.dao.EruptDao;
import xyz.erupt.wechat.core.WxContext;
import xyz.erupt.wechat.form.WxTemplateSendForm;
import xyz.erupt.wechat.live.WxTemplate;
import xyz.erupt.wechat.model.WxTemplateLog;
import me.chanjar.weixin.mp.bean.template.WxMpTemplateData;
import me.chanjar.weixin.mp.bean.template.WxMpTemplateMessage;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Sends a template to each openid and logs every attempt; the portal fills in the delivery result.
 *
 * @author YuePeng
 */
@Component
public class WxTemplateSendHandler implements OperationHandler<WxTemplate, WxTemplateSendForm> {

    private final EruptDao eruptDao;

    private final WxContext context;

    public WxTemplateSendHandler(EruptDao eruptDao, WxContext context) {
        this.eruptDao = eruptDao;
        this.context = context;
    }

    @Override
    @Transactional
    public String exec(List<WxTemplate> data, WxTemplateSendForm form, String[] param) {
        WxTemplate template = data.get(0);
        int success = 0, fail = 0;
        for (String openid : xyz.erupt.wechat.core.Wx.lines(form.getOpenids())) {
            WxMpTemplateMessage message = WxMpTemplateMessage.builder().toUser(openid).templateId(template.getTemplateId()).url(form.getUrl()).build();
            this.fill(message, form.getData());
            WxTemplateLog log = new WxTemplateLog();
            log.setAccount(context.account());
            log.setTemplateId(template.getTemplateId());
            log.setTitle(template.getTitle());
            log.setToOpenid(openid);
            log.setData(form.getData());
            log.setUrl(form.getUrl());
            log.setSendTime(LocalDateTime.now());
            try {
                log.setMsgId(context.mp().getTemplateMsgService().sendTemplateMsg(message));
                log.setStatus(WxTemplateLog.SENT);
                success++;
            } catch (Exception e) {
                log.setStatus(WxTemplateLog.FAILED);
                log.setErrorMsg(e.getMessage());
                fail++;
            }
            eruptDao.persist(log);
        }
        return "msg.success('" + success + " / " + fail + "')";
    }

    // {"first":"Hello"} or {"first":{"value":"Hello","color":"#ff0000"}}
    private void fill(WxMpTemplateMessage message, String json) {
        if (StringUtils.isBlank(json)) return;
        JsonObject data = GsonFactory.getGson().fromJson(json, JsonObject.class);
        for (String key : data.keySet()) {
            JsonElement value = data.get(key);
            if (value.isJsonObject()) {
                JsonObject item = value.getAsJsonObject();
                message.addData(new WxMpTemplateData(key, item.has("value") ? item.get("value").getAsString() : "",
                        item.has("color") ? item.get("color").getAsString() : null));
            } else {
                message.addData(new WxMpTemplateData(key, value.getAsString()));
            }
        }
    }

}
