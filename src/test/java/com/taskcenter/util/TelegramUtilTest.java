package com.taskcenter.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.time.LocalTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TelegramUtilTest {

    @Test
    void escapeHtml_withSpecialCharacters_escapesCorrectly() {
        String input = "Task <name> & _description_";
        String expected = "Task &lt;name&gt; &amp; _description_";
        
        String result = TelegramUtil.escapeHtml(input);
        
        assertEquals(expected, result);
    }
    
    @Test
    void escapeHtml_nullInput_returnsEmptyString() {
        assertEquals("", TelegramUtil.escapeHtml(null));
    }

    @Test
    @DisplayName("Mention parser: ali@gmail.com dan emailni olmasligi kerak")
    void extractMentionedUsernames_ignoresEmails() {
        Set<String> usernames = TelegramUtil.extractMentionedUsernames("Mening pochtam ali@gmail.com, yozing.");
        assertThat(usernames).isEmpty();
    }

    @Test
    @DisplayName("Mention parser: salom @ali. dan oxirgi nuqta kesilishi kerak")
    void extractMentionedUsernames_trimsTrailingDot() {
        Set<String> usernames = TelegramUtil.extractMentionedUsernames("salom @ali.");
        assertThat(usernames).containsExactly("ali");
    }

    @Test
    @DisplayName("Mention parser: @ali, @vali ikkala usernameni ajratishi kerak")
    void extractMentionedUsernames_multipleMentions() {
        Set<String> usernames = TelegramUtil.extractMentionedUsernames("@ali, @vali");
        assertThat(usernames).containsExactly("ali", "vali");
    }

    @Test
    @DisplayName("Mention parser: @yoq_user ni to'g'ri olishi kerak")
    void extractMentionedUsernames_withUnderscore() {
        Set<String> usernames = TelegramUtil.extractMentionedUsernames("@yoq_user");
        assertThat(usernames).containsExactly("yoq_user");
    }

    @Test
    @DisplayName("Mention parser: @ali- dan oxirgi tire kesilishi kerak")
    void extractMentionedUsernames_trimsTrailingDash() {
        Set<String> usernames = TelegramUtil.extractMentionedUsernames("salom @ali- va @valid-user!");
        assertThat(usernames).containsExactly("ali", "valid-user");
    }

    @Test
    @DisplayName("truncateComment: 100 dan kam matn to'liq escape qilinadi")
    void truncateComment_shortText_escapesHtmlWithoutTruncation() {
        String input = "Salom <dunyo> & 'dostlar'";
        String result = TelegramUtil.truncateComment(input, 100);
        assertThat(result).isEqualTo("Salom &lt;dunyo&gt; &amp; 'dostlar'");
    }

    @Test
    @DisplayName("truncateComment: 100 dan uzun va so'z o'rtasida kesilsa ... qo'shiladi va escape qilinadi")
    void truncateComment_longText_cutsWordWithEllipsis() {
        // 120 ta 'a' belgisi
        String input = "a".repeat(120);
        String result = TelegramUtil.truncateComment(input, 100);
        assertThat(result).isEqualTo("a".repeat(100) + "...");
    }

    @Test
    @DisplayName("truncateComment: avval 100 belgigacha kesib, keyin escapeHtml qilishi kerak (< va & buzilmaydi)")
    void truncateComment_htmlSpecialCharsTruncatedCorrectly() {
        // Matn boshida 90 ta belgi, keyin "<test> & more words to exceed 100 characters limit completely"
        String prefix = "x".repeat(95);
        String input = prefix + "<alert> & reminder text that is very long";
        // 100-belgi: prefix(95) + "<aler" (5 belgi) = 100 belgi
        // 100-belgida so'z o'rtasida (r harfidan keyin t harfi bor) -> "..." qo'shiladi
        String result = TelegramUtil.truncateComment(input, 100);
        assertThat(result).startsWith(prefix + "&lt;aler...");
        // HTML belgilari chala qolmagan (ya'ni &lt; to'liq va to'g'ri)
        assertThat(result).doesNotContain("<alert>");
    }

    @Test
    @DisplayName("isInQuietHours: kunduzi va tun bo'ylab oraliqlar to'g'ri tekshiriladi")
    void isInQuietHours_worksCorrectly() {
        LocalTime start = LocalTime.of(22, 0);
        LocalTime end = LocalTime.of(8, 0);

        // 23:00 - sokin soatda
        assertThat(TelegramUtil.isInQuietHours(LocalTime.of(23, 0), start, end)).isTrue();
        // 07:30 - sokin soatda
        assertThat(TelegramUtil.isInQuietHours(LocalTime.of(7, 30), start, end)).isTrue();
        // 14:00 - sokin soat emas
        assertThat(TelegramUtil.isInQuietHours(LocalTime.of(14, 0), start, end)).isFalse();

        // null qiymatlar bo'lsa cheklov yo'q
        assertThat(TelegramUtil.isInQuietHours(LocalTime.of(23, 0), null, end)).isFalse();
    }

    @Test
    @DisplayName("createTaskViewKeyboard: to'g'ri callbackData va text bilan inline keyboard yaratadi")
    void createTaskViewKeyboard_createsButton() {
        InlineKeyboardMarkup keyboard = TelegramUtil.createTaskViewKeyboard("task-123", "Mening vazifam");
        assertThat(keyboard).isNotNull();
        assertThat(keyboard.getKeyboard()).hasSize(1);
        assertThat(keyboard.getKeyboard().get(0).get(0).getText()).isEqualTo("📋 Mening vazifam");
        assertThat(keyboard.getKeyboard().get(0).get(0).getCallbackData()).isEqualTo("TASK_VIEW_task-123");
    }

    @Test
    @DisplayName("validateTelegramWebAppData: to'g'ri hash bilan TelegramUserData muvaffaqiyatli qaytadi")
    void validateTelegramWebAppData_validSignature_success() throws Exception {
        String botToken = "123456:ABC-DEF1234ghIkl-zyx57W2v1u123ew11";
        String userJson = "{\"id\":99887766,\"first_name\":\"Bekmurod\",\"username\":\"bekmurod_dev\"}";
        String authDate = "1710000000";

        // data_check_string: auth_date=... \n user=...
        String dataCheckString = "auth_date=" + authDate + "\nuser=" + userJson;

        javax.crypto.Mac hmacSha256 = javax.crypto.Mac.getInstance("HmacSHA256");
        javax.crypto.spec.SecretKeySpec secretKeySpec = new javax.crypto.spec.SecretKeySpec(
                "WebAppData".getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256");
        hmacSha256.init(secretKeySpec);
        byte[] secretKey = hmacSha256.doFinal(botToken.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        javax.crypto.Mac dataHmac = javax.crypto.Mac.getInstance("HmacSHA256");
        javax.crypto.spec.SecretKeySpec dataKeySpec = new javax.crypto.spec.SecretKeySpec(secretKey, "HmacSHA256");
        dataHmac.init(dataKeySpec);
        byte[] calculatedHashBytes = dataHmac.doFinal(dataCheckString.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        StringBuilder hexSb = new StringBuilder();
        for (byte b : calculatedHashBytes) {
            hexSb.append(String.format("%02x", b));
        }
        String validHash = hexSb.toString();

        String initData = "auth_date=" + authDate + "&user=" + java.net.URLEncoder.encode(userJson, java.nio.charset.StandardCharsets.UTF_8) + "&hash=" + validHash;

        TelegramUtil.TelegramUserData result = TelegramUtil.validateTelegramWebAppData(initData, botToken);
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(99887766L);
        assertThat(result.firstName()).isEqualTo("Bekmurod");
        assertThat(result.username()).isEqualTo("bekmurod_dev");
        assertThat(result.getDisplayName()).isEqualTo("Bekmurod");
    }

    @Test
    @DisplayName("validateTelegramWebAppData: noto'g'ri hash bo'lsa null qaytadi")
    void validateTelegramWebAppData_invalidSignature_returnsNull() {
        String botToken = "123456:ABC-DEF1234ghIkl-zyx57W2v1u123ew11";
        String initData = "auth_date=1710000000&user=%7B%22id%22%3A123%7D&hash=invalidhash123456";

        TelegramUtil.TelegramUserData result = TelegramUtil.validateTelegramWebAppData(initData, botToken);
        assertThat(result).isNull();
    }
}

