/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 码之科技工作室
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
package io.jpom.model;

import cn.hutool.cron.pattern.CronPattern;

import java.util.concurrent.TimeUnit;

/**
 * 周期
 *
 * @author bwcx_jzy
 * @date 2019/9/16
 */
public enum Cycle implements BaseEnum {
	/**
	 * 监控周期，code 代表周期时间，单位：分钟、秒
	 */
	seconds30(-30, "30秒"),
	none(0, "不开启"),
	one(1, "1分钟"),
	five(5, "5分钟"),
	ten(10, "10分钟"),
	thirty(30, "30分钟");

	final int code;
	final String desc;
	CronPattern cronPattern;
	long millis;

	Cycle(int code, String desc) {
		this.code = code;
		this.desc = desc;
		if (code > 0) {
			this.cronPattern = new CronPattern(String.format("0 0/%s * * * ?", code));
			this.millis = TimeUnit.MINUTES.toMillis(code);
		} else if (code == 0) {
			//

		} else {
			code = -code;
			this.cronPattern = new CronPattern(String.format("0/%s * * * * ?", code));
			this.millis = TimeUnit.SECONDS.toMillis(code);
		}
	}

	public long getMillis() {
		return millis;
	}

	public CronPattern getCronPattern() {
		return cronPattern;
	}

	@Override
	public int getCode() {
		return code;
	}

	@Override
	public String getDesc() {
		return desc;
	}

//	public static JSONArray getAllJSONArray() {
//		//监控周期
//		JSONArray jsonArray = BaseEnum.toJSONArray(Cycle.class);
//		jsonArray = jsonArray.stream().filter(o -> {
//			JSONObject jsonObject = (JSONObject) o;
//			int code = jsonObject.getIntValue("code");
//			return code != none.getCode();
//		}).collect(Collectors.toCollection(JSONArray::new));
//		try {
//			JSONObject jsonObject = BaseEnum.toJSONObject(Cycle.none);
//			jsonArray.add(0, jsonObject);
//		} catch (InvocationTargetException | IllegalAccessException ignored) {
//		}
//		return jsonArray;
//	}
//
//	public static JSONArray getJSONArray() {
//		//监控周期
//		JSONArray cycleArray = getAllJSONArray();
//		return cycleArray.stream().filter(o -> {
//			JSONObject jsonObject = (JSONObject) o;
//			int code = jsonObject.getIntValue("code");
//			return code > none.getCode();
//		}).collect(Collectors.toCollection(JSONArray::new));
//	}
}
