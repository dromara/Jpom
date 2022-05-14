import cn.hutool.core.thread.ThreadUtil;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;

/**
 * @author bwcx_jzy
 * @since 2022/5/13
 */
public class TestThread {

    @Test
    public void test() throws ExecutionException, InterruptedException {
        ExecutorService executorService = ThreadUtil.newSingleExecutor();


        for (int i = 0; i < 5; i++) {
            int finalI = i;
            executorService.submit(new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    System.out.println(finalI);
                    return finalI;
                }
            });
        }

        ThreadUtil.sleep(10, TimeUnit.SECONDS);
    }
}
