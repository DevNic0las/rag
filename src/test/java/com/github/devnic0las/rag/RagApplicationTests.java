package com.github.devnic0las.rag;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest
class RagApplicationTests {

	@Bean
	@ServiceConnection
	static PostgreSQLContainer<?> postgresContainer() {
		return new PostgreSQLContainer<>("postgres:16-alpine")
						.withDatabaseName("rag")
						.withUsername("rag")
						.withPassword("rag");
	}

	@Test
	void contextLoads() {

	}

}
