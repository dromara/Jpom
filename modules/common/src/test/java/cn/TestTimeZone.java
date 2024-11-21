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
            System.out.println(availableID);
        }
        System.out.println("=====");
        System.out.println(timeZone);
    }
}
