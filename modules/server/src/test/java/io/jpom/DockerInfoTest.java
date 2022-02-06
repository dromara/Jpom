package io.jpom;

import io.jpom.model.docker.DockerInfoModel;
import io.jpom.service.docker.DockerInfoService;
import io.jpom.system.init.InitDb;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author bwcx_jzy
 * @since 2022/2/6
 */
@SpringBootTest(classes = {JpomServerApplication.class, InitDb.class})
@AutoConfigureMockMvc
public class DockerInfoTest {

	@Resource
	private DockerInfoService dockerInfoService;

	@Test
	public void testQueryTag() {
		List<DockerInfoModel> models = dockerInfoService.queryByTag("sdfsd");
		System.out.println(models);
	}
}
