package io.jpom.model;

import cn.hutool.cron.pattern.CronPattern;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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

    private int code;
    private String desc;
    private CronPattern cronPattern;
    private long millis;

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

    public static JSONArray getAllJSONArray() {
        //监控周期
        JSONArray jsonArray = BaseEnum.toJSONArray(Cycle.class);
        jsonArray = jsonArray.stream().filter(o -> {
            JSONObject jsonObject = (JSONObject) o;
            int code = jsonObject.getIntValue("code");
            return code != none.getCode();
        }).collect(Collectors.toCollection(JSONArray::new));
        try {
            JSONObject jsonObject = BaseEnum.toJSONObject(Cycle.none);
            jsonArray.add(0, jsonObject);
        } catch (InvocationTargetException | IllegalAccessException ignored) {
        }
        return jsonArray;
    }

    public static JSONArray getJSONArray() {
        //监控周期
        JSONArray cycleArray = getAllJSONArray();
        return cycleArray.stream().filter(o -> {
            JSONObject jsonObject = (JSONObject) o;
            int code = jsonObject.getIntValue("code");
            return code > none.getCode();
        }).collect(Collectors.toCollection(JSONArray::new));
    }
}
