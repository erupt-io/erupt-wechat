package xyz.erupt.wechat.handler;

import me.chanjar.weixin.common.api.WxConsts;
import me.chanjar.weixin.common.bean.menu.WxMenu;
import me.chanjar.weixin.common.bean.menu.WxMenuButton;
import me.chanjar.weixin.common.bean.menu.WxMenuRule;
import me.chanjar.weixin.mp.api.WxMpMenuService;
import org.springframework.stereotype.Component;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.erupt.core.exception.EruptWebApiRuntimeException;
import xyz.erupt.core.i18n.I18nTranslate;
import xyz.erupt.jpa.dao.EruptDao;
import xyz.erupt.wechat.core.Wx;
import xyz.erupt.wechat.core.WxContext;
import xyz.erupt.wechat.model.WxAccount;

import java.util.*;

/**
 * Publishes the workspace account's menus: the untagged top-level menus become the default menu,
 * and each tag's top-level menus a conditional menu for that tag. WeChat allows 3 top-level menus
 * with 5 sub menus each.
 *
 * @author YuePeng
 */
@Component
public class WxMenuPublishHandler implements OperationHandler<Object, Void> {

    public static final String PUBLISH = "publish";

    public static final String DELETE = "delete";

    private final EruptDao eruptDao;

    private final WxContext context;

    public WxMenuPublishHandler(EruptDao eruptDao, WxContext context) {
        this.eruptDao = eruptDao;
        this.context = context;
    }

    @Override
    public String exec(List<Object> data, Void form, String[] param) {
        WxMpMenuService menus = context.mp().getMenuService();
        if (DELETE.equals(param[0])) {
            Wx.run(menus::menuDelete);
            return "msg.success('OK')";
        }
        List<xyz.erupt.wechat.model.WxMenu> all = eruptDao.lambdaQuery(xyz.erupt.wechat.model.WxMenu.class)
                .with(xyz.erupt.wechat.model.WxMenu::getAccount).eq(WxAccount::getId, context.accountId()).with()
                .orderBy(xyz.erupt.wechat.model.WxMenu::getSort).list();
        // tag -> its top-level menus; null is the default menu
        Map<Long, List<xyz.erupt.wechat.model.WxMenu>> tops = new LinkedHashMap<>();
        for (xyz.erupt.wechat.model.WxMenu menu : all) {
            if (null == menu.getParent()) tops.computeIfAbsent(menu.getForTag(), it -> new ArrayList<>()).add(menu);
        }
        if (!tops.containsKey(null)) throw new EruptWebApiRuntimeException(I18nTranslate.$translate("wechat.menu_empty"));
        // conditional menus are replaced by deleting them all first
        Wx.run(menus::menuDelete);
        Wx.call(() -> menus.menuCreate(this.menu(tops.get(null), all, null)));
        for (Map.Entry<Long, List<xyz.erupt.wechat.model.WxMenu>> entry : tops.entrySet()) {
            if (null != entry.getKey()) Wx.call(() -> menus.menuCreate(this.menu(entry.getValue(), all, entry.getKey())));
        }
        return "msg.success('OK')";
    }

    private WxMenu menu(List<xyz.erupt.wechat.model.WxMenu> tops, List<xyz.erupt.wechat.model.WxMenu> all, Long tag) {
        if (tops.size() > 3) throw new EruptWebApiRuntimeException(I18nTranslate.$translate("wechat.menu_top_limit"));
        WxMenu menu = new WxMenu();
        for (xyz.erupt.wechat.model.WxMenu top : tops) {
            WxMenuButton button = new WxMenuButton();
            button.setName(top.getName());
            List<xyz.erupt.wechat.model.WxMenu> children = all.stream()
                    .filter(it -> null != it.getParent() && top.getId().equals(it.getParent().getId())).toList();
            if (children.size() > 5) throw new EruptWebApiRuntimeException(I18nTranslate.$translate("wechat.menu_sub_limit") + " → " + top.getName());
            if (children.isEmpty()) {
                this.fill(button, top);
            } else {
                for (xyz.erupt.wechat.model.WxMenu child : children) {
                    WxMenuButton sub = new WxMenuButton();
                    sub.setName(child.getName());
                    this.fill(sub, child);
                    button.getSubButtons().add(sub);
                }
            }
            menu.getButtons().add(button);
        }
        if (null != tag) {
            WxMenuRule rule = new WxMenuRule();
            rule.setTagId(String.valueOf(tag));
            menu.setMatchRule(rule);
        }
        return menu;
    }

    private void fill(WxMenuButton button, xyz.erupt.wechat.model.WxMenu menu) {
        switch (menu.getType()) {
            case xyz.erupt.wechat.model.WxMenu.CLICK -> {
                button.setType(WxConsts.MenuButtonType.CLICK);
                button.setKey(menu.getMenuKey());
            }
            case xyz.erupt.wechat.model.WxMenu.VIEW -> {
                button.setType(WxConsts.MenuButtonType.VIEW);
                button.setUrl(menu.getUrl());
            }
            case xyz.erupt.wechat.model.WxMenu.MINIPROGRAM -> {
                button.setType(WxConsts.MenuButtonType.MINIPROGRAM);
                button.setAppId(menu.getMiniAppId());
                button.setPagePath(menu.getMiniPagePath());
                button.setUrl(menu.getUrl());
            }
            default -> throw new EruptWebApiRuntimeException(I18nTranslate.$translate("wechat.menu_folder_empty") + " → " + menu.getName());
        }
    }

}
