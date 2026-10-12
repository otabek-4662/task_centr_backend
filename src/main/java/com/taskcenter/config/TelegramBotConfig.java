package com.taskcenter.config;

import com.taskcenter.service.TelegramBotService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.BotSession;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "telegram.bot.enabled", havingValue = "true")
public class TelegramBotConfig implements DisposableBean {

    public static final long TELEGRAM_BOT_LEADER_LOCK_ID = 884729104719283741L;

    @Value("${telegram.bot.webhook-url:}")
    private String webhookUrl;

    @Value("${telegram.bot.webhook-secret:}")
    private String webhookSecret;

    private final DataSource dataSource;
    private final TelegramBotService telegramBotService;

    private volatile Connection leaderConnection;
    private volatile BotSession botSession;
    private ScheduledExecutorService leaderScheduler;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);

    public TelegramBotConfig() {
        this.dataSource = null;
        this.telegramBotService = null;
    }

    @Autowired
    public TelegramBotConfig(
            @Autowired(required = false) DataSource dataSource,
            @Lazy @Autowired(required = false) TelegramBotService telegramBotService) {
        this.dataSource = dataSource;
        this.telegramBotService = telegramBotService;
    }

    @Bean
    public TelegramBotsApi telegramBotsApi(TelegramBotService telegramBotService) {
        if (webhookUrl != null && !webhookUrl.isBlank()) {
            log.info("Telegram Bot Webhook rejimida: {}. Long-polling (DefaultBotSession) o'chirildi (409 Conflict bartaraf etildi).", webhookUrl);
            try {
                SetWebhook.SetWebhookBuilder builder = SetWebhook.builder().url(webhookUrl.trim());
                if (webhookSecret != null && !webhookSecret.isBlank()) {
                    builder.secretToken(webhookSecret.trim());
                }
                telegramBotService.execute(builder.build());
                log.info("Telegram Bot Webhook muvaffaqiyatli o'rnatildi.");
            } catch (TelegramApiException e) {
                log.warn("Telegram Webhook o'rnatishda xatolik: {}", e.getMessage());
            }
            return null;
        }

        // Long-polling rejimida bot ro'yxatdan o'tishi leader gate orqali amalga oshiriladi
        return null;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (webhookUrl != null && !webhookUrl.isBlank()) {
            // Webhook allaqachon telegramBotsApi metodida sozlangan
            return;
        }

        if (dataSource == null || telegramBotService == null) {
            log.warn("TelegramBotConfig: DataSource yoki TelegramBotService mavjud emas, leader gate boshlanmadi.");
            return;
        }

        startLeaderGate();
    }

    public synchronized void startLeaderGate() {
        if (shuttingDown.get() || running.get()) {
            return;
        }
        running.set(true);

        leaderScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "telegram-bot-leader-gate");
            t.setDaemon(true);
            return t;
        });

        // Darhol yoki har 5 soniyada leader lock olishga urinish
        leaderScheduler.scheduleWithFixedDelay(this::tryAcquireLeaderAndRegisterBot, 0, 5, TimeUnit.SECONDS);
    }

    private void tryAcquireLeaderAndRegisterBot() {
        if (shuttingDown.get()) {
            return;
        }

        if (botSession != null && botSession.isRunning()) {
            // Allaqachon leader sifatida ishlamoqda
            return;
        }

        Connection conn = null;
        try {
            conn = dataSource.getConnection();
            boolean acquired = false;
            try (PreparedStatement ps = conn.prepareStatement("SELECT pg_try_advisory_lock(?)")) {
                ps.setLong(1, TELEGRAM_BOT_LEADER_LOCK_ID);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        acquired = rs.getBoolean(1);
                    }
                }
            }

            if (acquired) {
                log.info("Telegram Bot PostgreSQL leader lock muvaffaqiyatli olindi (lockId={}). Bot ro'yxatdan o'tkazilmoqda...", TELEGRAM_BOT_LEADER_LOCK_ID);
                this.leaderConnection = conn;

                try {
                    TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
                    this.botSession = botsApi.registerBot(telegramBotService);
                    log.info("Telegram Bot Long-Polling rejimida leader sifatida ro'yxatdan o'tdi: @{}", telegramBotService.getBotUsername());
                } catch (TelegramApiException e) {
                    log.error("Telegram botni ro'yxatdan o'tkazishda xatolik (ehtimol 409 Conflict): {}. Lock bo'shatilib, backoff bilan qayta uriniladi.", e.getMessage());
                    releaseLeaderLock();
                }
            } else {
                // Boshqa instance (masalan, deploy paytidagi eski konteyner) botni ushlab turibdi
                log.info("Telegram Bot leader lock boshqa instance tomonidan band. Yangi instance standby rejimida kutmoqda (5s dan so'ng qayta tekshiriladi)...");
                conn.close();
            }
        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception ignored) {
                }
            }
            if (!shuttingDown.get()) {
                log.warn("Telegram Bot leader lock tekshirishda xatolik: {}", e.getMessage());
            }
        }
    }

    private synchronized void releaseLeaderLock() {
        if (leaderConnection != null) {
            try {
                try (PreparedStatement ps = leaderConnection.prepareStatement("SELECT pg_advisory_unlock(?)")) {
                    ps.setLong(1, TELEGRAM_BOT_LEADER_LOCK_ID);
                    ps.executeQuery();
                }
            } catch (Exception e) {
                log.warn("pg_advisory_unlock xatosi: {}", e.getMessage());
            } finally {
                try {
                    leaderConnection.close();
                } catch (Exception ignored) {
                }
                leaderConnection = null;
            }
        }
    }

    @Override
    public synchronized void destroy() {
        log.info("TelegramBotConfig graceful stop boshlandi...");
        shuttingDown.set(true);

        if (leaderScheduler != null) {
            leaderScheduler.shutdownNow();
            leaderScheduler = null;
        }

        if (botSession != null) {
            try {
                log.info("Telegram BotSession to'xtatilmoqda...");
                botSession.stop();
            } catch (Exception e) {
                log.warn("Telegram BotSession to'xtatishda xatolik: {}", e.getMessage());
            }
            botSession = null;
        }

        releaseLeaderLock();
        log.info("TelegramBotConfig muvaffaqiyatli to'xtatildi (advisory lock va ulanish bo'shatildi).");
    }

    public BotSession getBotSession() {
        return botSession;
    }

    public boolean isLeader() {
        return leaderConnection != null && botSession != null && botSession.isRunning();
    }
}
