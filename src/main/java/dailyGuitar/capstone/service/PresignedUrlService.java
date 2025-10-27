package dailyGuitar.capstone.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.net.URL;
import java.time.Duration;
import java.util.UUID;

@Service
public class PresignedUrlService {
	private final S3Presigner presigner;

	@Value("${S3_BUCKET}")
	private String bucket;

	@Value("${S3_PROFILE_PREFIX:profiles/}")
	private String prefix;

	public PresignedUrlService(S3Presigner presigner) {
		this.presigner = presigner;
	}

	public Result createUploadUrl(String contentType, String originalFilename) {
		String ext = extractExtension(originalFilename);
		String objectKey = prefix + UUID.randomUUID() + (ext.isEmpty() ? "" : ("." + ext));

		PutObjectRequest putObjectRequest = PutObjectRequest.builder()
				.bucket(bucket)
				.key(objectKey)
				.contentType(contentType)
				.build();

		PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
				.signatureDuration(Duration.ofMinutes(5))
				.putObjectRequest(putObjectRequest)
				.build();

		URL url = presigner.presignPutObject(presignRequest).url();
		return new Result(url.toString(), objectKey);
	}

	private String extractExtension(String filename) {
		if (filename == null) return "";
		int idx = filename.lastIndexOf('.');
		return (idx >= 0 && idx + 1 < filename.length()) ? filename.substring(idx + 1) : "";
	}

	public String createDownloadUrl(String objectKey) {
		GetObjectRequest getObjectRequest = GetObjectRequest.builder()
				.bucket(bucket)
				.key(objectKey)
				.build();

		GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
				.signatureDuration(Duration.ofHours(1)) // 다운로드는 1시간 유효
				.getObjectRequest(getObjectRequest)
				.build();

		URL url = presigner.presignGetObject(presignRequest).url();
		return url.toString();
	}

	public record Result(String uploadUrl, String objectKey) { }
}
