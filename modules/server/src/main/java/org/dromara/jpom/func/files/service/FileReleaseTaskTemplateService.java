/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.func.files.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.alibaba.fastjson2.JSONObject;
import org.dromara.jpom.func.files.model.FileReleaseTaskTemplate;
import org.dromara.jpom.func.files.model.IFileStorage;
import org.dromara.jpom.service.h2db.BaseWorkspaceService;
import org.springframework.stereotype.Service;

/**
 * @author bwcx_jzy
 * @since 2025/1/9
 */
@Service
public class FileReleaseTaskTemplateService extends BaseWorkspaceService<FileReleaseTaskTemplate> {

    /**
     * 添加模板
     *
     * @param type        类型
     * @param storage     文件对象
     * @param fileType    文件类型
     * @param data        数据
     * @param workspaceId 工作空间id
     */
    public void add(String type, String workspaceId, IFileStorage storage, Integer fileType, JSONObject data) {
        String templateTag;
        String nameTag;
        switch (type) {
            case "id":
                templateTag = "id:" + storage.getId();
                nameTag = "ID";
                break;
            case "alias":
                String aliasCode = storage.getAliasCode();
                if (StrUtil.isEmpty(aliasCode)) {
                    // 没有别名码，默认使用 id
                    templateTag = "id:" + storage.getId();
                    nameTag = "ID";
                } else {
                    templateTag = "alias:" + aliasCode;
                    nameTag = "别名";
                }
                break;
            default:
                return;
        }
        // 生成模板id。需要关联工作空间id，避免跨工作空间别名码重复
        String dataId = SecureUtil.sha1(fileType + templateTag + workspaceId);
        FileReleaseTaskTemplate fileReleaseTaskTemplate = new FileReleaseTaskTemplate();
        fileReleaseTaskTemplate.setId(dataId);
        fileReleaseTaskTemplate.setFileType(fileType);
        fileReleaseTaskTemplate.setName(storage.getName() + "-" + nameTag + "发布模板");
        fileReleaseTaskTemplate.setData(data.toJSONString());
        fileReleaseTaskTemplate.setTemplateTag(templateTag);
        this.upsert(fileReleaseTaskTemplate);
    }
}
