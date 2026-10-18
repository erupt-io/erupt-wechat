package xyz.erupt.wechat.handler;

import org.springframework.stereotype.Component;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.erupt.wechat.core.Wx;
import xyz.erupt.wechat.core.WxContext;
import xyz.erupt.wechat.live.WxPublication;

import java.util.List;

/**
 * @author YuePeng
 */
@Component
public class WxPublicationHandler implements OperationHandler<WxPublication, Void> {

    public static final String OPEN_COMMENTS = "open";

    public static final String CLOSE_COMMENTS = "close";

    private final WxContext context;

    public WxPublicationHandler(WxContext context) {
        this.context = context;
    }

    @Override
    public String exec(List<WxPublication> data, Void form, String[] param) {
        for (WxPublication publication : data) {
            Wx.run(() -> {
                if (OPEN_COMMENTS.equals(param[0])) context.mp().getCommentService().open(publication.getArticleId(), null);
                else context.mp().getCommentService().close(publication.getArticleId(), null);
            });
        }
        return "msg.success('OK')";
    }

}
