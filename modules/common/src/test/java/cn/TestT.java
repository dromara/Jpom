package cn;

import java.time.Duration;

/**
 * @author bwcx_jzy
 * @date 2019/9/4
 */
public class TestT {
    public static void main(String[] args) {
        Duration parse = Duration.parse("1H");
        System.out.println(parse.getSeconds());
    }
}
