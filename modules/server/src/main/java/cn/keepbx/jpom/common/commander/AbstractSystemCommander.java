//package cn.keepbx.jpom.common.commander;
//
//import cn.keepbx.jpom.BaseJpomApplication;
//import cn.keepbx.jpom.common.commander.impl.LinuxSystemCommander;
//import cn.keepbx.jpom.common.commander.impl.WindowsSystemCommander;
//import cn.keepbx.jpom.system.JpomRuntimeException;
//
///**
// * 系统监控基类
// *
// * @author jiangzeyin
// * @date 2019/4/15
// */
//public abstract class AbstractSystemCommander {
//    private static AbstractSystemCommander abstractSystemCommander = null;
//
//    public static AbstractSystemCommander getInstance() {
//        if (abstractSystemCommander != null) {
//            return abstractSystemCommander;
//        }
//        if (BaseJpomApplication.OS_INFO.isLinux()) {
//            // Linux系统
//            abstractSystemCommander = new LinuxSystemCommander();
//        } else if (BaseJpomApplication.OS_INFO.isWindows()) {
//            // Windows系统
//            abstractSystemCommander = new WindowsSystemCommander();
//        } else {
//            throw new JpomRuntimeException("不支持的：" + BaseJpomApplication.OS_INFO.getName());
//        }
//        return abstractSystemCommander;
//    }
//
//
//
//}
