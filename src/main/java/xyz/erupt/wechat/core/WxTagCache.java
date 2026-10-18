package xyz.erupt.wechat.core;

import me.chanjar.weixin.mp.bean.tag.WxUserTag;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The tags of an account, kept for a short while: every request checks its workspace against these
 * options, fan rows show tag names, and forms offer tags, so one list per account per
 * {@value #TTL_MILLIS} ms instead of one API call each time.
 *
 * @author YuePeng
 */
@Component
public class WxTagCache {

    private static final long TTL_MILLIS = 30_000;

    private record Cached(List<WxUserTag> tags, long expireAt) {
    }

    private final Map<Long, Cached> cache = new ConcurrentHashMap<>();

    private final WxMpServices services;

    public WxTagCache(WxMpServices services) {
        this.services = services;
    }

    public List<WxUserTag> tags(Long accountId) {
        long now = System.currentTimeMillis();
        Cached cached = cache.get(accountId);
        if (null != cached && cached.expireAt() > now) return cached.tags();
        List<WxUserTag> tags = Wx.call(() -> services.get(accountId).getUserTagService().tagGet());
        cache.put(accountId, new Cached(tags, now + TTL_MILLIS));
        return tags;
    }

    public String name(Long accountId, Long tagId) {
        return this.tags(accountId).stream().filter(it -> it.getId().equals(tagId)).map(WxUserTag::getName).findFirst().orElse(String.valueOf(tagId));
    }

    // A tag created, renamed or deleted shows up at once
    public void evict(Long accountId) {
        cache.remove(accountId);
    }

}
