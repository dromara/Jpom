import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;

import java.util.TimeZone;

/**
 * Created by jiangzeyin on 2019/3/1.
 */
public class TestString {
    public static void main(String[] args) {
//        System.out.println(CheckPassword.checkPassword("123aA!"));
        DateTime dateTime = DateUtil.parseUTC("2019-04-04T10:11:21Z");
        System.out.println(dateTime);
        dateTime.setTimeZone(TimeZone.getDefault());
        System.out.println(dateTime);
    }
}
