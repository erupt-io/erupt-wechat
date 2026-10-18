package xyz.erupt.wechat.core;

import org.springframework.stereotype.Component;
import xyz.erupt.annotation.fun.ChoiceFetchHandler;
import xyz.erupt.annotation.fun.VLModel;

import java.util.List;

/**
 * Tags of the current workspace's account, for the fields of scoped models and operation forms.
 *
 * @author YuePeng
 */
@Component
public class WxTagFetchHandler implements ChoiceFetchHandler<Object> {

    private final WxContext context;

    private final WxTagCache tagCache;

    public WxTagFetchHandler(WxContext context, WxTagCache tagCache) {
        this.context = context;
        this.tagCache = tagCache;
    }

    @Override
    public List<VLModel> fetch(String[] params) {
        return tagCache.tags(context.accountId()).stream().map(it -> new VLModel(String.valueOf(it.getId()), it.getName())).toList();
    }

}
