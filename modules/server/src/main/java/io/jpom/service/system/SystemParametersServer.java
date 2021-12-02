package io.jpom.service.system;

import io.jpom.model.BaseJsonModel;
import io.jpom.model.data.SystemParametersModel;
import io.jpom.service.h2db.BaseDbService;
import org.springframework.stereotype.Service;

/**
 * @author bwcx_jzy
 * @since 2021/12/2
 */
@Service
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
		systemParametersModel.setValue(jsonModel.toString());
		systemParametersModel.setDescription(desc);
		int update = super.update(systemParametersModel);
		if (update <= 0) {
			super.insert(systemParametersModel);
		}
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
		SystemParametersModel parametersModel = super.getByKey(name);
		if (parametersModel == null) {
			return null;
		}
		return parametersModel.jsonToBean(cls);
	}
}
