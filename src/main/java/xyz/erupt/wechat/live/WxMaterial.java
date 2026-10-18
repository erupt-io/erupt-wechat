package xyz.erupt.wechat.live;

import lombok.Getter;
import lombok.Setter;
import me.chanjar.weixin.common.api.WxConsts;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.mp.bean.material.WxMpMaterial;
import me.chanjar.weixin.mp.bean.material.WxMpMaterialFileBatchGetResult;
import org.springframework.stereotype.Component;
import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.EruptScope;
import xyz.erupt.annotation.fun.ChoiceFetchHandler;
import xyz.erupt.annotation.fun.VLModel;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.ViewType;
import xyz.erupt.annotation.sub_field.sub_edit.AttachmentType;
import xyz.erupt.annotation.sub_field.sub_edit.ChoiceType;
import xyz.erupt.annotation.sub_field.sub_edit.Search;
import xyz.erupt.annotation.sub_field.sub_edit.VL;
import xyz.erupt.core.annotation.EruptDataProcessor;
import xyz.erupt.core.exception.EruptWebApiRuntimeException;
import xyz.erupt.core.prop.EruptProp;
import xyz.erupt.core.query.EruptQuery;
import xyz.erupt.wechat.core.Wx;
import xyz.erupt.wechat.core.WxContext;
import xyz.erupt.wechat.core.WxScope;
import xyz.erupt.wechat.data.WxDataService;
import xyz.erupt.wechat.data.WxResource;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Permanent image, voice and video materials. Adding one uploads the attached file.
 *
 * @author YuePeng
 */
@Getter
@Setter
@EruptScope(value = WxScope.class, bind = false)
@EruptDataProcessor(WxDataService.DATA_PROCESSOR)
@Erupt(name = "Materials", primaryKeyCol = "mediaId", power = @Power(edit = false))
public class WxMaterial {

    @EruptField(views = @View(title = "Media ID"))
    private String mediaId;

    @EruptField(views = @View(title = "Type"), edit = @Edit(title = "Type", notNull = true, search = @Search, type = EditType.CHOICE, choiceType = @ChoiceType(vl = {
            @VL(value = WxConsts.MaterialType.IMAGE, label = "Image"),
            @VL(value = WxConsts.MaterialType.VOICE, label = "Voice"),
            @VL(value = WxConsts.MaterialType.VIDEO, label = "Video")
    })))
    private String type = WxConsts.MaterialType.IMAGE;

    @EruptField(views = @View(title = "Name"), edit = @Edit(title = "Name", notNull = true, search = @Search))
    private String name;

    @EruptField(edit = @Edit(title = "File", notNull = true, type = EditType.ATTACHMENT, attachmentType = @AttachmentType(maxLimit = 1),
            desc = "Image: jpg, png or gif under 10MB. Voice: mp3, wma, wav or amr under 2MB. Video: mp4 under 10MB"))
    private String file;

    @EruptField(views = @View(title = "URL", type = ViewType.LINK))
    private String url;

    @EruptField(views = @View(title = "Updated at", sortable = true))
    private LocalDateTime updateTime;

    @Component
    public static class Resource implements WxResource<WxMaterial> {

        private static final int PAGE = 20;

        private static final int MAX = 1000;

        private final EruptProp eruptProp;

        public Resource(EruptProp eruptProp) {
            this.eruptProp = eruptProp;
        }

        @Override
        public Class<WxMaterial> type() {
            return WxMaterial.class;
        }

        @Override
        public List<WxMaterial> list(WxContext wx, EruptQuery query) throws WxErrorException {
            Object type = WxDataService.condition(query, "type");
            List<String> types = null == type ? List.of(WxConsts.MaterialType.IMAGE, WxConsts.MaterialType.VOICE, WxConsts.MaterialType.VIDEO) : List.of(type.toString());
            List<WxMaterial> rows = new ArrayList<>();
            for (String each : types) {
                int offset = 0;
                WxMpMaterialFileBatchGetResult page;
                do {
                    page = wx.mp().getMaterialService().materialFileBatchGet(each, offset, PAGE);
                    if (null != page.getItems()) {
                        for (WxMpMaterialFileBatchGetResult.WxMaterialFileBatchGetNewsItem item : page.getItems()) {
                            WxMaterial row = new WxMaterial();
                            row.setMediaId(item.getMediaId());
                            row.setType(each);
                            row.setName(item.getName());
                            row.setUrl(item.getUrl());
                            row.setUpdateTime(Wx.time(item.getUpdateTime()));
                            rows.add(row);
                        }
                    }
                    offset += PAGE;
                } while (offset < page.getTotalCount() && offset < MAX);
            }
            return rows;
        }

        @Override
        public void add(WxContext wx, WxMaterial row) throws WxErrorException {
            File file = new File(eruptProp.getUploadPath(), row.getFile());
            if (!file.exists()) throw new EruptWebApiRuntimeException("File not found: " + row.getFile());
            WxMpMaterial material = new WxMpMaterial();
            material.setName(row.getName());
            material.setFile(file);
            if (WxConsts.MaterialType.VIDEO.equals(row.getType())) {
                material.setVideoTitle(row.getName());
                material.setVideoIntroduction(row.getName());
            }
            row.setMediaId(wx.mp().getMaterialService().materialFileUpload(row.getType(), material).getMediaId());
        }

        @Override
        public void delete(WxContext wx, WxMaterial row) throws WxErrorException {
            wx.mp().getMaterialService().materialDelete(row.getMediaId());
        }
    }

    /**
     * The account's images, for fields that take a media id: the thumbnail of a draft, the image
     * of an auto reply.
     */
    @Component
    public static class ImageOptions implements ChoiceFetchHandler<Object> {

        private final WxContext context;

        private final Resource resource;

        public ImageOptions(WxContext context, Resource resource) {
            this.context = context;
            this.resource = resource;
        }

        @Override
        public List<VLModel> fetch(String[] params) {
            EruptQuery query = EruptQuery.builder().conditions(List.of(new xyz.erupt.annotation.query.Condition("type", WxConsts.MaterialType.IMAGE, xyz.erupt.annotation.config.QueryExpression.EQ))).build();
            return Wx.call(() -> resource.list(context, query)).stream().map(it -> new VLModel(it.getMediaId(), it.getName())).toList();
        }
    }

}
