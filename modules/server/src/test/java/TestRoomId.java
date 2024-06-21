import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.RandomUtil;
import org.junit.Test;

/**
 * @author bwcx_jzy
 * @since 2024/6/21
 */
public class TestRoomId {

    private final long ID_BITS = 12L;
    private final long ID_MAX = ~(-1L << ID_BITS);
    private final long RANDOM_ID_BITS = 12L;
    private final long RANDOM_ID_SHIFT = ID_BITS;
    private final long RANDOM_ID_MAX = ~(-1L << RANDOM_ID_BITS);
    private long nextId = 0;

    @Test
    public void test() {
        for (int i = 0; i < ID_MAX * 2; i++) {
            if (nextId++ > ID_MAX) {
                nextId = 0;
            }
            long randId = RandomUtil.randomLong(0, RANDOM_ID_MAX);
            long id = randId << RANDOM_ID_SHIFT | nextId;
            System.out.println("当前自增：" + nextId + " 房间Id:" + id + " 排序ID:" + (id & ID_MAX));
            System.out.println(HexUtil.toHex(id));
        }

    }
}
