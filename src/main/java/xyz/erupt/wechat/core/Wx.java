package xyz.erupt.wechat.core;

import me.chanjar.weixin.common.error.WxErrorException;
import xyz.erupt.core.exception.EruptWebApiRuntimeException;
import xyz.erupt.core.i18n.I18nTranslate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/**
 * Small helpers around WxJava: every API call goes through {@link #call} so an errcode arrives in
 * the page as one translated message.
 *
 * @author YuePeng
 */
public final class Wx {

    private Wx() {
    }

    @FunctionalInterface
    public interface Call<T> {
        T call() throws WxErrorException;
    }

    @FunctionalInterface
    public interface Run {
        void run() throws WxErrorException;
    }

    public static <T> T call(Call<T> call) {
        try {
            return call.call();
        } catch (WxErrorException e) {
            throw new EruptWebApiRuntimeException(I18nTranslate.$translate("wechat.api_failed") + " → " + e.getMessage());
        }
    }

    public static void run(Run run) {
        call(() -> {
            run.run();
            return null;
        });
    }

    // One value per non-blank line: how openid lists are typed into forms
    public static List<String> lines(String text) {
        return null == text ? List.of() : text.lines().map(String::trim).filter(it -> !it.isEmpty()).toList();
    }

    public static LocalDateTime time(Long epochSeconds) {
        return null == epochSeconds ? null : LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), ZoneId.systemDefault());
    }

    public static LocalDateTime time(Date date) {
        return null == date ? null : LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }

    public static Date date(LocalDate day) {
        return Date.from(day.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

}
