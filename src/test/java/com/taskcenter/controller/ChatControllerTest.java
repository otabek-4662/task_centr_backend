package com.taskcenter.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ChatControllerTest {

    @Autowired
    private MockMvc mvc;

    private String tokenFor(String name) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.token");
    }

    private String bearer(String name) throws Exception {
        return "Bearer " + tokenFor(name);
    }

    private String getUserId(String name) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"password\":\"password123\"}"))
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.user.id");
    }

    // ==================== UMUMIY CHAT ====================

    @Test
    @DisplayName("Umumiy chatga xabar yozish va xabarlar ro'yxatini olish")
    void testPublicChatLifecycle() throws Exception {
        String token = bearer("elshod");

        // 1. Xabar yuborish
        mvc.perform(post("/api/chat/public")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Hammaga salom, bu test xabari!\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").value("Hammaga salom, bu test xabari!"))
                .andExpect(jsonPath("$.data.type").value("PUBLIC"))
                .andExpect(jsonPath("$.data.senderName").value("elshod"))
                .andExpect(jsonPath("$.data.edited").value(false));

        // 2. Xabarlar ro'yxatini olish
        mvc.perform(get("/api/chat/public")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].content").value("Hammaga salom, bu test xabari!"));
    }

    // ==================== DM CHAT ====================

    @Test
    @DisplayName("DM yozish va tarix olish")
    void testDirectMessageLifecycle() throws Exception {
        String elshodToken = bearer("elshod");
        String xusanToken = bearer("xusan");
        String xusanId = getUserId("xusan");
        String elshodId = getUserId("elshod");

        // 1. DM yuborish
        mvc.perform(post("/api/chat/direct")
                        .header("Authorization", elshodToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recipientId\":\"" + xusanId + "\",\"content\":\"Salom Xusan!\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.type").value("DIRECT"))
                .andExpect(jsonPath("$.data.recipientId").value(xusanId));

        // 2. Xusan tarixni tekshiradi
        mvc.perform(get("/api/chat/direct/" + elshodId)
                        .header("Authorization", xusanToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].content").value("Salom Xusan!"));
    }

    // ==================== TAHRIRLASH ====================

    @Test
    @DisplayName("Xabarni tahrirlash va edited flag tekshirish")
    void testEditMessage() throws Exception {
        String token = bearer("elshod");

        // 1. Xabar yuborish
        MvcResult sendResult = mvc.perform(post("/api/chat/public")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Xato yozildi\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        String messageId = JsonPath.read(sendResult.getResponse().getContentAsString(), "$.data.id");

        // 2. Tahrirlash
        mvc.perform(put("/api/chat/messages/" + messageId)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"To'g'ri yozildi\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("To'g'ri yozildi"))
                .andExpect(jsonPath("$.data.edited").value(true))
                .andExpect(jsonPath("$.data.editedAt").isNotEmpty());
    }

    // ==================== O'CHIRISH ====================

    @Test
    @DisplayName("Xabarni o'chirish")
    void testDeleteMessage() throws Exception {
        String token = bearer("elshod");

        // 1. Xabar yuborish
        MvcResult sendResult = mvc.perform(post("/api/chat/public")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Bu o'chiriladi\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        String messageId = JsonPath.read(sendResult.getResponse().getContentAsString(), "$.data.id");

        // 2. O'chirish
        mvc.perform(delete("/api/chat/messages/" + messageId)
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ==================== REPLY ====================

    @Test
    @DisplayName("Xabarga javob berish (reply)")
    void testReplyToMessage() throws Exception {
        String token = bearer("elshod");

        // 1. Asl xabar
        MvcResult originalResult = mvc.perform(post("/api/chat/public")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Asl xabar\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        String originalId = JsonPath.read(originalResult.getResponse().getContentAsString(), "$.data.id");

        // 2. Javob berish
        mvc.perform(post("/api/chat/public")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Bu javob!\",\"replyToId\":\"" + originalId + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.content").value("Bu javob!"))
                .andExpect(jsonPath("$.data.replyTo.id").value(originalId))
                .andExpect(jsonPath("$.data.replyTo.content").value("Asl xabar"));
    }

    // ==================== QIDIRUV ====================

    @Test
    @DisplayName("Umumiy chatdan xabar qidirish")
    void testSearchPublicMessages() throws Exception {
        String token = bearer("elshod");

        // 1. Xabarlar yuborish
        mvc.perform(post("/api/chat/public")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Java dasturlash tili\"}"))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/chat/public")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Python dasturlash tili\"}"))
                .andExpect(status().isCreated());

        // 2. "java" so'zini qidirish
        mvc.perform(get("/api/chat/public/search")
                        .header("Authorization", token)
                        .param("q", "java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].content").value("Java dasturlash tili"));
    }

    // ==================== FOYDALANUVCHILAR ====================

    @Test
    @DisplayName("Chat foydalanuvchilarini olish va online endpoint")
    void testGetUsersAndOnline() throws Exception {
        String token = bearer("elshod");

        // 1. Foydalanuvchilar ro'yxati
        mvc.perform(get("/api/chat/users")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());

        // 2. Online foydalanuvchilar
        mvc.perform(get("/api/chat/online")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ==================== SUHBATLAR RO'YXATI ====================

    @Test
    @DisplayName("DM suhbatlar ro'yxatini olish")
    void testGetConversations() throws Exception {
        String elshodToken = bearer("elshod");
        String xusanId = getUserId("xusan");

        // 1. DM yuborish
        mvc.perform(post("/api/chat/direct")
                        .header("Authorization", elshodToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recipientId\":\"" + xusanId + "\",\"content\":\"Test DM\"}"))
                .andExpect(status().isCreated());

        // 2. Suhbatlar ro'yxati
        mvc.perform(get("/api/chat/conversations")
                        .header("Authorization", elshodToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }
}
