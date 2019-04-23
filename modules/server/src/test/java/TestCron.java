import cn.hutool.cron.CronUtil;

/**
 * Created by jiangzeyin on 2019/3/4.
 */
public class TestCron {
    public static void main(String[] args) {
        String CRON_ID = "test";
        CronUtil.remove(CRON_ID);
        CronUtil.setMatchSecond(true);
        CronUtil.schedule(CRON_ID, "0/5 * * * * ?", () -> {
            System.out.println("123");
        });
        CronUtil.restart();
//        System.out.println(JpomApplicationEvent.getPid());
    }
}
