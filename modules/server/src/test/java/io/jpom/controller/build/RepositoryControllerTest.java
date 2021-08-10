package io.jpom.controller.build;

import io.jpom.ApplicationStartTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;


/**
 * @author Hotstrip
 * Test RepositoryController
 */
public class RepositoryControllerTest extends ApplicationStartTest {

	/**
	 * 测试加载仓库信息列表
	 * @throws Exception
	 */
	@Test
	public void testLoadRepositoryList() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.post("/build/repository/list")
				.contentType("application/json"))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andDo(MockMvcResultHandlers.print());
	}
}
