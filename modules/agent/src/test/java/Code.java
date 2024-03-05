/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
import org.junit.Test;

import java.util.concurrent.TimeUnit;

/**
 * @author bwcx_jzy
 * @since 2019/5/30
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
