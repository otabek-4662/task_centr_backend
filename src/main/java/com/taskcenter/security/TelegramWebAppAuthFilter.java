package com.taskcenter.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskcenter.dto.ApiResponse;
import com.taskcenter.model.User;
import com.taskcenter.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

/**
 * Telegram Mini App (TMA) dan kelayotgan initData ma'lumotlarini
 * HMAC-SHA256 algoritmi orqali tekshirib, foydalanuvchini autentifikatsiya
 * qiluvchi Spring Security filtri.
 *
 * initData manbalari:
 * 1. "X-Telegram-Init-Data" sarlavhasi (Header)
 * 2. "Authorization: tma <initData>" sarlavhasi
 * 3. So'rov parametri: "?initData=..."
 */
@Component
public class TelegramWebAppAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TelegramWebAppAuthFilter.class);

    public static final String HEADER_INIT_DATA = "X-Telegram-Init-Data";
    public static final String AUTH_PREFIX_TMA = "tma ";
    public static final String ATTR_TELEGRAM_USER_ID = "telegram_user_id";
    public static final String ATTR_TELEGRAM_PARAMS = "telegram_params";

    private final TelegramInitDataValidator validator;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public TelegramWebAppAuthFilter(TelegramInitDataValidator validator,
                                    UserRepository userRepository,
                                    ObjectMapper objectMapper) {
        this.validator = validator;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String initData = extractInitData(request);

        // Agar so'rovda Telegram initData berilmagan bo'lsa, keyingi filtrlarga (masalan, JWT ga) uzatamiz
        if (!StringUtils.hasText(initData)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Optional<Map<String, String>> validatedParamsOpt = validator.validate(initData);

            if (validatedParamsOpt.isEmpty()) {
                log.warn("Telegram WebApp initData HMAC-SHA256 tekshiruvidan o'tmadi yoki muddati tugagan: URI={}", request.getRequestURI());
                rejectUnauthorized(response, "Invalid or expired Telegram WebApp initData");
                return;
            }

            Map<String, String> params = validatedParamsOpt.get();
            String userJson = params.get("user");

            if (!StringUtils.hasText(userJson)) {
                log.warn("Telegram initData ichida user ma'lumotlari mavjud emas");
                rejectUnauthorized(response, "Telegram user data is missing in initData");
                return;
            }

            JsonNode userNode = objectMapper.readTree(userJson);
            long telegramId = userNode.path("id").asLong();

            request.setAttribute(ATTR_TELEGRAM_USER_ID, telegramId);
            request.setAttribute(ATTR_TELEGRAM_PARAMS, params);

            // Agar allaqachon autentifikatsiya qilinmagan bo'lsa, bazadan userni topib SecurityContext ga joylaymiz
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                Optional<User> userOpt = userRepository.findByTelegramChatId(telegramId);

                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            user, null, user.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("Telegram WebApp orqali muvaffaqiyatli autentifikatsiya qilindi: user={}, telegramId={}",
                            user.getName(), telegramId);
                } else {
                    log.warn("Telegram ID ({}) ga bog'langan foydalanuvchi topilmadi", telegramId);
                    // Agar endpoint Telegram orqali login/link qilish uchun bo'lsa, davom etishga ruxsat berish mumkin,
                    // ammo himoyalangan resurslar uchun 401 qaytariladi
                    rejectUnauthorized(response, "Telegram hisob tizim foydalanuvchisiga ulanmagan");
                    return;
                }
            }

            filterChain.doFilter(request, response);

        } catch (Exception e) {
            log.error("Telegram WebApp autentifikatsiyasini tekshirishda xatolik: {}", e.getMessage(), e);
            rejectUnauthorized(response, "Telegram WebApp autentifikatsiya xatosi: " + e.getMessage());
        }
    }

    private String extractInitData(HttpServletRequest request) {
        // 1. X-Telegram-Init-Data sarlavhasi
        String header = request.getHeader(HEADER_INIT_DATA);
        if (StringUtils.hasText(header)) {
            return header.trim();
        }

        // 2. Authorization: tma <initData>
        String authHeader = request.getHeader("Authorization");
        if (StringUtils.hasText(authHeader) && authHeader.toLowerCase().startsWith(AUTH_PREFIX_TMA)) {
            return authHeader.substring(AUTH_PREFIX_TMA.length()).trim();
        }

        // 3. So'rov parametri (query param)
        String param = request.getParameter("initData");
        if (StringUtils.hasText(param)) {
            return param.trim();
        }

        return null;
    }

    private void rejectUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiResponse<Void> apiResponse = ApiResponse.error("UNAUTHORIZED", message, HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}
