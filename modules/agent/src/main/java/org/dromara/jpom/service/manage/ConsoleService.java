///*
// * The MIT License (MIT)
// *
// * Copyright (c) 2019 Code Technology Studio
// *
// * Permission is hereby granted, free of charge, to any person obtaining a copy of
// * this software and associated documentation files (the "Software"), to deal in
// * the Software without restriction, including without limitation the rights to
// * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
// * the Software, and to permit persons to whom the Software is furnished to do so,
// * subject to the following conditions:
// *
// * The above copyright notice and this permission notice shall be included in all
// * copies or substantial portions of the Software.
// *
// * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
// * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
// * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
// * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
// * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
// */
//package org.dromara.jpom.service.manage;
//
//import org.dromara.jpom.common.commander.AbstractProjectCommander;
//import org.dromara.jpom.common.commander.CommandOpResult;
//import org.dromara.jpom.model.data.NodeProjectInfoModel;
//import org.dromara.jpom.socket.ConsoleCommandOp;
//import org.springframework.stereotype.Service;
//
///**
// * 控制台
// * Created by bwcx_jzy on 2018/9/28.
// *
// * @author bwcx_jzy
// */
//@Service
//public class ConsoleService {
//
//    /**
//     * 执行shell命令
//     *
//     * @param consoleCommandOp     执行的操作
//     * @param nodeProjectInfoModel 项目信息
//     * @return 执行结果
//     * @throws Exception 异常
//     */
//    public CommandOpResult execCommand(ConsoleCommandOp consoleCommandOp, NodeProjectInfoModel nodeProjectInfoModel) throws Exception {
//        CommandOpResult result;
//        AbstractProjectCommander abstractProjectCommander = AbstractProjectCommander.getInstance();
//        // 执行命令
//        switch (consoleCommandOp) {
//            case restart:
//                result = abstractProjectCommander.restart(nodeProjectInfoModel);
//                break;
//            case start:
//                result = abstractProjectCommander.start(nodeProjectInfoModel);
//                break;
//            case stop:
//                result = abstractProjectCommander.stop(nodeProjectInfoModel);
//                break;
//            case status: {
//                result = abstractProjectCommander.status(nodeProjectInfoModel);
//                break;
//            }
//            case reload: {
//                result = abstractProjectCommander.reload(nodeProjectInfoModel);
//                break;
//            }
//            case showlog:
//            default:
//                throw new IllegalArgumentException(consoleCommandOp + " error");
//        }
//        return result;
//    }
//}
