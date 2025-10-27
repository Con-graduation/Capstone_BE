package dailyGuitar.capstone.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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
	
	@Value("${ai.python.script.path}")
	private String pythonScriptPath;
	
	/**
	 * Python 스크립트를 실행하여 음원 파일을 분석합니다.
	 * 
	 * @param audioFilePath 분석할 오디오 파일의 경로
	 * @return Python 스크립트의 출력 결과 
	 * @throws IOException 스크립트 실행 중 오류 발생
	 * @throws InterruptedException 프로세스가 중단됨
	 */
	public String analyzeAudio(String audioFilePath) throws IOException, InterruptedException {
		// Python 스크립트 경로 확인
		File scriptFile = new File(pythonScriptPath);
		if (!scriptFile.exists()) {
			throw new IllegalArgumentException("Python script not found: " + pythonScriptPath);
		}
		
		// ProcessBuilder 생성
		List<String> command = new ArrayList<>();
		command.add("python3");
		command.add(pythonScriptPath);
		command.add(audioFilePath);
		
		ProcessBuilder processBuilder = new ProcessBuilder(command);
		processBuilder.redirectErrorStream(true); // stderr를 stdout으로 병합
		
		// 프로세스 실행
		Process process = processBuilder.start();
		
		// 표준 출력 읽기
		StringBuilder output = new StringBuilder();
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				output.append(line).append("\n");
			}
		}
		
		// 프로세스 종료 대기 (최대 30초)
		boolean finished = process.waitFor(30, TimeUnit.SECONDS);
		if (!finished) {
			process.destroyForcibly();
			throw new RuntimeException("Python script timeout (30 seconds)");
		}
		
		// 종료 코드 확인
		int exitCode = process.exitValue();
		if (exitCode != 0) {
			throw new RuntimeException("Python script failed with exit code: " + exitCode + "\nOutput: " + output);
		}
		
		return output.toString().trim();
	}
}

