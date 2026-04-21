package com.healthlens.api;

import com.healthlens.api.support.PostgresTestContainerBase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class HealthLensApplicationTests extends PostgresTestContainerBase {

	@Test
	void contextLoads() {
	}
}
