package dailyGuitar.capstone.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class AudioAnalysisService {
    private static final Logger log = LoggerFactory.getLogger(AudioAnalysisService.class);
	
    @Value("${ai.python.executable:python3}")
    private String pythonExecutable;
	@Value("${ai.python.script.path}")
	private String pythonScriptPath;
    @Value("${ai.viz.dir:}")
    private String vizDir;
	
	/**
	 * Python 스크립트를 실행하여 음원 파일을 분석합니다.
	 * 
	 * @param audioFilePath 분석할 오디오 파일의 경로
	 * @return Python 스크립트의 출력 결과 
	 * @throws IOException 스크립트 실행 중 오류 발생
	 * @throws InterruptedException 프로세스가 중단됨
	 */
    public String analyzeAudio(String audioFilePath) throws IOException, InterruptedException {
        return analyzeWithArgs(List.of("--audio", audioFilePath));
    }

    /**
     * 추가 인자와 함께 파이썬 스크립트를 실행합니다.
     * 인자는 스크립트 인자 형태 그대로 전달하세요. 예) ["--mode","chord","--audio","/tmp/a.wav",...]
     */
    public String analyzeWithArgs(List<String> args) throws IOException, InterruptedException {
        File scriptFile = new File(pythonScriptPath);
        if (!scriptFile.exists()) {
            throw new IllegalArgumentException("Python script not found: " + pythonScriptPath);
        }

        List<String> command = new ArrayList<>();
        command.add(pythonExecutable);
        command.add(pythonScriptPath);
        command.addAll(args);

        ProcessBuilder processBuilder = new ProcessBuilder(command);
		processBuilder.redirectErrorStream(true); // stderr를 stdout으로 병합
		
        // 프로세스 실행
        long startNs = System.nanoTime();
        log.info("[analysis] exec -> {}", String.join(" ", command));
        Process process = processBuilder.start();

        // 표준 출력 읽기 (실시간 스트리밍)
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                log.info("[analysis][out] {}", line);
            }
        }
		
		// 프로세스 종료 대기 (최대 30초)
        boolean finished = process.waitFor(60, TimeUnit.SECONDS);
		if (!finished) {
			process.destroyForcibly();
            throw new RuntimeException("Python script timeout (60 seconds)");
		}
		
		// 종료 코드 확인
		int exitCode = process.exitValue();
		if (exitCode != 0) {
			throw new RuntimeException("Python script failed with exit code: " + exitCode + "\nOutput: " + output);
		}
        long tookMs = (System.nanoTime() - startNs) / 1_000_000;
        log.info("[analysis] done ({} ms)", tookMs);
        return output.toString().trim();
	}
}

