package io.jpom.service.user;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import io.jpom.model.data.UserModel;
import io.jpom.service.h2db.BaseDbService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author bwcx_jzy
 * @since 2021/12/3
 */
@Service
public class UserService extends BaseDbService<UserModel> {

	/**
	 * 是否需要初始化
	 *
	 * @return true 有系统管理员账号，系统可以正常使用
	 */
	public boolean canUse() {
		UserModel userModel = new UserModel();
		userModel.setSystemUser(1);
		return super.exists(userModel);
	}

	@Override
	protected void fillSelectResult(UserModel data) {
		if (data == null) {
			return;
		}
		data.setSalt(null);
	}

	/**
	 * 验证用户md5
	 *
	 * @param userMd5 用户md5
	 * @return userModel 用户对象
	 */
	public UserModel checkUser(String userMd5) {
		UserModel userModel = new UserModel();
		userModel.setPassword(userMd5);
		return super.queryByBean(userModel);
	}

	/**
	 * 是否返回系统管理员信息
	 *
	 * @param hiddenSystem 隐藏系统管理员
	 * @return list
	 */
	public List<UserModel> list(boolean hiddenSystem) {
		UserModel userModel = new UserModel();
		userModel.setSystemUser(hiddenSystem ? 0 : 1);
		return super.listByBean(userModel);
	}

	/**
	 * 用户登录
	 *
	 * @param name 用户名
	 * @param pwd  密码
	 * @return 登录
	 */
	public UserModel simpleLogin(String name, String pwd) {
		UserModel userModel = super.getByKey(name, false);
		if (userModel == null) {
			return null;
		}

		String obj = SecureUtil.sha1(pwd + userModel.getSalt());
		if (StrUtil.equals(obj, userModel.getPassword())) {
			super.fillSelectResult(userModel);
			return userModel;
		}
		return null;
	}
}
