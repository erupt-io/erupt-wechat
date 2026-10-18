package xyz.erupt.wechat.proxy;

import me.chanjar.weixin.mp.api.WxMpMassMessageService;
import me.chanjar.weixin.mp.bean.WxMpMassOpenIdsMessage;
import me.chanjar.weixin.mp.bean.WxMpMassTagMessage;
import me.chanjar.weixin.mp.bean.result.WxMpMassSendResult;
import org.springframework.stereotype.Component;
import xyz.erupt.annotation.fun.DataProxy;
import xyz.erupt.wechat.core.Wx;
import xyz.erupt.wechat.core.WxContext;
import xyz.erupt.wechat.model.WxMassMessage;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Saving a mass message sends it: to the openids typed in, else to the tag, else to everyone.
 *
 * @author YuePeng
 */
@Component
public class WxMassMessageDataProxy implements DataProxy<WxMassMessage> {

    private final WxContext context;

    public WxMassMessageDataProxy(WxContext context) {
        this.context = context;
    }

    @Override
    public void beforeAdd(WxMassMessage message) {
        WxMpMassMessageService mass = context.mp(message.getAccount().getId()).getMassMessageService();
        List<String> openids = Wx.lines(message.getOpenids());
        WxMpMassSendResult result;
        if (openids.isEmpty()) {
            WxMpMassTagMessage tagMessage = new WxMpMassTagMessage();
            tagMessage.setMsgType(message.getMsgType());
            tagMessage.setContent(message.getContent());
            tagMessage.setMediaId(message.getMediaId());
            if (null == message.getTag()) tagMessage.setSendAll(true);
            else tagMessage.setTagId(message.getTag());
            result = Wx.call(() -> mass.massGroupMessageSend(tagMessage));
        } else {
            WxMpMassOpenIdsMessage idsMessage = new WxMpMassOpenIdsMessage();
            idsMessage.setMsgType(message.getMsgType());
            idsMessage.setContent(message.getContent());
            idsMessage.setMediaId(message.getMediaId());
            idsMessage.setToUsers(openids);
            result = Wx.call(() -> mass.massOpenIdsMessageSend(idsMessage));
        }
        message.setMsgId(result.getMsgId());
        message.setStatus(WxMassMessage.SENDING);
        message.setSendTime(LocalDateTime.now());
    }

}
