package io.jpom.permission;

import io.jpom.service.h2db.BaseNodeService;

import java.lang.annotation.*;

/**
 * @author bwcx_jzy
 * @since 2021/12/23
 */

@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface NodeDataPermission {

	/**
	 * 参数名
	 *
	 * @return 默认ID
	 */
	String parameterName() default "id";

	/**
	 * 数据 class
	 *
	 * @return cls
	 */
	Class<? extends BaseNodeService<?>> cls();
}
