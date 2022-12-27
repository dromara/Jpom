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
