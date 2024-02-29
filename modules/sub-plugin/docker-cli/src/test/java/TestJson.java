/*
 * Copyright (c) 2019 Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONFactory;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import com.github.dockerjava.api.model.SwarmNodeState;
import com.github.dockerjava.api.model.SwarmNodeStatus;
import org.junit.Test;

/**
 * @author bwcx_jzy
 * @since 2022/12/27
 */
public class TestJson {

    static {
        JSONFactory.setUseJacksonAnnotation(false);
        JSON.config(JSONWriter.Feature.WriteEnumsUsingName);
        System.out.println(JSON.isEnabled(JSONWriter.Feature.WriteEnumsUsingName));
    }

    @Test
    public void test() {
        SwarmNodeStatus swarmNodeStatus = new SwarmNodeStatus();
        swarmNodeStatus.withState(SwarmNodeState.DISCONNECTED);

        System.out.println(JSONObject.toJSONString(swarmNodeStatus));

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("state", SwarmNodeState.DOWN);
        System.out.println(jsonObject);
    }
}
