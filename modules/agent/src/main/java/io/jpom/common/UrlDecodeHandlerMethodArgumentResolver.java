package io.jpom.common;

import cn.hutool.core.util.URLUtil;
import cn.jiangzeyin.common.interceptor.DefaultHandlerMethodArgumentResolver;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * 解析 参数 url 编码
 *
 * @author bwcx_jzy
 * @since 2021/10/25
 */
public class UrlDecodeHandlerMethodArgumentResolver extends DefaultHandlerMethodArgumentResolver {

	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
		Object argument = super.resolveArgument(parameter, mavContainer, webRequest, binderFactory);
		if (argument instanceof String) {
			// 解码
			return URLUtil.decode(argument.toString());
		}
		return argument;
	}
}
