package xyz.erupt.wechat.proxy;

import me.chanjar.weixin.mp.api.WxMpQrcodeService;
import me.chanjar.weixin.mp.bean.result.WxMpQrCodeTicket;
import org.springframework.stereotype.Component;
import xyz.erupt.annotation.fun.DataProxy;
import xyz.erupt.wechat.core.Wx;
import xyz.erupt.wechat.core.WxContext;
import xyz.erupt.wechat.model.WxQrCode;

/**
 * Saving a code asks WeChat for its ticket; the picture is WeChat's own showqrcode URL.
 *
 * @author YuePeng
 */
@Component
public class WxQrCodeDataProxy implements DataProxy<WxQrCode> {

    private final WxContext context;

    public WxQrCodeDataProxy(WxContext context) {
        this.context = context;
    }

    @Override
    public void beforeAdd(WxQrCode code) {
        WxMpQrcodeService qrcode = context.mp(code.getAccount().getId()).getQrcodeService();
        WxMpQrCodeTicket ticket = Wx.call(() -> WxQrCode.TEMP.equals(code.getType())
                ? qrcode.qrCodeCreateTmpTicket(code.getScene(), code.getExpireSeconds())
                : qrcode.qrCodeCreateLastTicket(code.getScene()));
        code.setTicket(ticket.getTicket());
        code.setUrl(ticket.getUrl());
        code.setPicUrl(Wx.call(() -> qrcode.qrCodePictureUrl(ticket.getTicket())));
    }

}
