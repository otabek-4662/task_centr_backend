package com.taskcenter.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskcenter.dto.ApiResponse;
import com.taskcenter.model.User;
import com.taskcenter.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

/**
 * Spring MVC HandlerInterceptor sifatida Telegram Mini App initData
 * (HMAC-SHA256) ma'lumotlarini tekshiruvchi interceptor.
 */
@Component
public class TelegramWebAppInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(TelegramWebAppInterceptor.class);

    private final TelegramInitDataValidator validator;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public TelegramWebAppInterceptor(TelegramInitDataValidator validator,
                                     UserRepository userRepository,
                                     ObjectMapper objectMapper) {
        this.validator = validator;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String initData = request.getHeader("X-Telegram-Init-Data");
        if (!StringUtils.hasText(initData)) {
            String authHeader = request.getHeader("Authorization");
            if (StringUtils.hasText(authHeader) && authHeader.toLowerCase().startsWith("tma ")) {
                initData = authHeader.substring(4).trim();
            } else {
                initData = request.getParameter("initData");
            }
        }

        if (!StringUtils.hasText(initData)) {
            // Agar so'rovda Telegram auth talab etilsa va initData kelmasa, rad etiladi
            return true;
        }

        Optional<Map<String, String>> paramsOpt = validator.validate(initData);
        if (paramsOpt.isEmpty()) {
            writeError(response, "Invalid or expired Telegram WebApp initData");
            return false;
        }

        Map<String, String> params = paramsOpt.get();
        String userJson = params.get("user");
        if (!StringUtils.hasText(userJson)) {
            writeError(response, "Telegram user data missing in initData");
            return false;
        }

        JsonNode userNode = objectMapper.readTree(userJson);
        long telegramId = userNode.path("id").asLong();

        request.setAttribute("telegram_user_id", telegramId);
        request.setAttribute("telegram_params", params);

        Optional<User> userOpt = userRepository.findByTelegramChatId(telegramId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    user, null, user.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);
            return true;
        } else {
            writeError(response, "Telegram hisob tizim foydalanuvchisiga ulanmagan");
            return false;
        }
    }

    private void writeError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error("UNAUTHORIZED", message, HttpServletResponse.SC_UNAUTHORIZED)));
    }
}
