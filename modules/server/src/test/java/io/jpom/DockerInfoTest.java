/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.jpom;

import io.jpom.common.Const;
import io.jpom.model.docker.DockerInfoModel;
import io.jpom.service.docker.DockerInfoService;
import org.junit.jupiter.api.Test;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author bwcx_jzy
 * @since 2022/2/6
 */
public class DockerInfoTest extends ApplicationStartTest {

	@Resource
	private DockerInfoService dockerInfoService;

	@Test
	public void testQueryTag() {
		int sdfsd = dockerInfoService.countByTag(Const.WORKSPACE_DEFAULT_ID, "sdfsd");

		System.out.println(sdfsd);
	}

	@Test
	public void testQueryTag2() {
		List<DockerInfoModel> models = dockerInfoService.queryByTag(Const.WORKSPACE_DEFAULT_ID, 1, "sdfsd");
		System.out.println(models);
	}
}
