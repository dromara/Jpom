package io.jpom.service.script;

import cn.hutool.core.io.FileUtil;
import io.jpom.common.BaseOperService;
import io.jpom.model.data.ScriptModel;
import io.jpom.system.AgentConfigBean;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 脚本模板管理
 *
 * @author jiangzeyin
 * @date 2019/4/24
 */
@Service
public class ScriptServer extends BaseOperService<ScriptModel> {

    public ScriptServer() {
        super(AgentConfigBean.SCRIPT);
    }

    @Override
    public List<ScriptModel> list() {
        List<ScriptModel> scriptModels = super.list();
        if (scriptModels == null) {
            return null;
        }
        // 读取文件内容
        scriptModels.forEach(ScriptModel::readFileTime);
        return scriptModels;
    }

    @Override
    public ScriptModel getItem(String id) {
        ScriptModel scriptModel = super.getItem(id);
        if (scriptModel != null) {
            scriptModel.readFileContext();
        }
        return scriptModel;
    }

    @Override
    public void addItem(ScriptModel scriptModel) {
        super.addItem(scriptModel);
        scriptModel.saveFile();
    }

    @Override
    public void updateItem(ScriptModel scriptModel) {
        super.updateItem(scriptModel);
        scriptModel.saveFile();
    }

    @Override
    public void deleteItem(String id) {
        ScriptModel scriptModel = getItem(id);
        if (scriptModel != null) {
            FileUtil.del(scriptModel.getFile(true).getParentFile());
        }
        super.deleteItem(id);
    }
}
