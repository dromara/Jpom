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
package io.jpom.system.init;

import cn.hutool.core.date.SystemClock;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.hutool.extra.servlet.ServletUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.PreLoadClass;
import cn.jiangzeyin.common.PreLoadMethod;
import cn.jiangzeyin.common.spring.SpringUtil;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseServerController;
import io.jpom.common.Const;
import io.jpom.model.data.NodeModel;
import io.jpom.model.data.UserModel;
import io.jpom.model.log.UserOperateLogV1;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.plugin.MethodFeature;
import io.jpom.service.dblog.DbUserOperateLogService;
import io.jpom.system.AopLogInterface;
import io.jpom.system.WebAopLog;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

/**
 * 操作记录控制器
 *
 * @author jiangzeyin
 * @date 2019/4/19
 */
@PreLoadClass
public class OperateLogController implements AopLogInterface {
	private static final ThreadLocal<CacheInfo> CACHE_INFO_THREAD_LOCAL = new ThreadLocal<>();


	@PreLoadMethod
	private static void init() {
		WebAopLog.setAopLogInterface(SpringUtil.getBean(OperateLogController.class));
	}

	private CacheInfo createCacheInfo(Method method) {
		Feature methodFeature = method.getAnnotation(Feature.class);
		if (methodFeature == null) {
			return null;
		}
		Class<?> declaringClass = method.getDeclaringClass();
		MethodFeature feature = methodFeature.method();
		if (feature == MethodFeature.NULL) {
			DefaultSystemLog.getLog().error("权限分发配置错误：{}  {}", declaringClass, method.getName());
			return null;
		}
		ClassFeature cls = methodFeature.cls();
		if (cls == null || cls == ClassFeature.NULL) {
			Feature classFeature = declaringClass.getAnnotation(Feature.class);
			if (classFeature == null || classFeature.cls() == ClassFeature.NULL) {
				DefaultSystemLog.getLog().error("权限分发配置错误：{}  {} class not find", declaringClass, method.getName());
				return null;
			}
			cls = classFeature.cls();
		}
		CacheInfo cacheInfo = new CacheInfo();
		cacheInfo.classFeature = cls;
		cacheInfo.methodFeature = feature;
		cacheInfo.optTime = SystemClock.now();
		return cacheInfo;
	}

	@Override
	public void before(ProceedingJoinPoint joinPoint) {
		Signature signature = joinPoint.getSignature();
		if (signature instanceof MethodSignature) {
			MethodSignature methodSignature = (MethodSignature) signature;
			Method method = methodSignature.getMethod();
			CacheInfo cacheInfo = this.createCacheInfo(method);
			if (cacheInfo == null) {
				return;
			}
			//
			ServletRequestAttributes servletRequestAttributes = BaseServerController.getRequestAttributes();
			HttpServletRequest request = servletRequestAttributes.getRequest();
			// 获取ip地址
			cacheInfo.ip = ServletUtil.getClientIP(request);
			// 获取节点
			cacheInfo.nodeModel = (NodeModel) request.getAttribute("node");
			//
			cacheInfo.dataId = request.getParameter("id");
			//
			cacheInfo.userAgent = ServletUtil.getHeaderIgnoreCase(request, HttpHeaders.USER_AGENT);
			cacheInfo.workspaceId = ServletUtil.getHeaderIgnoreCase(request, Const.WORKSPACEID_REQ_HEADER);
			//
			Map<String, String> map = ServletUtil.getParamMap(request);
			// 过滤密码字段
			Set<Map.Entry<String, String>> entries = map.entrySet();
			for (Map.Entry<String, String> entry : entries) {
				String key = entry.getKey();
				if (StrUtil.containsAnyIgnoreCase(key, "pwd", "password")) {
					entry.setValue("***");
				}
			}
			map.put("request_url", request.getRequestURI());
			cacheInfo.reqData = JSONObject.toJSONString(map);
			CACHE_INFO_THREAD_LOCAL.set(cacheInfo);
		}
	}

	@Override
	public void afterReturning(Object value) {
		try {
			CacheInfo cacheInfo = CACHE_INFO_THREAD_LOCAL.get();
			if (cacheInfo == null || cacheInfo.methodFeature == MethodFeature.LIST) {
				return;
			}
			if (cacheInfo.classFeature == null || cacheInfo.methodFeature == null) {
				new RuntimeException("权限功能没有配置正确").printStackTrace();
				return;
			}
			UserModel userModel = BaseServerController.getUserByThreadLocal();
			userModel = userModel == null ? BaseServerController.getUserModel() : userModel;
			// 没有对应的用户
			if (userModel == null) {
				return;
			}
			this.log(userModel, value, cacheInfo);
		} finally {
			CACHE_INFO_THREAD_LOCAL.remove();
		}
	}

	/**
	 * 记录操作日志
	 *
	 * @param userModel 用户
	 * @param value     返回执行
	 * @param cacheInfo 请求信息
	 */
	public void log(UserModel userModel, Object value, CacheInfo cacheInfo) {
		UserOperateLogV1 userOperateLogV1 = new UserOperateLogV1();
		userOperateLogV1.setWorkspaceId(cacheInfo.workspaceId);
		userOperateLogV1.setClassFeature(cacheInfo.classFeature.name());
		userOperateLogV1.setMethodFeature(cacheInfo.methodFeature.name());
		userOperateLogV1.setDataId(cacheInfo.dataId);
		userOperateLogV1.setUserId(userModel.getId());
		userOperateLogV1.setIp(cacheInfo.ip);
		userOperateLogV1.setUserAgent(cacheInfo.userAgent);
		userOperateLogV1.setReqData(cacheInfo.reqData);
		userOperateLogV1.setOptTime(ObjectUtil.defaultIfNull(cacheInfo.optTime, SystemClock.now()));
		if (value != null) {
			// 解析结果
			if (value instanceof Throwable) {
				// 发生异常
				Throwable throwable = (Throwable) value;
				userOperateLogV1.setResultMsg(ExceptionUtil.stacktraceToString(throwable));
				userOperateLogV1.setOptStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
			} else {
				String json = value.toString();
				userOperateLogV1.setResultMsg(json);
				try {
					JsonMessage<String> jsonMessage = JSONObject.parseObject(json, JsonMessage.class);
					userOperateLogV1.setOptStatus(jsonMessage.getCode());
				} catch (Exception ignored) {
				}
			}
		}
		//
		if (cacheInfo.nodeModel != null) {
			userOperateLogV1.setNodeId(cacheInfo.nodeModel.getId());
			if (StrUtil.isEmpty(cacheInfo.workspaceId)) {
				userOperateLogV1.setWorkspaceId(cacheInfo.nodeModel.getWorkspaceId());
			}
		}
		//
		try {
			BaseServerController.resetInfo(UserModel.EMPTY);
			DbUserOperateLogService dbUserOperateLogService = SpringUtil.getBean(DbUserOperateLogService.class);
			dbUserOperateLogService.insert(userOperateLogV1);
		} finally {
			BaseServerController.removeEmpty();
		}
	}


	/**
	 * 修改执行结果
	 *
	 * @param reqId 请求id
	 * @param val   结果
	 */
	public void updateLog(String reqId, String val) {
		DbUserOperateLogService dbUserOperateLogService = SpringUtil.getBean(DbUserOperateLogService.class);

		Entity entity = new Entity();
		entity.set("resultMsg", val);
		try {
			JsonMessage<?> jsonMessage = JSONObject.parseObject(val, JsonMessage.class);
			entity.set("optStatus", jsonMessage.getCode());
		} catch (Exception ignored) {
		}
		//
		Entity where = new Entity();
		where.set("reqId", reqId);
		dbUserOperateLogService.update(entity, where);
	}

	/**
	 * 临时缓存
	 */
	public static class CacheInfo {
		private Long optTime;
		private String workspaceId;
		private ClassFeature classFeature;
		private MethodFeature methodFeature;
		private String ip;
		private NodeModel nodeModel;
		private String dataId;
		private String userAgent;
		private String reqData;

		public void setOptTime(Long optTime) {
			this.optTime = optTime;
		}

		public void setWorkspaceId(String workspaceId) {
			this.workspaceId = workspaceId;
		}

		public void setClassFeature(ClassFeature classFeature) {
			this.classFeature = classFeature;
		}

		public void setMethodFeature(MethodFeature methodFeature) {
			this.methodFeature = methodFeature;
		}

		public void setIp(String ip) {
			this.ip = ip;
		}

		public void setNodeModel(NodeModel nodeModel) {
			this.nodeModel = nodeModel;
		}

		public void setDataId(String dataId) {
			this.dataId = dataId;
		}

		public void setUserAgent(String userAgent) {
			this.userAgent = userAgent;
		}

		public void setReqData(String reqData) {
			this.reqData = reqData;
		}
	}
}
