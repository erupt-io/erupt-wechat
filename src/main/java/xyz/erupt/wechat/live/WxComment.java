package xyz.erupt.wechat.live;

import lombok.Getter;
import lombok.Setter;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.mp.bean.comment.WxMpCommentListVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.EruptScope;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_erupt.RowOperation;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.BoolType;
import xyz.erupt.annotation.sub_field.sub_edit.Search;
import xyz.erupt.core.annotation.EruptDataProcessor;
import xyz.erupt.core.query.EruptQuery;
import xyz.erupt.wechat.core.Wx;
import xyz.erupt.wechat.core.WxContext;
import xyz.erupt.wechat.core.WxScope;
import xyz.erupt.wechat.data.WxDataService;
import xyz.erupt.wechat.data.WxResource;
import xyz.erupt.wechat.form.WxReplyForm;
import xyz.erupt.wechat.handler.WxCommentHandler;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Comments under one article, opened from Publications or by article id.
 *
 * @author YuePeng
 */
@Getter
@Setter
@EruptScope(value = WxScope.class, bind = false)
@EruptDataProcessor(WxDataService.DATA_PROCESSOR)
@Erupt(name = "Comments", power = @Power(add = false, edit = false), rowOperation = {
        @RowOperation(code = "elect", title = "Feature", icon = "fa fa-star", mode = RowOperation.Mode.MULTI,
                operationHandler = WxCommentHandler.class, operationParam = WxCommentHandler.ELECT),
        @RowOperation(code = "unelect", title = "Unfeature", icon = "fa fa-star-o", mode = RowOperation.Mode.MULTI,
                operationHandler = WxCommentHandler.class, operationParam = WxCommentHandler.UNELECT),
        @RowOperation(code = "reply", title = "Reply", icon = "fa fa-reply", mode = RowOperation.Mode.SINGLE, eruptClass = WxReplyForm.class,
                operationHandler = WxCommentHandler.class, operationParam = WxCommentHandler.REPLY),
        @RowOperation(code = "replyDelete", title = "Delete reply", icon = "fa fa-trash-o", mode = RowOperation.Mode.SINGLE,
                operationHandler = WxCommentHandler.class, operationParam = WxCommentHandler.REPLY_DELETE)
})
public class WxComment {

    @EruptField(views = @View(title = "Article"), edit = @Edit(title = "Article", search = @Search(notNull = true), desc = "The article id of a publication"))
    private String msgDataId;

    @EruptField(views = @View(title = "ID"))
    private String id;

    @EruptField(views = @View(title = "OpenID"))
    private String openid;

    @EruptField(views = @View(title = "Comment"))
    private String content;

    @EruptField(views = @View(title = "Featured"), edit = @Edit(title = "Featured", search = @Search, boolType = @BoolType(trueText = "Featured", falseText = "Plain")))
    private Boolean elected;

    @EruptField(views = @View(title = "Reply"))
    private String reply;

    @EruptField(views = @View(title = "Commented at", sortable = true))
    private LocalDateTime createTime;

    @Component
    public static class Resource implements WxResource<WxComment> {

        private static final int PAGE = 50;

        private static final int MAX = 1000;

        @Override
        public Class<WxComment> type() {
            return WxComment.class;
        }

        @Override
        public List<WxComment> list(WxContext wx, EruptQuery query) throws WxErrorException {
            Object article = WxDataService.condition(query, "msgDataId");
            List<WxComment> rows = new ArrayList<>();
            if (null == article) return rows;
            String msgDataId = article.toString();
            int begin = 0;
            WxMpCommentListVo page;
            do {
                page = wx.mp().getCommentService().list(msgDataId, null, begin, PAGE, 0);
                if (null != page.getComment()) {
                    for (WxMpCommentListVo.WxMpComment comment : page.getComment()) {
                        WxComment row = new WxComment();
                        row.setMsgDataId(msgDataId);
                        row.setId(String.valueOf(comment.getUserCommentId()));
                        row.setOpenid(comment.getOpenid());
                        row.setContent(comment.getContent());
                        row.setElected(Integer.valueOf(1).equals(comment.getCommentType()));
                        if (null != comment.getReply()) row.setReply(comment.getReply().getContent());
                        if (StringUtils.isNumeric(comment.getCreateTime())) row.setCreateTime(Wx.time(Long.parseLong(comment.getCreateTime())));
                        rows.add(row);
                    }
                }
                begin += PAGE;
            } while (null != page.getTotal() && begin < page.getTotal() && begin < MAX);
            return rows;
        }

        @Override
        public void delete(WxContext wx, WxComment row) throws WxErrorException {
            wx.mp().getCommentService().delete(row.getMsgDataId(), null, Long.valueOf(row.getId()));
        }
    }

}
