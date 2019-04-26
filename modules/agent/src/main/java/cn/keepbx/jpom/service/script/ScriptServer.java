package cn.keepbx.jpom.service.script;

import cn.hutool.core.io.FileUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.keepbx.jpom.common.BaseOperService;
import cn.keepbx.jpom.model.data.ScriptModel;
import cn.keepbx.jpom.system.AgentConfigBean;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/**
 * 脚本模板管理
 *
 * @author jiangzeyin
 * @date 2019/4/24
 */
@Service
public class ScriptServer extends BaseOperService<ScriptModel> {

    @Override
    public List<ScriptModel> list() throws IOException {
        JSONObject jsonObject = getJSONObject(AgentConfigBean.SCRIPT);
        if (jsonObject == null) {
            return null;
        }
        JSONArray jsonArray = formatToArray(jsonObject);
        List<ScriptModel> scriptModels = jsonArray.toJavaList(ScriptModel.class);
        if (scriptModels == null) {
            return null;
        }
        // 读取文件内容
        scriptModels.forEach(ScriptModel::readFileTime);
        return scriptModels;
    }

    @Override
    public ScriptModel getItem(String id) {
        ScriptModel scriptModel = getJsonObjectById(AgentConfigBean.SCRIPT, id, ScriptModel.class);
        if (scriptModel != null) {
            scriptModel.readFileContext();
        }
        return scriptModel;
    }

    @Override
    public void addItem(ScriptModel scriptModel) {
        saveJson(AgentConfigBean.SCRIPT, scriptModel.toJson());
        scriptModel.saveFile();
    }

    @Override
    public boolean updateItem(ScriptModel scriptModel) {
        try {
            updateJson(AgentConfigBean.SCRIPT, scriptModel.toJson());
            scriptModel.saveFile();
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error(e.getMessage(), e);
            return false;
        }
        return true;
    }

    @Override
    public void deleteItem(String id) {
        ScriptModel scriptModel = getItem(id);
        if (scriptModel != null) {
            FileUtil.del(scriptModel.getFile(true).getParentFile());
        }
        deleteJson(AgentConfigBean.SCRIPT, id);
    }
}
