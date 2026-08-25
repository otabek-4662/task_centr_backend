package com.taskcenter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BackendApplication {
	public static void main(String[] args) {
		// Render postgres:// va postgresql:// ni jdbc:postgresql:// ga aylantirish
		// URL ichidagi user:pass ni ham ajratib username/password ga yozamiz
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
				if (atIndex >= 0) {
					String userInfo = withoutProtocol.substring(0, atIndex);
					int colonIdx = userInfo.indexOf(':');
					if (colonIdx >= 0) {
						String user = userInfo.substring(0, colonIdx);
						String pass = userInfo.substring(colonIdx + 1);
						System.setProperty("spring.datasource.username", user);
						System.setProperty("spring.datasource.password", pass);
						System.out.println("Render DB user: " + user);
					}
				}
			} catch (Exception e) {
				System.err.println("DB URL convert xatosi: " + e.getMessage());
			}
		}
		SpringApplication.run(BackendApplication.class, args);
	}
}
