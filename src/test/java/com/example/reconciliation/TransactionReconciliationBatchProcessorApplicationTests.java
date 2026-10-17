package com.example.reconciliation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "app.batch.launch-on-startup=false")
class TransactionReconciliationBatchProcessorApplicationTests {

	@Test
	void contextLoads() {
	}

	@Test
	void mainShouldRunSpringApplication() {
		try (MockedStatic<SpringApplication> mocked = mockStatic(SpringApplication.class)) {

			assertDoesNotThrow(() -> TransactionReconciliationBatchProcessorApplication.main(new String[] {}));

			mocked.verify(() -> SpringApplication.run(TransactionReconciliationBatchProcessorApplication.class,
					new String[] {}));
		}
	}

}
