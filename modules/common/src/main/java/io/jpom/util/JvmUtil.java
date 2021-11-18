package io.jpom.util;

import cn.hutool.cache.impl.TimedCache;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Tuple;
import cn.hutool.core.text.StrSplitter;
import cn.hutool.core.util.StrUtil;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * jvm jmx 工具
 *
 * @author jiangzeyin
 * @date 2019/4/13
 */
public class JvmUtil {

	/**
	 * 旧版jpom进程标记
	 */
	private static final String OLD_JPOM_PID_TAG = "Dapplication";
	/**
	 * 旧版jpom进程标记
	 */
	private static final String OLD2_JPOM_PID_TAG = "Jpom.application";
	private static final String POM_PID_TAG = "DJpom.application";

	/**
	 * 获取进程标识
	 *
	 * @param id   i
	 * @param path 路径
	 * @return str
	 */
	public static String getJpomPidTag(String id, String path) {
		return String.format("-%s=%s -DJpom.basedir=%s", POM_PID_TAG, id, path);
	}


	/**
	 * 获取当前系统运行的java 程序个数
	 *
	 * @return 如果发生异常则返回0
	 */
	public static int getJavaVirtualCount() {
		String execSystemCommand = CommandUtil.execSystemCommand("jps -l");
		List<String> list = StrSplitter.splitTrim(execSystemCommand, StrUtil.LF, true);
		return Math.max(CollUtil.size(list) - 1, 0);
	}

	/**
	 * 根据pid 获取jvm
	 *
	 * @param pid 进程id
	 * @return VirtualMachine
	 */
	public static String getVirtualMachineInfo(int pid) {
		String execSystemCommand = CommandUtil.execSystemCommand("jps -mv");
		List<String> list = StrSplitter.splitTrim(execSystemCommand, StrUtil.LF, true);
		Optional<String> any = list.stream().filter(s -> {
			List<String> split = StrUtil.split(s, StrUtil.SPACE);
			return StrUtil.equals(pid + "", CollUtil.getFirst(split));
		}).findAny();
		return any.orElse(null);
	}

	/**
	 * 工具Jpom运行项目的id 获取virtualMachine
	 *
	 * @param tag 项目id
	 * @return VirtualMachine
	 */
	public static Integer getVirtualMachine(String tag) {
		String execSystemCommand = CommandUtil.execSystemCommand("jps -mv");
		List<String> list = StrSplitter.splitTrim(execSystemCommand, StrUtil.LF, true);
		Optional<String> any = list.stream().filter(s -> checkCommandLineIsJpom(s, tag)).map(s -> {
			List<String> split = StrUtil.split(s, StrUtil.SPACE);
			return CollUtil.getFirst(split);
		}).findAny();
		return any.map(Convert::toInt).orElse(null);
	}

	/**
	 * 判断命令行是否为jpom 标识
	 *
	 * @param commandLine 命令行
	 * @param tag         标识
	 * @return true
	 */
	public static boolean checkCommandLineIsJpom(String commandLine, String tag) {
		if (StrUtil.isEmpty(commandLine)) {
			return false;
		}
		String[] split = StrUtil.splitToArray(commandLine, StrUtil.SPACE);
		String appTag = String.format("-%s=%s", JvmUtil.POM_PID_TAG, tag);
		String appTag2 = String.format("-%s=%s", JvmUtil.OLD_JPOM_PID_TAG, tag);
		String appTag3 = String.format("-%s=%s", JvmUtil.OLD2_JPOM_PID_TAG, tag);
		for (String item : split) {
			if (StrUtil.equalsAnyIgnoreCase(item, appTag, appTag2, appTag3)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 工具指定的 mainClass 获取对应所有的的  MonitoredVm对象
	 *
	 * @param mainClass 程序运行主类
	 * @return pid
	 */
	public static Integer findMainClassPid(String mainClass) {
		String execSystemCommand = CommandUtil.execSystemCommand("jps -l");
		List<String> list = StrSplitter.splitTrim(execSystemCommand, StrUtil.LF, true);
		Optional<Tuple> any = list.stream().map(s -> {
			List<String> split = StrUtil.split(s, StrUtil.SPACE);
			return new Tuple(CollUtil.getFirst(split), CollUtil.getLast(split));
		}).filter(tuple -> StrUtil.equals(mainClass, tuple.get(1))).findAny();
		return any.map(tuple -> Convert.toInt(tuple.get(0))).orElse(null);
	}

	private static boolean checkFile(String fileName, String mainClass) {
		try {
			File file = FileUtil.file(fileName);
			if (!file.exists() || file.isDirectory()) {
				return false;
			}
			try (JarFile jarFile1 = new JarFile(file)) {
				Manifest manifest = jarFile1.getManifest();
				Attributes attributes = manifest.getMainAttributes();
				String jarMainClass = attributes.getValue(Attributes.Name.MAIN_CLASS);
				String jarStartClass = attributes.getValue("Start-Class");
				return StrUtil.equals(mainClass, jarMainClass) || StrUtil.equals(mainClass, jarStartClass);
			}
		} catch (Exception e) {
			return false;
		}
	}
}
