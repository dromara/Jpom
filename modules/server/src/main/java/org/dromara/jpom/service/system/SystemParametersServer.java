/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.service.system;

import cn.hutool.core.util.ReflectUtil;
import cn.keepbx.jpom.model.BaseJsonModel;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.model.data.SystemParametersModel;
import org.dromara.jpom.service.h2db.BaseDbService;
import org.springframework.stereotype.Service;

import java.util.function.Function;

/**
 * @author bwcx_jzy
 * @since 2021/12/2
 */
@Service
@Slf4j
public class SystemParametersServer extends BaseDbService<SystemParametersModel> {


    /**
     * 先尝试更新，更新失败尝试插入
     *
     * @param name      参数名称
     * @param jsonModel 参数值
     * @param desc      描述
     */
    public void upsert(String name, BaseJsonModel jsonModel, String desc) {
        SystemParametersModel systemParametersModel = new SystemParametersModel();
        systemParametersModel.setId(name);
        systemParametersModel.setValue(jsonModel.toJson().toString());
        systemParametersModel.setDescription(desc);
        super.upsert(systemParametersModel);
    }

    /**
     * 先尝试更新，更新失败尝试插入
     *
     * @param name 参数名称
     * @param data 参数值
     * @param desc 描述
     */
    public void upsert(String name, Object data, String desc) {
        SystemParametersModel systemParametersModel = new SystemParametersModel();
        systemParametersModel.setId(name);
        systemParametersModel.setValue(JSONObject.toJSONString(data));
        systemParametersModel.setDescription(desc);
        super.upsert(systemParametersModel);
    }

    /**
     * 查询 系统参数 值
     *
     * @param name 参数名称
     * @param cls  类
     * @param <T>  泛型
     * @return data
     */
    public <T> T getConfig(String name, Class<T> cls) {
        return this.getConfig(name, cls, null);
    }

    /**
     * 查询 系统参数 值
     *
     * @param name  参数名称
     * @param cls   类
     * @param mapTo 回调
     * @param <T>   泛型
     * @return data
     */
    public <T> T getConfig(String name, Class<T> cls, Function<T, T> mapTo) {
        SystemParametersModel parametersModel = super.getByKey(name);
        if (parametersModel == null) {
            return null;
        }
        T jsonToBean = parametersModel.jsonToBean(cls);
        if (mapTo == null) {
            return jsonToBean;
        }
        return mapTo.apply(jsonToBean);
    }

    /**
     * 查询系统参数值,没有数据创建一个空对象
     *
     * @param name 参数名称
     * @param cls  类
     * @param <T>  泛型
     * @return data
     */
    public <T> T getConfigDefNewInstance(String name, Class<T> cls) {
        T config;
        try {
            config = this.getConfig(name, cls);
        } catch (Exception e) {
            log.error(I18nMessageUtil.get("i18n.read_system_parameter_exception.ee72"), e);
            return ReflectUtil.newInstance(cls);
        }
        return config == null ? ReflectUtil.newInstance(cls) : config;
    }
}
