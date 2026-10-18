package xyz.erupt.wechat.proxy;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import xyz.erupt.annotation.fun.DataProxy;
import xyz.erupt.annotation.query.Condition;
import xyz.erupt.wechat.core.WxContext;
import xyz.erupt.wechat.core.WxTagCache;
import xyz.erupt.wechat.model.WxFans;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The workspace tag narrows the fans; tag ids are shown by name.
 *
 * @author YuePeng
 */
@Component
public class WxFansDataProxy implements DataProxy<WxFans> {

    private final WxContext context;

    private final WxTagCache tagCache;

    public WxFansDataProxy(WxContext context, WxTagCache tagCache) {
        this.context = context;
        this.tagCache = tagCache;
    }

    @Override
    public String beforeFetch(List<Condition> conditions) {
        Long tag = context.scope().getTag();
        // the tag is a number the scope checked against the account's tags, never free text
        return null == tag ? null : WxFans.class.getSimpleName() + ".tagIds like '%," + tag + ",%'";
    }

    @Override
    public void afterFetch(Collection<Map<String, Object>> list) {
        Long accountId = context.accountId();
        for (Map<String, Object> row : list) {
            String tagIds = (String) row.get("tagIds");
            if (StringUtils.isBlank(tagIds)) continue;
            row.put("tags", Arrays.stream(tagIds.split(",")).filter(StringUtils::isNotBlank)
                    .map(it -> tagCache.name(accountId, Long.valueOf(it))).collect(Collectors.joining(", ")));
        }
    }

}
