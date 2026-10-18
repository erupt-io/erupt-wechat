package xyz.erupt.wechat.handler;

import me.chanjar.weixin.mp.api.WxMpMassMessageService;
import me.chanjar.weixin.mp.bean.WxMpMassPreviewMessage;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.erupt.jpa.dao.EruptDao;
import xyz.erupt.wechat.core.Wx;
import xyz.erupt.wechat.core.WxContext;
import xyz.erupt.wechat.form.WxPreviewForm;
import xyz.erupt.wechat.model.WxMassMessage;

import java.util.List;

/**
 * @author YuePeng
 */
@Component
public class WxMassMessageHandler implements OperationHandler<WxMassMessage, WxPreviewForm> {

    public static final String PREVIEW = "preview";

    public static final String STATUS = "status";

    public static final String CANCEL = "cancel";

    private final EruptDao eruptDao;

    private final WxContext context;

    public WxMassMessageHandler(EruptDao eruptDao, WxContext context) {
        this.eruptDao = eruptDao;
        this.context = context;
    }

    @Override
    @Transactional
    public String exec(List<WxMassMessage> data, WxPreviewForm form, String[] param) {
        WxMassMessage message = eruptDao.find(WxMassMessage.class, data.get(0).getId());
        WxMpMassMessageService mass = context.mp().getMassMessageService();
        switch (param[0]) {
            case PREVIEW -> {
                WxMpMassPreviewMessage preview = new WxMpMassPreviewMessage();
                preview.setToWxUserOpenid(form.getOpenid());
                preview.setMsgType(message.getMsgType());
                preview.setContent(message.getContent());
                preview.setMediaId(message.getMediaId());
                Wx.run(() -> mass.massMessagePreview(preview));
            }
            case STATUS -> {
                message.setStatus(Wx.call(() -> mass.messageMassGet(Long.valueOf(message.getMsgId()))).getMsgstatus());
                eruptDao.merge(message);
                return "msg.info('" + message.getStatus() + "')";
            }
            case CANCEL -> {
                Wx.run(() -> mass.delete(Long.valueOf(message.getMsgId()), null));
                message.setStatus("CANCELLED");
                eruptDao.merge(message);
            }
            default -> {
            }
        }
        return "msg.success('OK')";
    }

}
