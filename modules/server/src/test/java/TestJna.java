/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import org.junit.Test;

/**
 * @author bwcx_jzy
 * @since 24/3/5 005
 */
public class TestJna {

    @Test
    public void test() {
        // 设置你要执行的命令
        String command = "your_command_here arg1 arg2";

        // 使用 JNA 的 Native 类加载运行时库
        // 在不同的平台上，可能需要不同的库名称
        // 例如，在 Windows 上，可以使用 "kernel32"；在 Linux 上，可以使用 "c"
        // 你可能需要根据实际情况进行调整
        String libraryName = "kernel32";
        CLibrary libc = (CLibrary) Native.load(libraryName, CLibrary.class);

        // 执行命令
        int result = libc.system(command);

        // 输出执行结果
        System.out.println("Exit Code: " + result);
    }

    // 定义一个接口，用于加载运行时库
    public interface CLibrary extends com.sun.jna.Library {
        int system(String command);
    }


    public interface CLibrary2 extends Library {
        CLibrary2 INSTANCE = (CLibrary2)
            Native.load((Platform.isWindows() ? "msvcrt" : "c"),
                CLibrary2.class);

        void printf(String format, Object... args);
    }

    @Test
    public void test2() {
        CLibrary2 instance = CLibrary2.INSTANCE;
        System.out.println(instance);
        instance.printf("Hello, World\n");
    }
}
