/**
 * @author bwcx_jzy
 * @since 2025/6/11
 */
import com.jcraft.jsch.*;
import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 基于终端解析的轻量级命令记录器
 * 通过解析终端输出流来识别和记录执行的命令
 */
public class TerminalParserRecorder {

    private static final Pattern PROMPT_PATTERNS = Pattern.compile(
        ".*?[\\$#>]\\s*$|.*?\\w+@\\w+[:\\s]+.*?[\\$#>]\\s*$"
    );

    private static final Pattern COMMAND_COMPLETION_PATTERN = Pattern.compile(
        ".*?\\[\\d+\\]\\s*.*?|.*?\\+\\s*.*?|.*?>\\s*.*?"
    );

    private Session session;
    private ChannelShell channel;
    private BlockingQueue<String> outputQueue;
    private List<ExecutedCommand> commandLog;
    private TerminalState currentState;
    private Thread parsingThread;
    private volatile boolean isRecording = false;

    public TerminalParserRecorder() {
        this.outputQueue = new LinkedBlockingQueue<>();
        this.commandLog = Collections.synchronizedList(new ArrayList<>());
        this.currentState = new TerminalState();
    }

    /**
     * 终端状态跟踪
     */
    private static class TerminalState {
        private StringBuilder currentLine = new StringBuilder();
        private String lastPrompt = "";
        private String pendingCommand = "";
        private boolean waitingForPrompt = false;
        private int cursorPosition = 0;

        public void reset() {
            currentLine.setLength(0);
            pendingCommand = "";
            waitingForPrompt = false;
            cursorPosition = 0;
        }
    }

    /**
     * 执行命令记录
     */
    public static class ExecutedCommand {
        private final String command;
        private final String user;
        private final String host;
        private final long timestamp;
        private final String sessionId;
        private String output;
        private int exitCode = -1;

        public ExecutedCommand(String command, String user, String host, String sessionId) {
            this.command = command;
            this.user = user;
            this.host = host;
            this.sessionId = sessionId;
            this.timestamp = System.currentTimeMillis();
        }

        // Getters
        public String getCommand() { return command; }
        public String getUser() { return user; }
        public String getHost() { return host; }
        public long getTimestamp() { return timestamp; }
        public String getSessionId() { return sessionId; }
        public String getOutput() { return output; }
        public int getExitCode() { return exitCode; }

        public void setOutput(String output) { this.output = output; }
        public void setExitCode(int exitCode) { this.exitCode = exitCode; }

        @Override
        public String toString() {
            return String.format("[%s] %s@%s $ %s",
                new Date(timestamp).toString(), user, host, command);
        }
    }

    /**
     * 连接SSH并开始记录
     */
    public void connect(String host, int port, String username, String password) throws Exception {
        JSch jsch = new JSch();
        session = jsch.getSession(username, host, port);
        session.setPassword(password);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();

        channel = (ChannelShell) session.openChannel("shell");

        // 设置终端类型以获得更好的解析效果
        channel.setPtyType("vt100");
        channel.setPtySize(80, 24, 640, 480);

        // 创建输出拦截流
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        TeeOutputStream teeOut = new TeeOutputStream(System.out, baos);
        channel.setOutputStream(teeOut);

        channel.connect();

        // 启动解析线程
        startParsing(channel.getInputStream());
        isRecording = true;
    }

    /**
     * 开始解析终端输出
     */
    private void startParsing(InputStream inputStream) {
        parsingThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                StringBuilder outputBuffer = new StringBuilder();
                int ch;

                while (isRecording && (ch = reader.read()) != -1) {
                    char character = (char) ch;

                    // 处理特殊字符
                    if (character == '\r') {
                        continue; // 忽略回车符
                    } else if (character == '\n') {
                        // 处理换行
                        String line = outputBuffer.toString();
                        processLine(line);
                        outputBuffer.setLength(0);
                    } else if (character == '\b') {
                        // 处理退格
                        if (outputBuffer.length() > 0) {
                            outputBuffer.deleteCharAt(outputBuffer.length() - 1);
                        }
                    } else if (character == '\u001B') {
                        // 处理ANSI转义序列
                        String escapeSequence = readEscapeSequence(reader);
                        processEscapeSequence(escapeSequence);
                    } else {
                        outputBuffer.append(character);
                    }
                }
            } catch (IOException e) {
                if (isRecording) {
                    e.printStackTrace();
                }
            }
        });

        parsingThread.setDaemon(true);
        parsingThread.start();
    }

    /**
     * 处理每一行输出
     */
    private void processLine(String line) {
        String cleanLine = cleanAnsiEscapes(line);

        // 检测命令提示符
        if (isPromptLine(cleanLine)) {
            if (currentState.waitingForPrompt && !currentState.pendingCommand.isEmpty()) {
                // 记录已执行的命令
                recordCommand(currentState.pendingCommand.trim());
                currentState.reset();
            }
            currentState.lastPrompt = cleanLine;
        } else if (!cleanLine.trim().isEmpty()) {
            // 可能是命令行
            String potentialCommand = extractPotentialCommand(cleanLine);
            if (potentialCommand != null) {
                currentState.pendingCommand = potentialCommand;
                currentState.waitingForPrompt = true;
            }
        }
    }

    /**
     * 读取ANSI转义序列
     */
    private String readEscapeSequence(BufferedReader reader) throws IOException {
        StringBuilder seq = new StringBuilder("\u001B");
        int ch;

        // 读取完整的转义序列
        while ((ch = reader.read()) != -1) {
            char c = (char) ch;
            seq.append(c);

            // 大多数转义序列以字母结束
            if (Character.isLetter(c)) {
                break;
            }

            // 防止无限循环
            if (seq.length() > 20) {
                break;
            }
        }

        return seq.toString();
    }

    /**
     * 处理ANSI转义序列
     */
    private void processEscapeSequence(String sequence) {
        // 这里可以处理光标移动、清屏等操作
        // 对于命令记录，主要关注的是内容而不是格式
        if (sequence.contains("K")) {
            // 清除行的部分内容
            currentState.currentLine.setLength(0);
        }
    }

    /**
     * 清理ANSI转义字符
     */
    private String cleanAnsiEscapes(String input) {
        return input.replaceAll("\\x1B\\[[0-9;]*[a-zA-Z]", "");
    }

    /**
     * 检测是否为命令提示符行
     */
    private boolean isPromptLine(String line) {
        return PROMPT_PATTERNS.matcher(line).matches() ||
            line.endsWith("$ ") ||
            line.endsWith("# ") ||
            line.endsWith("> ") ||
            line.matches(".*?\\w+@\\w+.*?[\\$#>].*");
    }

    /**
     * 从行中提取潜在的命令
     */
    private String extractPotentialCommand(String line) {
        // 移除命令提示符部分
        String[] parts = line.split("[\\$#>]", 2);
        if (parts.length > 1) {
            String command = parts[1].trim();
            if (!command.isEmpty() && isValidCommand(command)) {
                return command;
            }
        }

        // 如果没有找到提示符，检查是否是直接的命令
        String trimmed = line.trim();
        if (isValidCommand(trimmed)) {
            return trimmed;
        }

        return null;
    }

    /**
     * 验证是否为有效命令
     */
    private boolean isValidCommand(String command) {
        if (command == null || command.isEmpty()) {
            return false;
        }

        // 过滤掉明显不是命令的内容
        return !command.matches(".*?\\d+\\s*$") && // 不是纯数字
            !command.matches("^[\\s\\-=]+$") && // 不是分隔线
            !command.startsWith("Welcome") &&   // 不是欢迎信息
            !command.startsWith("Last login") && // 不是登录信息
            command.length() < 1000; // 不是过长的输出
    }

    /**
     * 记录命令
     */
    private void recordCommand(String command) {
        try {
            String username = session.getUserName();
            String hostname = session.getHost();
            String sessionId = Integer.toHexString(session.hashCode());

            ExecutedCommand record = new ExecutedCommand(command, username, hostname, sessionId);
            commandLog.add(record);

            // 记录日志
            System.out.println("Recorded command: " + record);

            // 可以在这里添加其他处理逻辑，如写入数据库、发送审计日志等

        } catch (Exception e) {
            System.err.println("Error recording command: " + e.getMessage());
        }
    }

    /**
     * 发送命令
     */
    public void sendCommand(String command) throws Exception {
        if (channel != null && channel.isConnected()) {
            OutputStream out = channel.getOutputStream();
            out.write((command + "\n").getBytes());
            out.flush();
        }
    }

    /**
     * 获取命令日志
     */
    public List<ExecutedCommand> getCommandLog() {
        return new ArrayList<>(commandLog);
    }

    /**
     * 根据条件查询命令
     */
    public List<ExecutedCommand> queryCommands(String userFilter, long startTime, long endTime) {
        synchronized (commandLog) {
            return commandLog.stream()
                .filter(cmd -> userFilter == null || userFilter.equals(cmd.getUser()))
                .filter(cmd -> cmd.getTimestamp() >= startTime && cmd.getTimestamp() <= endTime)
                .collect(ArrayList::new, (list, item) -> list.add(item), (list1, list2) -> list1.addAll(list2));
        }
    }

    /**
     * 导出命令日志
     */
    public void exportToFile(String filename) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("Timestamp,User,Host,SessionId,Command");
            synchronized (commandLog) {
                for (ExecutedCommand cmd : commandLog) {
                    writer.printf("%d,%s,%s,%s,\"%s\"%n",
                        cmd.getTimestamp(),
                        cmd.getUser(),
                        cmd.getHost(),
                        cmd.getSessionId(),
                        cmd.getCommand().replace("\"", "\"\""));
                }
            }
        }
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        isRecording = false;

        if (parsingThread != null) {
            parsingThread.interrupt();
        }

        if (channel != null && channel.isConnected()) {
            channel.disconnect();
        }

        if (session != null && session.isConnected()) {
            session.disconnect();
        }
    }

    /**
     * Tee输出流，同时写入两个流
     */
    private static class TeeOutputStream extends OutputStream {
        private OutputStream out1;
        private OutputStream out2;

        public TeeOutputStream(OutputStream out1, OutputStream out2) {
            this.out1 = out1;
            this.out2 = out2;
        }

        @Override
        public void write(int b) throws IOException {
            out1.write(b);
            out2.write(b);
        }

        @Override
        public void flush() throws IOException {
            out1.flush();
            out2.flush();
        }

        @Override
        public void close() throws IOException {
            out1.close();
            out2.close();
        }
    }

    /**
     * 使用示例
     */
    public static void main(String[] args) {
        TerminalParserRecorder recorder = new TerminalParserRecorder();

        try {
            recorder.connect("192.168.30.29", 22, "user", "123456+.");

            // 等待连接稳定
            Thread.sleep(2000);

            // 发送测试命令
            recorder.sendCommand("ls -la");
            Thread.sleep(3000);

            recorder.sendCommand("pwd");
            Thread.sleep(2000);

            recorder.sendCommand("history | tail -5");
            Thread.sleep(3000);

            // 查看记录的命令
            List<ExecutedCommand> commands = recorder.getCommandLog();
            System.out.println("\n=== Command Log ===");
            commands.forEach(System.out::println);

            // 导出到文件
            recorder.exportToFile("command_audit.csv");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            recorder.disconnect();
        }
    }
}
