package io.jpom;

import io.jpom.system.init.InitDb;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * @author Hotstrip
 * Test application start, then you can use such as service instance to test your methods
 */
@SpringBootTest(classes = {JpomServerApplication.class, InitDb.class})
@AutoConfigureMockMvc
public class ApplicationStartTest {
	protected  static Logger logger = LoggerFactory.getLogger(ApplicationStartTest.class);
	@Autowired
	protected MockMvc mockMvc;

	@Test
	public void testApplicationStart() {
		logger.info("Jpom Server Application started.....");
	}
}
