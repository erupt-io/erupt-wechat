package xyz.erupt.wechat.core;

import org.springframework.stereotype.Component;
import xyz.erupt.annotation.fun.ChoiceFetchHandler;
import xyz.erupt.annotation.fun.VLModel;

import java.util.List;

/**
 * Tags of the account picked in the same form; outside the form, of the current workspace.
 *
 * @author YuePeng
 */
@Component
public class WxScopeTagFetchHandler implements ChoiceFetchHandler<WxScope> {

    private final WxTagCache tagCache;

    public WxScopeTagFetchHandler(WxTagCache tagCache) {
        this.tagCache = tagCache;
    }

    // Without an account there are no tags to offer: the form asks through fetchFilter
    @Override
    public List<VLModel> fetch(String[] params) {
        return List.of();
    }

    @Override
    public List<VLModel> fetchFilter(WxScope scope, String[] params) {
        if (null == scope || null == scope.getAccount()) return List.of();
        return tagCache.tags(scope.getAccount()).stream().map(it -> new VLModel(String.valueOf(it.getId()), it.getName())).toList();
    }

}
