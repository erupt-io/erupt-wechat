package xyz.erupt.wechat.handler;

import com.google.gson.JsonObject;
import me.chanjar.weixin.mp.bean.subscribe.WxMpSubscribeMessage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.erupt.core.config.GsonFactory;
import xyz.erupt.wechat.core.Wx;
import xyz.erupt.wechat.core.WxContext;
import xyz.erupt.wechat.form.WxSubscribeSendForm;
import xyz.erupt.wechat.live.WxSubscribeTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author YuePeng
 */
@Component
public class WxSubscribeSendHandler implements OperationHandler<WxSubscribeTemplate, WxSubscribeSendForm> {

    private final WxContext context;

    public WxSubscribeSendHandler(WxContext context) {
        this.context = context;
    }

    @Override
    public String exec(List<WxSubscribeTemplate> data, WxSubscribeSendForm form, String[] param) {
        Map<String, String> values = new LinkedHashMap<>();
        if (StringUtils.isNotBlank(form.getData())) {
            JsonObject json = GsonFactory.getGson().fromJson(form.getData(), JsonObject.class);
            for (String key : json.keySet()) values.put(key, json.get(key).getAsString());
        }
        WxMpSubscribeMessage message = WxMpSubscribeMessage.builder().toUser(form.getOpenid())
                .templateId(data.get(0).getPriTmplId()).page(form.getPage()).dataMap(values).build();
        Wx.call(() -> context.mp().getSubscribeMsgService().send(message));
        return "msg.success('OK')";
    }

}
