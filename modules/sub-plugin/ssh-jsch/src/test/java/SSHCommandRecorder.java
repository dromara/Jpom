/**
 * @author bwcx_jzy
 * @since 2025/6/11
 */
import com.jcraft.jsch.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

/**
 * SSH终端命令记录器 - 重构版
 * 通过双向流拦截和智能解析准确记录用户执行的所有命令
 */
public class SSHCommandRecorder {

    // ANSI转义序列模式
    private static final Pattern ANSI_ESCAPE = Pattern.compile("\\x1B\\[[0-?]*[ -/]*[@-~]");

    // 命令提示符模式（支持多种shell）
    private static final Pattern PROMPT_PATTERN = Pattern.compile(
        ".*?[\\$#>%]\\s*$|.*?\\w+[@:].*?[\\$#>%]\\s*$|.*?\\]\\s*[\\$#>%]\\s*$"
    );

    // 会话相关
    private Session session;
    private ChannelShell channel;
    private InputStream channelInput;
    private OutputStream channelOutput;

    // 数据处理
    private final BlockingQueue<String> inputQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<String> outputQueue = new LinkedBlockingQueue<>();
    private final List<CommandRecord> commandHistory = Collections.synchronizedList(new ArrayList<>());

    // 状态管理
    private volatile boolean recording = false;
    private final TerminalState terminalState = new TerminalState();

    // 线程池
    private final ExecutorService executorService = Executors.newFixedThreadPool(4);

    /**
     * 命令记录实体
     */
    public static class CommandRecord {
        private final String command;
        private final String user;
        private final String host;
        private final long timestamp;
        private final String sessionId;
        private String output;
        private CommandType type;
        private long duration;

        public enum CommandType {
            TYPED,          // 直接输入
            HISTORY_UP,     // 上键选择
            HISTORY_DOWN,   // 下键选择
            SEARCH,         // 搜索选择
            TAB_COMPLETE    // Tab补全
        }

        public CommandRecord(String command, String user, String host, String sessionId) {
            this.command = command.trim();
            this.user = user;
            this.host = host;
            this.sessionId = sessionId;
            this.timestamp = System.currentTimeMillis();
            this.type = CommandType.TYPED;
        }

        // Getters and Setters
        public String getCommand() { return command; }
        public String getUser() { return user; }
        public String getHost() { return host; }
        public long getTimestamp() { return timestamp; }
        public String getSessionId() { return sessionId; }
        public String getOutput() { return output; }
        public CommandType getType() { return type; }
        public long getDuration() { return duration; }

        public void setOutput(String output) { this.output = output; }
        public void setType(CommandType type) { this.type = type; }
        public void setDuration(long duration) { this.duration = duration; }

        @Override
        public String toString() {
            return String.format("[%s] %s@%s [%s] $ %s",
                new Date(timestamp), user, host, type, command);
        }
    }

    /**
     * 终端状态跟踪
     */
    private static class TerminalState {
        private final StringBuilder currentLine = new StringBuilder();
        private final StringBuilder commandBuffer = new StringBuilder();
        private String lastPrompt = "";
        private String currentCommand = "";
        private boolean inCommand = false;
        private boolean waitingForOutput = false;
        private long commandStartTime = 0;
        private int cursorPosition = 0;

        // 特殊键检测
        private boolean upKeyPressed = false;
        private boolean downKeyPressed = false;
        private boolean ctrlRPressed = false;
        private boolean tabPressed = false;

        public synchronized void reset() {
            currentLine.setLength(0);
            commandBuffer.setLength(0);
            currentCommand = "";
            inCommand = false;
            waitingForOutput = false;
            commandStartTime = 0;
            resetKeyStates();
        }

        public synchronized void resetKeyStates() {
            upKeyPressed = false;
            downKeyPressed = false;
            ctrlRPressed = false;
            tabPressed = false;
        }
    }

    /**
     * 输入流拦截器
     */
    private class InputStreamInterceptor extends InputStream {
        private final InputStream originalInput;
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        public InputStreamInterceptor(InputStream originalInput) {
            this.originalInput = originalInput;
        }

        @Override
        public int read() throws IOException {
            int data = originalInput.read();
            if (data != -1) {
                buffer.write(data);
                processInputByte((byte) data);
            }
            return data;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int bytesRead = originalInput.read(b, off, len);
            if (bytesRead > 0) {
                buffer.write(b, off, bytesRead);
                processInputBytes(b, off, bytesRead);
            }
            return bytesRead;
        }

        private void processInputByte(byte b) {
            processInputBytes(new byte[]{b}, 0, 1);
        }

        private void processInputBytes(byte[] bytes, int offset, int length) {
            try {
                String input = new String(bytes, offset, length, StandardCharsets.UTF_8);
                if (!input.isEmpty()) {
                    inputQueue.offer(input);
                }
            } catch (Exception e) {
                System.err.println("Error processing input: " + e.getMessage());
            }
        }
    }

    /**
     * 输出流拦截器
     */
    private class OutputStreamInterceptor extends OutputStream {
        private final OutputStream originalOutput;
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        public OutputStreamInterceptor(OutputStream originalOutput) {
            this.originalOutput = originalOutput;
        }

        @Override
        public void write(int b) throws IOException {
            originalOutput.write(b);
            buffer.write(b);
            processOutputByte((byte) b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            originalOutput.write(b, off, len);
            buffer.write(b, off, len);
            processOutputBytes(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            originalOutput.flush();
        }

        private void processOutputByte(byte b) {
            processOutputBytes(new byte[]{b}, 0, 1);
        }

        private void processOutputBytes(byte[] bytes, int offset, int length) {
            try {
                String output = new String(bytes, offset, length, StandardCharsets.UTF_8);
                if (!output.isEmpty()) {
                    outputQueue.offer(output);
                }
            } catch (Exception e) {
                System.err.println("Error processing output: " + e.getMessage());
            }
        }
    }

    /**
     * 建立SSH连接并开始记录
     */
    public void connect(String host, int port, String username, String password) throws Exception {
        JSch jsch = new JSch();
        session = jsch.getSession(username, host, port);
        session.setPassword(password);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();

        channel = (ChannelShell) session.openChannel("shell");

        // 设置终端参数
        channel.setPtyType("xterm-256color");
        channel.setPtySize(120, 40, 960, 640);

        // 创建管道流
        PipedInputStream toChannelInput = new PipedInputStream();
        PipedOutputStream fromUserInput = new PipedOutputStream(toChannelInput);

        PipedInputStream fromChannelOutput = new PipedInputStream();
        PipedOutputStream toUserOutput = new PipedOutputStream(fromChannelOutput);

        // 设置拦截器
        channelInput = new InputStreamInterceptor(System.in);
        channelOutput = new OutputStreamInterceptor(System.out);

        channel.setInputStream(toChannelInput);
        channel.setOutputStream(toUserOutput);

        channel.connect();

        // 获取真实的输入输出流
        InputStream realChannelOutput = channel.getInputStream();
        OutputStream realChannelInput = channel.getOutputStream();

        recording = true;

        // 启动处理线程
        startProcessingThreads(realChannelOutput, realChannelInput, fromUserInput, fromChannelOutput);

        System.out.println("SSH连接已建立，开始记录命令...");
    }

    /**
     * 启动处理线程
     */
    private void startProcessingThreads(InputStream channelOut, OutputStream channelIn,
                                        OutputStream userIn, InputStream userOut) {

        // 处理用户输入 -> SSH服务器
        executorService.submit(() -> {
            try {
                Scanner scanner = new Scanner(System.in);
                while (recording && scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    processUserInput(line);
                    channelIn.write((line + "\r\n").getBytes(StandardCharsets.UTF_8));
                    channelIn.flush();
                }
            } catch (Exception e) {
                if (recording) {
                    e.printStackTrace();
                }
            }
        });

        // 处理SSH服务器输出 -> 用户
        executorService.submit(() -> {
            try {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while (recording && (bytesRead = channelOut.read(buffer)) != -1) {
                    String output = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
                    processServerOutput(output);
                    System.out.print(output);
                }
            } catch (Exception e) {
                if (recording) {
                    e.printStackTrace();
                }
            }
        });

        // 输入分析线程
        executorService.submit(this::processInputQueue);

        // 输出分析线程
        executorService.submit(this::processOutputQueue);
    }

    /**
     * 处理用户输入
     */
    private void processUserInput(String input) {
        synchronized (terminalState) {
            // 检测特殊键序列
            if (input.contains("\u001B[A")) { // 上键
                terminalState.upKeyPressed = true;
            } else if (input.contains("\u001B[B")) { // 下键
                terminalState.downKeyPressed = true;
            } else if (input.contains("\u0012")) { // Ctrl+R
                terminalState.ctrlRPressed = true;
            } else if (input.contains("\t")) { // Tab
                terminalState.tabPressed = true;
            }

            // 处理正常命令输入
            if (!input.trim().isEmpty() && !containsOnlyControlChars(input)) {
                terminalState.currentCommand = input.trim();
                terminalState.inCommand = true;
                terminalState.commandStartTime = System.currentTimeMillis();
                terminalState.waitingForOutput = true;
            }
        }
    }

    /**
     * 处理服务器输出
     */
    private void processServerOutput(String output) {
        synchronized (terminalState) {
            String cleanOutput = cleanAnsiEscapes(output);
            terminalState.currentLine.append(cleanOutput);

            // 检测命令提示符
            if (isPromptLine(cleanOutput)) {
                if (terminalState.waitingForOutput && !terminalState.currentCommand.isEmpty()) {
                    // 记录命令
                    recordCommand(terminalState.currentCommand, determineCommandType());
                    terminalState.reset();
                }

                // 提取新的提示符
                String[] lines = cleanOutput.split("\\r?\\n");
                for (String line : lines) {
                    if (isPromptLine(line.trim())) {
                        terminalState.lastPrompt = line.trim();
                        break;
                    }
                }
            }
        }
    }

    /**
     * 处理输入队列
     */
    private void processInputQueue() {
        while (recording) {
            try {
                String input = inputQueue.poll(100, TimeUnit.MILLISECONDS);
                if (input != null) {
                    analyzeInput(input);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * 处理输出队列
     */
    private void processOutputQueue() {
        while (recording) {
            try {
                String output = outputQueue.poll(100, TimeUnit.MILLISECONDS);
                if (output != null) {
                    analyzeOutput(output);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * 分析输入数据
     */
    private void analyzeInput(String input) {
        // 检测控制序列
        if (input.contains("\u001B[A")) {
            terminalState.upKeyPressed = true;
        } else if (input.contains("\u001B[B")) {
            terminalState.downKeyPressed = true;
        } else if (input.contains("\u0012")) {
            terminalState.ctrlRPressed = true;
        }

        // 检测回车键（命令执行）
        if (input.contains("\r") || input.contains("\n")) {
            synchronized (terminalState) {
                if (terminalState.inCommand) {
                    terminalState.waitingForOutput = true;
                }
            }
        }
    }

    /**
     * 分析输出数据
     */
    private void analyzeOutput(String output) {
        String cleanOutput = cleanAnsiEscapes(output);

        // 检测命令回显和提示符
        String[] lines = cleanOutput.split("\\r?\\n");
        for (String line : lines) {
            String trimmedLine = line.trim();

            if (isPromptLine(trimmedLine)) {
                synchronized (terminalState) {
                    if (terminalState.waitingForOutput && !terminalState.currentCommand.isEmpty()) {
                        recordCommand(terminalState.currentCommand, determineCommandType());
                        terminalState.reset();
                    }
                    terminalState.lastPrompt = trimmedLine;
                }
            } else if (!trimmedLine.isEmpty() && !terminalState.inCommand) {
                // 可能是命令回显
                synchronized (terminalState) {
                    if (isPotentialCommand(trimmedLine)) {
                        terminalState.currentCommand = trimmedLine;
                        terminalState.inCommand = true;
                        terminalState.commandStartTime = System.currentTimeMillis();
                    }
                }
            }
        }
    }

    /**
     * 判断命令类型
     */
    private CommandRecord.CommandType determineCommandType() {
        if (terminalState.upKeyPressed || terminalState.downKeyPressed) {
            return terminalState.upKeyPressed ?
                CommandRecord.CommandType.HISTORY_UP : CommandRecord.CommandType.HISTORY_DOWN;
        } else if (terminalState.ctrlRPressed) {
            return CommandRecord.CommandType.SEARCH;
        } else if (terminalState.tabPressed) {
            return CommandRecord.CommandType.TAB_COMPLETE;
        } else {
            return CommandRecord.CommandType.TYPED;
        }
    }

    /**
     * 记录命令
     */
    private void recordCommand(String command, CommandRecord.CommandType type) {
        if (command == null || command.trim().isEmpty() || !isValidCommand(command)) {
            return;
        }

        try {
            String username = session.getUserName();
            String hostname = session.getHost();
            String sessionId = Integer.toHexString(session.hashCode());

            CommandRecord record = new CommandRecord(command, username, hostname, sessionId);
            record.setType(type);

            if (terminalState.commandStartTime > 0) {
                record.setDuration(System.currentTimeMillis() - terminalState.commandStartTime);
            }

            commandHistory.add(record);

            // 记录到控制台和日志
            System.out.println("\n[AUDIT] " + record);

            // 异步写入文件或数据库
            executorService.submit(() -> writeToAuditLog(record));

        } catch (Exception e) {
            System.err.println("Error recording command: " + e.getMessage());
        }
    }

    /**
     * 写入审计日志
     */
    private void writeToAuditLog(CommandRecord record) {
        try {
            String logFile = "ssh_audit_" + new java.text.SimpleDateFormat("yyyy-MM-dd").format(new Date()) + ".log";
            try (PrintWriter writer = new PrintWriter(new FileWriter(logFile, true))) {
                writer.printf("%d|%s|%s|%s|%s|%s|%d%n",
                    record.getTimestamp(),
                    record.getUser(),
                    record.getHost(),
                    record.getSessionId(),
                    record.getType(),
                    record.getCommand().replace("|", "\\|"),
                    record.getDuration());
            }
        } catch (IOException e) {
            System.err.println("Error writing to audit log: " + e.getMessage());
        }
    }

    /**
     * 清理ANSI转义序列
     */
    private String cleanAnsiEscapes(String input) {
        if (input == null) return "";
        return ANSI_ESCAPE.matcher(input).replaceAll("");
    }

    /**
     * 检测是否为命令提示符行
     */
    private boolean isPromptLine(String line) {
        return PROMPT_PATTERN.matcher(line).matches() ||
            line.endsWith("$ ") || line.endsWith("# ") || line.endsWith("> ") ||
            line.matches(".*?\\w+[@:].*?[\\$#>].*");
    }

    /**
     * 检测是否为潜在命令
     */
    private boolean isPotentialCommand(String line) {
        return line.length() > 0 &&
            line.length() < 500 &&
            !line.matches("^\\s*$") &&
            !line.startsWith("Welcome") &&
            !line.startsWith("Last login") &&
            !line.matches(".*?\\d{4}-\\d{2}-\\d{2}.*?");
    }

    /**
     * 验证是否为有效命令
     */
    private boolean isValidCommand(String command) {
        if (command == null || command.trim().isEmpty()) {
            return false;
        }

        String trimmed = command.trim();
        return trimmed.length() > 0 &&
            trimmed.length() < 1000 &&
            !trimmed.matches("^\\s*$") &&
            !trimmed.matches("^\\d+$") &&
            !containsOnlyControlChars(trimmed);
    }

    /**
     * 检查是否只包含控制字符
     */
    private boolean containsOnlyControlChars(String str) {
        return str.chars().allMatch(ch -> ch < 32 || ch == 127);
    }

    /**
     * 发送命令（用于程序化调用）
     */
    public void sendCommand(String command) throws Exception {
        if (channel != null && channel.isConnected()) {
            OutputStream out = channel.getOutputStream();
            out.write((command + "\r\n").getBytes(StandardCharsets.UTF_8));
            out.flush();
        }
    }

    /**
     * 获取命令历史记录
     */
    public List<CommandRecord> getCommandHistory() {
        synchronized (commandHistory) {
            return new ArrayList<>(commandHistory);
        }
    }

    /**
     * 根据条件查询命令
     */
    public List<CommandRecord> queryCommands(String userFilter, long startTime, long endTime,
                                             CommandRecord.CommandType typeFilter) {
        synchronized (commandHistory) {
            return commandHistory.stream()
                .filter(cmd -> userFilter == null || userFilter.equals(cmd.getUser()))
                .filter(cmd -> cmd.getTimestamp() >= startTime && cmd.getTimestamp() <= endTime)
                .filter(cmd -> typeFilter == null || typeFilter.equals(cmd.getType()))
                .collect(ArrayList::new, (list, item) -> list.add(item), (list1, list2) -> list1.addAll(list2));
        }
    }

    /**
     * 导出审计报告
     */
    public void exportAuditReport(String filename) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("SSH Command Audit Report");
            writer.println("Generated: " + new Date());
           // writer.println("=".repeat(80));
            writer.println();

            synchronized (commandHistory) {
                Map<String, Long> userStats = new HashMap<>();
                Map<CommandRecord.CommandType, Long> typeStats = new HashMap<>();

                for (CommandRecord record : commandHistory) {
                    userStats.merge(record.getUser(), 1L, Long::sum);
                    typeStats.merge(record.getType(), 1L, Long::sum);
                }

                writer.println("Statistics:");
                writer.println("Total Commands: " + commandHistory.size());
                writer.println("Users: " + userStats);
                writer.println("Command Types: " + typeStats);
                writer.println();

                writer.println("Command Details:");
               // writer.println("-".repeat(80));
                for (CommandRecord record : commandHistory) {
                    writer.printf("[%s] %s@%s [%s] [%dms] $ %s%n",
                        new Date(record.getTimestamp()),
                        record.getUser(),
                        record.getHost(),
                        record.getType(),
                        record.getDuration(),
                        record.getCommand());
                }
            }
        }
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        recording = false;

        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }

        if (channel != null && channel.isConnected()) {
            channel.disconnect();
        }

        if (session != null && session.isConnected()) {
            session.disconnect();
        }

        System.out.println("SSH连接已断开，命令记录已停止。");
    }

    /**
     * 使用示例和测试
     */
    public static void main(String[] args) {
        SSHCommandRecorder recorder = new SSHCommandRecorder();

        try {
            // 连接到SSH服务器
            System.out.println("正在连接SSH服务器...");
            recorder.connect("192.168.30.29", 22, "user", "123456+.");

            // 保持连接，让用户交互
            System.out.println("请在终端中执行命令，所有命令将被记录...");
            System.out.println("输入 'exit' 或按 Ctrl+C 结束记录");

            // 等待用户操作
            Scanner scanner = new Scanner(System.in);
            while (scanner.hasNextLine()) {
                String input = scanner.nextLine();
                if ("exit".equals(input)) {
                    break;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 导出审计报告
            try {
                List<CommandRecord> history = recorder.getCommandHistory();
                System.out.println("\n=== 命令执行历史 ===");
                history.forEach(System.out::println);

                recorder.exportAuditReport("ssh_audit_report.txt");
                System.out.println("审计报告已导出到: ssh_audit_report.txt");

            } catch (Exception e) {
                e.printStackTrace();
            }

            recorder.disconnect();
        }
    }
}
