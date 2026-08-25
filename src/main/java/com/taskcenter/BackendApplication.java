package com.taskcenter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BackendApplication {
	public static void main(String[] args) {
		// Render postgres:// va postgresql:// ni jdbc:postgresql:// ga aylantirish
		String url = System.getenv("SPRING_DATASOURCE_URL");
		if (url != null && url.startsWith("postgres")) {
			try {
				int protoEnd = url.indexOf("://");
				String withoutProtocol = protoEnd >= 0 ? url.substring(protoEnd + 3) : url;
				int atIndex = withoutProtocol.indexOf('@');
				String hostPart = atIndex >= 0 ? withoutProtocol.substring(atIndex + 1) : withoutProtocol;
				String jdbcUrl = "jdbc:postgresql://" + hostPart;
				System.setProperty("spring.datasource.url", jdbcUrl);
				System.out.println("Render DB URL -> JDBC: " + jdbcUrl);
			} catch (Exception e) {
				System.err.println("DB URL convert xatosi: " + e.getMessage());
			}
		}
		SpringApplication.run(BackendApplication.class, args);
	}
}
