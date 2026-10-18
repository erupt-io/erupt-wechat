package xyz.erupt.wechat.handler;

import org.springframework.stereotype.Component;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.erupt.wechat.core.Wx;
import xyz.erupt.wechat.core.WxContext;
import xyz.erupt.wechat.live.WxDraft;

import java.util.List;

/**
 * Publishes a draft; WeChat reports the outcome to the portal and it appears under Publications.
 *
 * @author YuePeng
 */
@Component
public class WxDraftPublishHandler implements OperationHandler<WxDraft, Void> {

    private final WxContext context;

    public WxDraftPublishHandler(WxContext context) {
        this.context = context;
    }

    @Override
    public String exec(List<WxDraft> data, Void form, String[] param) {
        String publishId = Wx.call(() -> context.mp().getFreePublishService().submit(data.get(0).getMediaId()));
        return "msg.success('" + publishId + "')";
    }

}
