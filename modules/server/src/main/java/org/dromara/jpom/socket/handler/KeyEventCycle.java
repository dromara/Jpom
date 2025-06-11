package org.dromara.jpom.socket.handler;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.function.Consumer;

/**
 * 控制台案件事件处理
 *
 * @author bwcx_jzy
 * @since 2025/6/11
 */
@Slf4j
public class KeyEventCycle {
    // 输入缓存
    private StringBuffer buffer = new StringBuffer();
    // 输入后是否接收返回字符串
    private boolean inputReceive = false;
    // TAB 输入暂停（处理Y/N确认）
    private boolean tabInputPause = false;
    // 光标位置
    private int inputSelection = 0;
    // 搜索状态，0未开始，1开始搜索，2搜索结束
    private int searchState = 0;
    @Setter
    private Charset charset;
    private KeyControl keyControl = KeyControl.KEY_END;
    private Consumer<String> consumer;

    /**
     * 从控制台读取输入按键进行处理
     *
     * @param consumer 完整命令后输入回调
     * @param bytes    输入按键
     */
    public void read(Consumer<String> consumer, byte... bytes) {
        this.consumer = consumer;
        String str = new String(bytes, charset);
        if (keyControl == KeyControl.KEY_TAB && tabInputPause) {
            if (str.equalsIgnoreCase("y") || str.equalsIgnoreCase("n")) {
                tabInputPause = false;
                return;
            }
        }
        keyControl = KeyControl.getKeyControl(bytes);
        if ((keyControl == KeyControl.KEY_INPUT || keyControl == KeyControl.KEY_FUNCTION) && !tabInputPause) {
            buffer.insert(inputSelection, str);
            inputSelection += str.length();
        } else if (keyControl == KeyControl.KEY_ENTER) {
            // 回车，结束当前输入周期
            if (buffer.length() > 0 && searchState != 1) {
                consumer.accept(buffer.toString());
            } else if (searchState == 1) {
                // Control + R结束
                searchState = 2;
            }
            // 重置周期
            buffer = new StringBuffer();
            inputReceive = false;
            inputSelection = 0;
        } else if (keyControl == KeyControl.KEY_BACK) {
            buffer.delete(Math.max(inputSelection - 1, 0), inputSelection);
            inputSelection = Math.max(inputSelection - 1, 0);
        } else if (keyControl == KeyControl.KEY_DELETE) {
            buffer.delete(inputSelection, Math.min(inputSelection + 1, buffer.length()));
        } else if (keyControl == KeyControl.KEY_LEFT) {
            inputSelection = Math.max(inputSelection - 1, 0);
        } else if (keyControl == KeyControl.KEY_RIGHT) {
            inputSelection = Math.min(inputSelection + 1, buffer.length());
        } else if (keyControl == KeyControl.KEY_HOME) {
            inputSelection = 0;
        } else if (keyControl == KeyControl.KEY_END) {
            inputSelection = buffer.length();
        } else if (keyControl == KeyControl.KEY_TAB) {
            inputReceive = true;
        } else if (keyControl == KeyControl.KEY_UP || keyControl == KeyControl.KEY_DOWN) {
            // 清空命令缓冲
            inputSelection = 0;
            inputReceive = true;
        } else if (keyControl == KeyControl.KEY_ETX) {
            buffer = new StringBuffer();
            inputSelection = 0;
        } else if (keyControl == KeyControl.KEY_SEARCH) {
            buffer = new StringBuffer();
            searchState = 1;
        }
    }

    /**
     * 从SSH服务端接收字节
     *
     * @param bytes 字节
     */
    public void receive(byte... bytes) {
        if (searchState == 2) {
            // 处理搜索命令结束后，接收到ssh服务器返回的完整命令
            int index = indexOf(bytes, new byte[]{27, 91, 75});
            if (index > -1) {
                bytes = Arrays.copyOf(bytes, index);
            }
            String str = new String(bytes, charset).split("# ")[1];
            consumer.accept(str.trim());
            searchState = 0;
            return;
        }
        if (inputReceive) {
            String str = new String(bytes, charset);
            if (keyControl == KeyControl.KEY_UP || keyControl == KeyControl.KEY_DOWN) {
                // 上下键只有第一条是正常的，后面的都是根据第一条进行退格删除再补充的。
                // 8,8,8,99,100,32,47,112,114,50,111,99,47,
                try {
                    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                        for (byte aByte : bytes) {
                            if (aByte == 8) {
                                // 首位是退格键，就执行删除末尾值
                                buffer.deleteCharAt(Math.max(buffer.length() - 1, 0));
                            } else if (aByte == 27) {
                                // 遇到【逃离/取消】就跳出循环
                                break;
                            } else if (aByte != 0) {
                                outputStream.write(aByte);
                            }
                        }
                        buffer.append(new String(outputStream.toByteArray(), charset));
                    }
                    inputSelection = buffer.length();
                } catch (Exception e) {
                    log.error("", e);
                }
                return;
            } else {
                if (keyControl == KeyControl.KEY_TAB) {
                    if (bytes[0] == 7) {
                        // 接收到终端响铃，就删除响铃
                        bytes = Arrays.copyOfRange(bytes, 1, bytes.length);
                    }
                    if (Arrays.equals(new byte[]{13, 10}, bytes)) {
                        inputReceive = false;
                        return;
                    }
                    // tab下文件很多
                    if (str.contains("y or n")) {
                        tabInputPause = true;
                        inputReceive = false;
                        return;
                    }
                    // cat 'hello word.txt'
                    // cat hello\ word.txt
                    if (str.split(" ").length > 1 && (!str.contains("'") && !str.contains("\\"))) {
                        inputReceive = false;
                        return;
                    }
                }
                // 非上下键输入输入中，如果接受到数据就执行插入数据，根据当前光标位置执行插入
                // 存在退格，就从光标位置开始删除
                int backCount = 0;
                for (byte aByte : bytes) {
                    if (aByte == 8) {
                        buffer.deleteCharAt(inputSelection - 1);
                        backCount++;
                    }
                }
                str = new String(Arrays.copyOfRange(bytes, 0, bytes.length - backCount), charset);
                // #https://gitee.com/dromara/Jpom/issues/ICA57K
                buffer.insert(inputSelection - 1, str);
                inputSelection += str.length();
            }
        }
        inputReceive = false;
    }

    /**
     * 查找指定字节数组在原始字节数组中的位置
     *
     * @param originalArray   原始字节数组
     * @param byteArrayToFind 要查找的字节数组
     * @return 找到的位置索引，如果找不到返回 -1
     */
    private static int indexOf(byte[] originalArray, byte[] byteArrayToFind) {
        // 遍历原始字节数组，查找匹配的起始位置
        for (int i = 0; i <= originalArray.length - byteArrayToFind.length; i++) {
            boolean match = true;
            for (int j = 0; j < byteArrayToFind.length; j++) {
                if (originalArray[i + j] != byteArrayToFind[j]) {
                    match = false;
                    break;
                }
            }
            if (match) {
                return i;
            }
        }
        return -1;
    }

}


