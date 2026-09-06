package com.aisaas.common.ai.agent.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 代码执行工具
 * 执行代码片段并返回结果
 */
@Slf4j
@Component
public class CodeExecutorTool implements Tool {

    private static final long DEFAULT_TIMEOUT_SECONDS = 30;
    private static final List<String> SUPPORTED_LANGUAGES = Arrays.asList(
            "java", "python", "javascript", "js", "typescript", "ts", "go", "rust", "c", "cpp", "csharp", "bash", "shell"
    );

    @Override
    public String getName() {
        return "code_executor";
    }

    @Override
    public String getDescription() {
        return "执行代码片段并返回执行结果。支持多种编程语言，包括Java、Python、JavaScript等。可以执行代码并捕获输出和错误信息。";
    }

    @Override
    public List<Parameter> getParameters() {
        return Arrays.asList(
                Parameter.builder()
                        .name("code")
                        .type("string")
                        .description("要执行的代码")
                        .required(true)
                        .build(),
                Parameter.builder()
                        .name("language")
                        .type("string")
                        .description("编程语言：java, python, javascript, go, rust, c, cpp, bash等")
                        .required(true)
                        .enumValues(SUPPORTED_LANGUAGES)
                        .build(),
                Parameter.builder()
                        .name("timeout")
                        .type("integer")
                        .description("执行超时时间（秒），默认30秒")
                        .required(false)
                        .defaultValue(30)
                        .build(),
                Parameter.builder()
                        .name("stdin")
                        .type("string")
                        .description("标准输入数据")
                        .required(false)
                        .build()
        );
    }

    @Override
    public ToolResult execute(Map<String, Object> params) {
        log.debug("Executing code: language={}, params={}", params.get("language"), params.keySet());

        try {
            // 提取参数
            String code = (String) params.get("code");
            String language = (String) params.get("language");
            Integer timeout = getIntParam(params, "timeout", (int) DEFAULT_TIMEOUT_SECONDS);
            String stdin = (String) params.getOrDefault("stdin", "");

            // 验证参数
            if (code == null || code.trim().isEmpty()) {
                return ToolResult.failure("代码不能为空");
            }

            if (language == null || language.trim().isEmpty()) {
                return ToolResult.failure("编程语言不能为空");
            }

            language = language.toLowerCase().trim();

            // 执行代码
            ExecutionResult result = executeCode(code, language, timeout, stdin);

            // 构建结果
            java.util.Map<String, Object> data = new java.util.HashMap<>();
            data.put("stdout", result.getStdout());
            data.put("stderr", result.getStderr());
            data.put("exitCode", result.getExitCode());
            data.put("executionTimeMs", result.getExecutionTimeMs());
            data.put("language", language);
            data.put("success", result.getExitCode() == 0 && result.getStderr().isEmpty());

            log.info("Code execution completed: language={}, exitCode={}, timeMs={}",
                    language, result.getExitCode(), result.getExecutionTimeMs());

            return ToolResult.success(data);

        } catch (Exception e) {
            log.error("Code execution failed", e);
            return ToolResult.failure("代码执行失败: " + e.getMessage());
        }
    }

    /**
     * 执行代码
     */
    private ExecutionResult executeCode(String code, String language, int timeout, String stdin) throws Exception {
        long startTime = System.currentTimeMillis();

        // 创建临时目录
        Path tempDir = Files.createTempDirectory("code_exec_");

        try {
            ProcessBuilder pb = new ProcessBuilder();

            // 根据语言设置执行命令
            switch (language) {
                case "python":
                case "py":
                    Path pyFile = tempDir.resolve("script.py");
                    Files.write(pyFile, code.getBytes());
                    pb.command("python", pyFile.toString());
                    break;

                case "java":
                    Path javaFile = tempDir.resolve("Main.java");
                    Files.write(javaFile, code.getBytes());
                    pb.command("bash", "-c", "cd " + tempDir + " && javac Main.java && java Main");
                    break;

                case "javascript":
                case "js":
                    Path jsFile = tempDir.resolve("script.js");
                    Files.write(jsFile, code.getBytes());
                    pb.command("node", jsFile.toString());
                    break;

                case "bash":
                case "shell":
                case "sh":
                    Path shFile = tempDir.resolve("script.sh");
                    Files.write(shFile, code.getBytes());
                    pb.command("bash", shFile.toString());
                    break;

                default:
                    throw new UnsupportedOperationException("Unsupported language: " + language);
            }

            // 设置工作目录
            pb.directory(tempDir.toFile());
            pb.redirectErrorStream(false);

            // 启动进程
            Process process = pb.start();

            // 写入stdin
            if (stdin != null && !stdin.isEmpty()) {
                process.getOutputStream().write(stdin.getBytes());
                process.getOutputStream().close();
            }

            // 等待进程完成
            boolean finished = process.waitFor(timeout, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException("Code execution timeout after " + timeout + " seconds");
            }

            // 读取输出
            String stdout = readStream(process.getInputStream());
            String stderr = readStream(process.getErrorStream());
            int exitCode = process.exitValue();

            long executionTime = System.currentTimeMillis() - startTime;

            return new ExecutionResult(stdout, stderr, exitCode, executionTime);

        } finally {
            // 清理临时文件
            deleteDirectory(tempDir.toFile());
        }
    }

    /**
     * 读取输入流
     */
    private String readStream(java.io.InputStream is) throws java.io.IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * 删除目录
     */
    private void deleteDirectory(java.io.File dir) {
        if (dir.isDirectory()) {
            java.io.File[] files = dir.listFiles();
            if (files != null) {
                for (java.io.File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        dir.delete();
    }

    private int getIntParam(Map<String, Object> params, String key, int defaultValue) {
        Object value = params.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 执行结果
     */
    private static class ExecutionResult {
        private final String stdout;
        private final String stderr;
        private final int exitCode;
        private final long executionTimeMs;

        public ExecutionResult(String stdout, String stderr, int exitCode, long executionTimeMs) {
            this.stdout = stdout;
            this.stderr = stderr;
            this.exitCode = exitCode;
            this.executionTimeMs = executionTimeMs;
        }

        public String getStdout() {
            return stdout;
        }

        public String getStderr() {
            return stderr;
        }

        public int getExitCode() {
            return exitCode;
        }

        public long getExecutionTimeMs() {
            return executionTimeMs;
        }
    }
}
