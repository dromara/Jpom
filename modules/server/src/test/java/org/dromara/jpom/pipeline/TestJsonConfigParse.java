package org.dromara.jpom.pipeline;

import cn.hutool.core.util.ReflectUtil;
import com.alibaba.fastjson2.JSONObject;
import org.dromara.jpom.build.pipeline.actuator.ActuatorFactory;
import org.dromara.jpom.build.pipeline.actuator.PipelineItemActuator;
import org.dromara.jpom.build.pipeline.enums.StageType;
import org.dromara.jpom.build.pipeline.config.StageGroup;
import org.dromara.jpom.build.pipeline.config.IStage;
import org.dromara.jpom.build.pipeline.config.PipelineConfig;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author bwcx_jzy
 * @since 2024/4/10
 */
public class TestJsonConfigParse {
    String json = "{\n" +
        "            \"repositories\": {\n" +
        "            \"6F7T\": {\n" +
        "                \"sort\": 0,\n" +
        "                    \"repositoryId\": \"1f8f8d83723140918c9249b1e39f80ec\",\n" +
        "                    \"branchName\": \"master\",\n" +
        "                    \"branchTagName\": \"\"\n" +
        "            },\n" +
        "            \"ZRFN\": {\n" +
        "                \"sort\": 1,\n" +
        "                    \"repositoryId\": \"08e47feae17a45bc9d9fbf8c049654ed\",\n" +
        "                    \"branchName\": \"trunk\",\n" +
        "                    \"branchTagName\": \"\"\n" +
        "            }\n" +
        "        },\n" +
        "            \"stageGroups\": [{\n" +
        "            \"name\": \"default_steps_nzfn\",\n" +
        "                \"stages\": [{\n" +
        "                \"description\": \"子流程1\",\n" +
        "                    \"stageType\": \"EXEC\",\n" +
        "                    \"env\": {\n" +
        "                    \"111\": \"1111111\",\n" +
        "                        \"11111\": \"3333333333\"\n" +
        "                },\n" +
        "                \"repoTag\": \"6F7T\",\n" +
        "                    \"commands\": \"ee\"\n" +
        "            }],\n" +
        "            \"description\": \"打包编译\"\n" +
        "        }, {\n" +
        "            \"name\": \"default_steps_m5ef\",\n" +
        "                \"stages\": [{\n" +
        "                \"description\": \"子流程1\",\n" +
        "                    \"stageType\": \"PUBLISH\",\n" +
        "                    \"repoTag\": \"6F7T\",\n" +
        "                    \"subStageType\": \"PUBLISH_PROJECT\",\n" +
        "                    \"nodeId\": \"18c043e7772f4603972a8fc80a2189ba\",\n" +
        "                    \"projectId\": \"test-copy\",\n" +
        "                    \"artifacts\": [{\n" +
        "                    \"path\": [\"/\"]\n" +
        "                }]\n" +
        "            }],\n" +
        "            \"description\": \"发布文件\"\n" +
        "        }],\n" +
        "            \"version\": \"1.0.0\"\n" +
        "        }";

    @Test
    public void test2() {
        PipelineConfig pipelineConfig = PipelineConfig.fromJson(json);
        pipelineConfig.verify("");
        PipelineItemActuator resolve = ActuatorFactory.resolve(pipelineConfig);
        resolve.exec();
        System.out.println(resolve);
    }

    @Test
    public void test1() {
        PipelineConfig pipelineConfig = PipelineConfig.fromJson(json);
        pipelineConfig.verify("");
        System.out.println(pipelineConfig);
    }

    @Test
    public void test() {

        JSONObject jsonObject = JSONObject.parseObject(json);
//        jsonObject.getString()
        PipelineConfig pipelineConfig = jsonObject.to(PipelineConfig.class);
        List<StageGroup> stageGroups = pipelineConfig.getStageGroups();
        stageGroups.forEach(new Consumer<StageGroup>() {
            @Override
            public void accept(StageGroup stageGroup) {
                System.out.println(stageGroup);
                List<IStage> stages = stageGroup.getStages();
                stages.forEach(new Consumer<IStage>() {
                    @Override
                    public void accept(IStage stage) {
                        //StageExecCommand stageExecCommand = (StageExecCommand) stage.to();
                        //String stageType1 = stage.getString("stageType");
                        //Object invoke = ReflectUtil.invoke(stage, "getString", "stageType");
                        StageType stageType = stage.getStageType();
                        JSONObject from = JSONObject.from(stage);
//                        from.to()
                        //JSONObject json1 = stage.toJson();
                        Class<? extends IStage> aClass = stage.getClass();
                        Method[] methods = ReflectUtil.getMethods(stage.getClass());
                        //Method getStageType = ReflectUtil.getMethodByName(aClass, "getString", String.class);
                        //Object invoke = ReflectUtil.invoke(stage, getStageType);
                        // StageType stageType = stage.getStageType();
                        //stage.verify();
                        // System.out.println(stage);
                    }
                });
            }
        });
        System.out.println(pipelineConfig);
    }
}
