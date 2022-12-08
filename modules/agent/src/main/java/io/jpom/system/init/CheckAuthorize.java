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
package io.jpom.system.init;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import io.jpom.common.JpomManifest;
import io.jpom.system.AgentAuthorize;
import io.jpom.system.ConfigBean;
import io.jpom.system.ExtConfigBean;
import io.jpom.util.JvmUtil;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

/**
 * 检查授权信息
 *
 * @author jiangzeyin
 * @since 2019/4/17
 */
//@PreLoadClass(value = Integer.MIN_VALUE)
@Configuration
public class CheckAuthorize implements InitializingBean {

    //    @PreLoadMethod
    private void checkAgentAuthorize() {
        AgentAuthorize.getInstance();
    }

    //    @PreLoadMethod
    private void checkJps() {
        //boolean exist = JvmUtil.exist(JpomManifest.getInstance().getPid());
        //JvmUtil.setJpsNormal(exist);
        JvmUtil.checkJpsNormal();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.checkJps();
        this.checkAgentAuthorize();
    }
}
