package cn;

import cn.hutool.core.io.LineHandler;
import cn.hutool.core.io.file.Tailer;

import java.io.File;

public class TestTailer {
    public static void main(String[] args) {

        Tailer tailer = new Tailer(new File("D:\\Idea\\Jpom\\modules\\server\\target\\classes\\log\\request\\request-2019-07-21.0.log"), new LineHandler() {
            @Override
            public void handle(String line) {
                System.out.println(line);
            }
        }, 10);
        tailer.start(true);
        System.out.println("12");
    }
}
