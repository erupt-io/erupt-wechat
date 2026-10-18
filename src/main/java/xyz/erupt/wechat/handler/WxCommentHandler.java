package xyz.erupt.wechat.handler;

import me.chanjar.weixin.mp.api.WxMpCommentService;
import org.springframework.stereotype.Component;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.erupt.wechat.core.Wx;
import xyz.erupt.wechat.core.WxContext;
import xyz.erupt.wechat.form.WxReplyForm;
import xyz.erupt.wechat.live.WxComment;

import java.util.List;

/**
 * @author YuePeng
 */
@Component
public class WxCommentHandler implements OperationHandler<WxComment, WxReplyForm> {

    public static final String ELECT = "elect";

    public static final String UNELECT = "unelect";

    public static final String REPLY = "reply";

    public static final String REPLY_DELETE = "replyDelete";

    private final WxContext context;

    public WxCommentHandler(WxContext context) {
        this.context = context;
    }

    @Override
    public String exec(List<WxComment> data, WxReplyForm form, String[] param) {
        WxMpCommentService comments = context.mp().getCommentService();
        for (WxComment comment : data) {
            Long id = Long.valueOf(comment.getId());
            Wx.run(() -> {
                switch (param[0]) {
                    case ELECT -> comments.markElect(comment.getMsgDataId(), null, id);
                    case UNELECT -> comments.unmarkElect(comment.getMsgDataId(), null, id);
                    case REPLY -> comments.replyAdd(comment.getMsgDataId(), null, id, form.getContent());
                    case REPLY_DELETE -> comments.replyDelete(comment.getMsgDataId(), null, id);
                    default -> {
                    }
                }
            });
        }
        return "msg.success('OK')";
    }

}
