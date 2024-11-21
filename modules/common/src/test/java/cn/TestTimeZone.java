package cn;

import org.junit.Test;

import java.util.TimeZone;

/**
 * @author bwcx_jzy
 * @since 2024/11/21
 */
public class TestTimeZone {

    @Test
    public void test() {
        TimeZone timeZone = TimeZone.getDefault();
        String[] availableIDs = TimeZone.getAvailableIDs();

        for (String availableID : availableIDs) {
            TimeZone zone = TimeZone.getTimeZone(availableID);

            // 获取偏移量
            int offset = zone.getRawOffset();

            // 判断是否为东八区
            if (offset == 8 * 60 * 60 * 1000) {
                System.out.println(availableID);
            } else {
                //System.out.println("系统默认时区不是东八区");
            }
        }
        System.out.println("=====");
        System.out.println(timeZone);
    }
}
