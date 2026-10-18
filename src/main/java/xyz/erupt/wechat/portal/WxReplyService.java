package xyz.erupt.wechat.portal;

import me.chanjar.weixin.common.api.WxConsts;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutNewsMessage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.jpa.dao.EruptDao;
import xyz.erupt.wechat.core.Wx;
import xyz.erupt.wechat.model.*;

import java.util.List;

/**
 * What happens to a message or event WeChat pushes: it is logged, fans and send results are kept
 * current, and a text message or a subscribe gets the matching auto reply.
 *
 * @author YuePeng
 */
@Service
public class WxReplyService {

    private final EruptDao eruptDao;

    public WxReplyService(EruptDao eruptDao) {
        this.eruptDao = eruptDao;
    }

    @Transactional
    public WxMpXmlOutMessage receive(Long accountId, WxMpXmlMessage in) {
        WxAccount account = eruptDao.find(WxAccount.class, accountId);
        this.log(account, in);
        WxAutoReply rule = null;
        if (WxConsts.XmlMsgType.EVENT.equals(in.getMsgType())) {
            switch (null == in.getEvent() ? "" : in.getEvent()) {
                case WxConsts.EventType.SUBSCRIBE -> {
                    this.fans(account, in, true);
                    rule = this.rule(account, WxAutoReply.SUBSCRIBE, null);
                }
                case WxConsts.EventType.UNSUBSCRIBE -> this.fans(account, in, false);
                case WxConsts.EventType.TEMPLATE_SEND_JOB_FINISH -> this.templateResult(in);
                case WxConsts.EventType.MASS_SEND_JOB_FINISH -> this.massResult(in);
                default -> {
                }
            }
        } else if (WxConsts.XmlMsgType.TEXT.equals(in.getMsgType())) {
            String content = StringUtils.trimToEmpty(in.getContent());
            rule = this.rule(account, WxAutoReply.KEYWORD_EXACT, content);
            if (null == rule) {
                rule = this.rules(account, WxAutoReply.KEYWORD_CONTAIN).stream()
                        .filter(it -> StringUtils.isNotBlank(it.getKeyword()) && content.contains(it.getKeyword())).findFirst().orElse(null);
            }
            if (null == rule) rule = this.rule(account, WxAutoReply.DEFAULT, null);
        }
        return null == rule ? null : this.reply(rule, in);
    }

    private void log(WxAccount account, WxMpXmlMessage in) {
        WxEventLog log = new WxEventLog();
        log.setAccount(account);
        log.setOpenid(in.getFromUser());
        log.setMsgType(in.getMsgType());
        log.setEvent(in.getEvent());
        log.setEventKey(in.getEventKey());
        log.setContent(StringUtils.abbreviate(StringUtils.firstNonBlank(in.getContent(), in.getRecognition(), in.getTitle(), in.getPicUrl(), in.getMediaId()), 4000));
        log.setMsgId(null == in.getMsgId() ? null : String.valueOf(in.getMsgId()));
        log.setCreateTime(null == in.getCreateTime() ? java.time.LocalDateTime.now() : Wx.time(in.getCreateTime()));
        eruptDao.persist(log);
    }

    private void fans(WxAccount account, WxMpXmlMessage in, boolean subscribe) {
        WxFans fans = eruptDao.lambdaQuery(WxFans.class)
                .with(WxFans::getAccount).eq(WxAccount::getId, account.getId()).with()
                .eq(WxFans::getOpenid, in.getFromUser()).list().stream().findFirst().orElse(null);
        if (null == fans) {
            fans = new WxFans();
            fans.setAccount(account);
            fans.setOpenid(in.getFromUser());
        }
        fans.setSubscribe(subscribe);
        if (subscribe) {
            fans.setSubscribeTime(Wx.time(in.getCreateTime()));
            // a scan of a parameter code arrives as "qrscene_<scene>"
            fans.setSubscribeScene(in.getEventKey());
        }
        eruptDao.merge(fans);
    }

    private void templateResult(WxMpXmlMessage in) {
        if (null == in.getMsgId()) return;
        for (WxTemplateLog log : eruptDao.lambdaQuery(WxTemplateLog.class).eq(WxTemplateLog::getMsgId, String.valueOf(in.getMsgId())).list()) {
            boolean delivered = "success".equalsIgnoreCase(in.getStatus());
            log.setStatus(delivered ? WxTemplateLog.DELIVERED : WxTemplateLog.FAILED);
            if (!delivered) log.setErrorMsg(in.getStatus());
            eruptDao.merge(log);
        }
    }

    private void massResult(WxMpXmlMessage in) {
        if (null == in.getMsgId()) return;
        for (WxMassMessage message : eruptDao.lambdaQuery(WxMassMessage.class).eq(WxMassMessage::getMsgId, String.valueOf(in.getMsgId())).list()) {
            message.setStatus(in.getStatus());
            message.setTotalCount(in.getTotalCount());
            message.setSentCount(in.getSentCount());
            message.setErrorCount(in.getErrorCount());
            eruptDao.merge(message);
        }
    }

    private WxAutoReply rule(WxAccount account, String type, String keyword) {
        return this.rules(account, type).stream().filter(it -> null == keyword || keyword.equals(it.getKeyword())).findFirst().orElse(null);
    }

    private List<WxAutoReply> rules(WxAccount account, String type) {
        return eruptDao.lambdaQuery(WxAutoReply.class)
                .with(WxAutoReply::getAccount).eq(WxAccount::getId, account.getId()).with()
                .eq(WxAutoReply::getType, type).eq(WxAutoReply::getEnable, true).list();
    }

    private WxMpXmlOutMessage reply(WxAutoReply rule, WxMpXmlMessage in) {
        switch (rule.getReplyType()) {
            case WxAutoReply.TEXT -> {
                if (StringUtils.isBlank(rule.getContent())) return null;
                return WxMpXmlOutMessage.TEXT().content(rule.getContent()).fromUser(in.getToUser()).toUser(in.getFromUser()).build();
            }
            case WxAutoReply.IMAGE -> {
                if (StringUtils.isBlank(rule.getMediaId())) return null;
                return WxMpXmlOutMessage.IMAGE().mediaId(rule.getMediaId()).fromUser(in.getToUser()).toUser(in.getFromUser()).build();
            }
            case WxAutoReply.NEWS -> {
                if (StringUtils.isBlank(rule.getNewsUrl())) return null;
                WxMpXmlOutNewsMessage.Item item = new WxMpXmlOutNewsMessage.Item();
                item.setTitle(rule.getNewsTitle());
                item.setDescription(rule.getNewsDescription());
                item.setPicUrl(rule.getNewsPicUrl());
                item.setUrl(rule.getNewsUrl());
                return WxMpXmlOutMessage.NEWS().addArticle(item).fromUser(in.getToUser()).toUser(in.getFromUser()).build();
            }
            default -> {
                return null;
            }
        }
    }

}
