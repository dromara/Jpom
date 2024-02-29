/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package git;

import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.plugin.GitProcessFactory;
import org.junit.Test;

/**
 * <br>
 * Created By Hong on 2023/3/31
 *
 * @author Hong
 **/
@Slf4j
public class TestGitEnv {

    @Test
    public void test() {
        log.info("系统中是否存在GIT环境：{}", GitProcessFactory.existsSystemGit());
    }

}
