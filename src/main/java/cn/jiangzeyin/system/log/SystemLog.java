package cn.jiangzeyin.system.log;

import ch.qos.logback.classic.*;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy;
import ch.qos.logback.core.util.FileSize;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author jiangzeyin
 * Created by jiangzeyin on 2017/2/3.
 */
@Configuration
public class SystemLog {
    private static final LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    private static final Map<LogType, Logger> LOG_TYPE_LOGGER_MAP = new ConcurrentHashMap<>();
    private static final Map<String, Logger> LOGGER_MAP = new ConcurrentHashMap<>();
    private static final String TYPE_ERROR_TAG = "ERROR";
    private static ConsoleAppender<ILoggingEvent> consoleAppender;
    private static String LogPath = "/ztoutiao/log/";

    @PostConstruct
    public void test() {
        //System.out.println("执行加载方法");
        init();
    }

    private static void init() {
        System.out.println("调用方法");
        //SystemLog.LogPath = logPath;
        consoleAppender = initConsole();
        //initLogBackXml();
        initSystemLog();
    }

    /**
     * 加载系统日志文件对象
     */
    private static void initSystemLog() {
        for (LogType type : LogType.values()) {
            String tag = type.toString();
            Level level = Level.INFO;
            if (tag.endsWith(TYPE_ERROR_TAG)) {
                level = Level.ERROR;
            }
            Logger logger = initLogger(tag, tag, level);
            LOG_TYPE_LOGGER_MAP.put(type, logger);
            //logger.info("init ok!");
            //logger.error("init error");
        }
    }

//    /**
//     * 加载自定义日志文件对象
//     */
//    private static void initLogBackXml() {
//        // 加载自定义日志类型
//        InputStream inputStream = SystemLog.class.getResourceAsStream("/logback-spring.xml");
//        try {
//            Document document = XmlUtil.load(inputStream);
//            Element rootElement = document.getRootElement();
//            List<Element> property_s = rootElement.elements("property");
//            for (Element property : property_s) {
//                Attribute name = property.attribute("name");
//                String name_value = name.getValue();
//                // 自定义日志类型
//                if (MY_LOG_TYPENAME.equalsIgnoreCase(name_value)) {
//                    String value = property.attribute("value").getValue();
//                    String[] values = StringUtil.StringToArray(value);
//                    if (values != null)
//                        for (String item : values) {
//                            Level level = Level.INFO;
//                            if (item.endsWith(TYPE_ERROR_TAG)) {
//                                level = Level.ERROR;
//                            }
//                            Logger logger = initLogger(item, item, level);
//                            LOGGER_MAP.put(item, logger);
//                            //logger.info("init ok!");
//                        }
//                } else if (LOG_PATH_NAME.equalsIgnoreCase(name_value)) {
//                    // 日志保存跟路径
//                    LogPath = property.attribute("value").getValue();
//                }
//            }
//        } catch (DocumentException e) {
//            SystemLog.ERROR().error("加载日志文件xml", e);
//        }
//    }

    /**
     * 加载控制显示
     *
     * @return
     */
    private static ConsoleAppender<ILoggingEvent> initConsole() {
        ConsoleAppender<ILoggingEvent> appender = new ConsoleAppender<>();
        appender.setContext(loggerContext);
        PatternLayout patternLayout = new PatternLayout();
        patternLayout.setContext(loggerContext);
        patternLayout.setPattern("%date %level [%thread] %logger{10} [%file:%line]- x:\\(%X\\) %msg%n");
        patternLayout.start();
        appender.setLayout(patternLayout);

        appender.start();
        return appender;
    }

    /**
     * 创建日志对象
     *
     * @param tag
     * @param path
     * @param level
     * @return
     */
    private static Logger initLogger(String tag, String path, Level level) {
        Logger logger = (Logger) LoggerFactory.getLogger(tag);
        logger.detachAndStopAllAppenders();
        logger.setLevel(level);
        AsyncAppender asyncAppender = new AsyncAppender();
        asyncAppender.setContext(loggerContext);
        asyncAppender.setDiscardingThreshold(0);
        asyncAppender.setQueueSize(512);
        //define appender
        RollingFileAppender appender = new RollingFileAppender<>();
        //policy
        SizeAndTimeBasedRollingPolicy<Object> policy = new SizeAndTimeBasedRollingPolicy<>();
        policy.setContext(loggerContext);
        //"/ztoutiao/logsss/" + EntitySystemBean.getInstance().systemTag + "/" + path + "/" + tag + "-%d{yyyy-MM-dd}.%i.logsss"
        policy.setFileNamePattern(String.format("%s/%s/%s-%%d{yyyy-MM-dd}.%%i.log", LogPath, path, tag));
        policy.setMaxFileSize("100MB");
        policy.setMaxHistory(30);
        policy.setTotalSizeCap(FileSize.valueOf("10GB"));
        policy.setParent(appender);
        policy.start();
        //encoder
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(loggerContext);
        encoder.setPattern("%d{HH:mm:ss.SSS} %-5level [%thread %file:%line] %logger - %msg%n");
        encoder.start();
        //start appender    // "/ztoutiao/logsss/" + EntitySystemBean.getInstance().systemTag + "/" + path + "/" + tag + ".logsss"
        appender.setFile(String.format("%s/%s/%s.log", LogPath, path, tag));
        appender.setName("appender" + tag);
        appender.setRollingPolicy(policy);
        appender.setContext(loggerContext);
        appender.setEncoder(encoder);
        //appender.setPrudent(true); //support that multiple JVMs can safely write to the same file.
        appender.start();
        asyncAppender.addAppender(appender);

        asyncAppender.start();
        logger.addAppender(asyncAppender);
        if (level == Level.ERROR) {
            logger.addAppender(consoleAppender);
        }
        //setup level
        // newLogger.setLevel(Level.ERROR);
        //remove the appenders that inherited 'ROOT'.
        logger.setAdditive(true);
        return logger;
    }

    /**
     * 获取系统日志
     *
     * @param type
     * @return
     */
    public static Logger LOG(LogType type) {
        Logger logger = LOG_TYPE_LOGGER_MAP.get(type);
        if (logger == null)
            return LOG(LogType.DEFAULT);
        return logger;
    }

    public static Logger LOG() {
        return LOG(LogType.DEFAULT);
    }

    public static Logger ERROR() {
        return LOG(LogType.ERROR);
    }

    /**
     * 获取自定义日志
     *
     * @param type
     * @return
     */
    public static Logger LOG(String type) {
        Logger logger = LOGGER_MAP.get(type);
        if (logger == null) {
            Assert.notNull(type);
            if (type.endsWith(TYPE_ERROR_TAG))
                return ERROR();
            else
                return LOG();
        }
        return logger;
    }
}
