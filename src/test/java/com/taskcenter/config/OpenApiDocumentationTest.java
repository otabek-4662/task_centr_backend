package com.taskcenter.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiDocumentationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("OpenAPI JSON v3/api-docs muvaffaqiyatli generatsiya bo'lishi va barcha talablarga mosligi")
    void testOpenApiDocumentationIntegrity() throws Exception {
        MvcResult result = mockMvc.perform(get("/v3/api-docs/0-all"))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        assertThat(content).isNotBlank();

        JsonNode root = objectMapper.readTree(content);

        // 1. Info va OpenAPI versiyasi
        assertThat(root.has("openapi")).isTrue();
        assertThat(root.path("info").path("title").asText()).isEqualTo("Task Center API");
        assertThat(root.path("info").path("version").asText()).isEqualTo("2.0.0");

        // 2. Components xavfsizlik va sxemalar
        JsonNode components = root.path("components");
        assertThat(components.has("securitySchemes")).isTrue();
        assertThat(components.path("securitySchemes").has("bearerAuth")).isTrue();
        assertThat(components.path("schemas").has("ErrorResponse")).isTrue();
        assertThat(components.path("schemas").has("PageResponse")).isTrue();

        // 3. Paths tekshiruvi: operationId unikal bo'lishi va kamida 1 ta response bo'lishi
        JsonNode paths = root.path("paths");
        assertThat(paths.isObject()).isTrue();
        assertThat(paths.size()).isGreaterThan(10);

        Set<String> operationIds = new HashSet<>();
        List<String> duplicateOperationIds = new ArrayList<>();
        List<String> missingOperationIds = new ArrayList<>();
        List<String> operationsWithoutResponse = new ArrayList<>();

        Iterator<Map.Entry<String, JsonNode>> fields = paths.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> pathEntry = fields.next();
            String path = pathEntry.getKey();
            JsonNode pathNode = pathEntry.getValue();

            Iterator<Map.Entry<String, JsonNode>> methodFields = pathNode.fields();
            while (methodFields.hasNext()) {
                Map.Entry<String, JsonNode> methodEntry = methodFields.next();
                String httpMethod = methodEntry.getKey().toUpperCase();

                // Faqat HTTP metodlar (parameters, $ref va h.k. emas)
                if (!List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD").contains(httpMethod)) {
                    continue;
                }

                JsonNode operation = methodEntry.getValue();

                // operationId tekshiruvi
                if (!operation.has("operationId") || operation.path("operationId").asText().isBlank()) {
                    missingOperationIds.add(httpMethod + " " + path);
                } else {
                    String opId = operation.path("operationId").asText();
                    if (!operationIds.add(opId)) {
                        duplicateOperationIds.add(opId + " at " + httpMethod + " " + path);
                    }
                }

                // Responses tekshiruvi (kamida bitta response bo'lishi kerak)
                JsonNode responses = operation.path("responses");
                if (responses.isMissingNode() || responses.isEmpty()) {
                    operationsWithoutResponse.add(httpMethod + " " + path);
                }
            }
        }

        assertThat(missingOperationIds)
                .as("Barcha endpointlarda operationId ko'rsatilgan bo'lishi shart")
                .isEmpty();

        assertThat(duplicateOperationIds)
                .as("operationId lar takrorlanmas va unikal bo'lishi shart")
                .isEmpty();

        assertThat(operationsWithoutResponse)
                .as("Har bir endpointda kamida bitta response sxemasi bo'lishi shart")
                .isEmpty();

        // docs/openapi.json va docs/endpoints_summary.md fayliga saqlash
        try {
            java.nio.file.Path docsDir = java.nio.file.Paths.get("docs");
            if (!java.nio.file.Files.exists(docsDir)) {
                java.nio.file.Files.createDirectories(docsDir);
            }
            java.nio.file.Files.writeString(docsDir.resolve("openapi.json"), content, java.nio.charset.StandardCharsets.UTF_8);

            StringBuilder md = new StringBuilder();
            md.append("# OpenAPI Hujjati — Barcha Endpointlar va OperationId lar Ro'yxati\n\n");
            md.append("| # | Tag | HTTP Metod | Endpoint URL | OperationId | Tavsif | Holati |\n");
            md.append("|---|---|---|---|---|---|---|\n");

            int idx = 1;
            Iterator<Map.Entry<String, JsonNode>> pFields = paths.fields();
            List<String[]> rows = new ArrayList<>();
            while (pFields.hasNext()) {
                Map.Entry<String, JsonNode> pEntry = pFields.next();
                String path = pEntry.getKey();
                JsonNode pNode = pEntry.getValue();

                Iterator<Map.Entry<String, JsonNode>> mFields = pNode.fields();
                while (mFields.hasNext()) {
                    Map.Entry<String, JsonNode> mEntry = mFields.next();
                    String method = mEntry.getKey().toUpperCase();
                    if (!List.of("GET", "POST", "PUT", "PATCH", "DELETE").contains(method)) continue;

                    JsonNode op = mEntry.getValue();
                    String tag = op.path("tags").isArray() && op.path("tags").size() > 0 ? op.path("tags").get(0).asText() : "-";
                    String opId = op.path("operationId").asText("-");
                    String summary = op.path("summary").asText("-");
                    boolean deprecated = op.path("deprecated").asBoolean(false);
                    String status = deprecated ? "⚠️ Deprecated" : "✅ Faol";

                    rows.add(new String[]{tag, method, path, opId, summary, status});
                }
            }

            // Tag va Path bo'yicha tartiblash
            rows.sort(Comparator.comparing((String[] r) -> r[0]).thenComparing(r -> r[2]));

            for (String[] r : rows) {
                md.append(String.format("| %d | %s | `%s` | `%s` | `%s` | %s | %s |\n",
                        idx++, r[0], r[1], r[2], r[3], r[4], r[5]));
            }

            java.nio.file.Files.writeString(docsDir.resolve("endpoints_summary.md"), md.toString(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            // e.printStackTrace();
        }
    }

    @Test
    @DisplayName("App guruhidagi OpenAPI JSON faol endpointlarni o'z ichiga oladi va @Deprecated larni inkor etadi")
    void testAppOpenApiExcludesDeprecated() throws Exception {
        MvcResult result = mockMvc.perform(get("/v3/api-docs/app"))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        assertThat(content).isNotBlank();

        JsonNode root = objectMapper.readTree(content);
        JsonNode paths = root.path("paths");
        assertThat(paths.isObject()).isTrue();
        assertThat(paths.size()).isGreaterThan(0);

        Iterator<Map.Entry<String, JsonNode>> pFields = paths.fields();
        while (pFields.hasNext()) {
            JsonNode pathNode = pFields.next().getValue();
            Iterator<Map.Entry<String, JsonNode>> mFields = pathNode.fields();
            while (mFields.hasNext()) {
                JsonNode op = mFields.next().getValue();
                assertThat(op.path("deprecated").asBoolean(false))
                        .as("App guruhida @Deprecated endpoint bo'lmasligi kerak")
                        .isFalse();
            }
        }
    }

    @Test
    @DisplayName("Legacy guruhidagi OpenAPI JSON barcha endpointlarni o'z ichiga oladi")
    void testLegacyOpenApiIncludesAll() throws Exception {
        MvcResult result = mockMvc.perform(get("/v3/api-docs/legacy"))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        assertThat(content).isNotBlank();

        JsonNode root = objectMapper.readTree(content);
        JsonNode paths = root.path("paths");
        assertThat(paths.isObject()).isTrue();
        assertThat(paths.size()).isGreaterThan(10);
    }
}
