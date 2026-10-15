package com.taskcenter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync(proxyTargetClass = true)
@EnableCaching
@EnableScheduling
public class BackendApplication {
	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BackendApplication.class);

	public static void main(String[] args) {
		String rawUrl = System.getenv("SPRING_DATASOURCE_URL");
		if (rawUrl == null || rawUrl.isBlank()) {
			rawUrl = System.getenv("DATABASE_URL");
		}
		if (rawUrl != null && (rawUrl.startsWith("postgres://") || rawUrl.startsWith("postgresql://"))) {
			com.taskcenter.util.DatabaseUrlNormalizer.NormalizedConfig normalized =
					com.taskcenter.util.DatabaseUrlNormalizer.normalize(
							rawUrl,
							System.getenv("SPRING_DATASOURCE_USERNAME"),
							System.getenv("SPRING_DATASOURCE_PASSWORD")
					);
			if (normalized.jdbcUrl() != null) {
				System.setProperty("spring.datasource.url", normalized.jdbcUrl());
				System.setProperty("SPRING_DATASOURCE_URL", normalized.jdbcUrl());
			}
			if (normalized.username() != null) {
				System.setProperty("spring.datasource.username", normalized.username());
				System.setProperty("SPRING_DATASOURCE_USERNAME", normalized.username());
			}
			if (normalized.password() != null) {
				System.setProperty("spring.datasource.password", normalized.password());
				System.setProperty("SPRING_DATASOURCE_PASSWORD", normalized.password());
			}
			log.info("Render DB configured: host={}, db={}", normalized.host(), normalized.dbName());
		}
		SpringApplication.run(BackendApplication.class, args);
	}

	@org.springframework.context.annotation.Bean
	public java.time.Clock clock() {
		return java.time.Clock.system(java.time.ZoneId.of("Asia/Tashkent"));
	}
}
