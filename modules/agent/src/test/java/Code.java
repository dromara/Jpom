import org.junit.Test;

import java.util.concurrent.TimeUnit;

/**
 * @author bwcx_jzy
 * @date 2019/5/30
 **/
public class Code {
    public static void main(String[] args) {
        int num = 0;
        for (int i = 0; i < 100; i++) {
            num = num++;
            /**
             *  do something
             */
        }
        System.out.println(num);
    }


    @Test
    public void testTime() {
        System.out.println((int) (TimeUnit.SECONDS.toMillis(10) / 500));
    }
}
