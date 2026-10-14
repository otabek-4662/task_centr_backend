package com.taskcenter.service.storage;

import com.taskcenter.dto.FileUploadResponse;
import com.taskcenter.exception.BadRequestException;
import com.taskcenter.exception.ResourceNotFoundException;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "storage.type", havingValue = "s3")
public class S3FileStorageService implements FileStorageService {

    private static final List<String> FORBIDDEN_EXTENSIONS = List.of("exe", "bat", "sh", "cmd", "php", "js", "html");
    private static final List<String> ALLOWED_EXTENSIONS = List.of(
            "jpg", "jpeg", "png", "gif", "webp", "svg", "pdf", "doc", "docx", "xls", "xlsx", "txt", "csv", "zip"
    );
    private static final DateTimeFormatter AMZ_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
    private static final DateTimeFormatter SHORT_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final String endpoint;
    private final String region;
    private final String bucket;
    private final String accessKey;
    private final String secretKey;
    private final String publicUrl;
    private final HttpClient httpClient;

    public S3FileStorageService(
            @Value("${storage.s3.endpoint:}") String endpoint,
            @Value("${storage.s3.region:auto}") String region,
            @Value("${storage.s3.bucket:task-center-files}") String bucket,
            @Value("${storage.s3.access-key:}") String accessKey,
            @Value("${storage.s3.secret-key:}") String secretKey,
            @Value("${storage.s3.public-url:}") String publicUrl) {
        this.endpoint = endpoint != null ? endpoint.replaceAll("/+$", "") : "";
        this.region = (region != null && !region.isBlank()) ? region.trim() : "us-east-1";
        this.bucket = bucket != null ? bucket.trim() : "task-center-files";
        this.accessKey = accessKey != null ? accessKey.trim() : "";
        this.secretKey = secretKey != null ? secretKey.trim() : "";
        this.publicUrl = publicUrl != null ? publicUrl.replaceAll("/+$", "") : "";
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public String storeFile(MultipartFile file, String subDirectory) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Yuklanayotgan fayl bo'sh bo'lishi mumkin emas");
        }
        String originalFilename = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "file"
        );
        if (originalFilename.contains("..")) {
            throw new BadRequestException("Fayl nomida noqonuniy belgilar mavjud: " + originalFilename);
        }
        validateExtension(originalFilename);

        String cleanSub = (subDirectory != null && !subDirectory.isBlank())
                ? subDirectory.trim().replaceAll("^/+|/+$", "")
                : "";
        String uniqueName = UUID.randomUUID() + "_" + originalFilename;
        String objectKey = cleanSub.isEmpty() ? uniqueName : (cleanSub + "/" + uniqueName);
        String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";

        try {
            putObject(objectKey, file.getBytes(), contentType);
            return objectKey;
        } catch (Exception ex) {
            throw new RuntimeException("S3 xotirasiga fayl yuklashda xatolik: " + originalFilename, ex);
        }
    }

    @Override
    public FileUploadResponse storeFileWithThumbnail(MultipartFile file, String subDirectory) {
        String storedKey = storeFile(file, subDirectory);
        String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
        String fileUrl = !publicUrl.isEmpty() ? (publicUrl + "/" + storedKey) : ("/api/files/" + storedKey);

        FileUploadResponse response = FileUploadResponse.builder()
                .fileUrl(fileUrl)
                .fileType(contentType)
                .fileSize(file.getSize())
                .build();

        if (contentType.startsWith("image/") && !contentType.contains("svg")) {
            try {
                ByteArrayOutputStream thumbOut = new ByteArrayOutputStream();
                Thumbnails.of(new ByteArrayInputStream(file.getBytes()))
                        .size(200, 200)
                        .toOutputStream(thumbOut);
                byte[] thumbBytes = thumbOut.toByteArray();

                int slashIdx = storedKey.lastIndexOf('/');
                String thumbKey = (slashIdx >= 0)
                        ? storedKey.substring(0, slashIdx + 1) + "thumb_" + storedKey.substring(slashIdx + 1)
                        : "thumb_" + storedKey;

                putObject(thumbKey, thumbBytes, contentType);
                response.setThumbnailUrl(!publicUrl.isEmpty() ? (publicUrl + "/" + thumbKey) : ("/api/files/" + thumbKey));
            } catch (Exception ignored) {
            }
        }

        return response;
    }

    @Override
    public Resource loadFileAsResource(String storagePath) {
        if (storagePath == null || storagePath.isBlank() || storagePath.contains("..")) {
            throw new BadRequestException("Noqonuniy fayl yo'li: " + storagePath);
        }
        try {
            byte[] data = getObject(storagePath);
            final String filename = storagePath.contains("/")
                    ? storagePath.substring(storagePath.lastIndexOf('/') + 1)
                    : storagePath;
            return new ByteArrayResource(data) {
                @Override
                public String getFilename() {
                    return filename;
                }
            };
        } catch (ResourceNotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResourceNotFoundException("Fayl topilmadi: " + storagePath);
        }
    }

    @Override
    public void deleteFile(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) {
            return;
        }
        try {
            deleteObject(storagePath);
        } catch (Exception ignored) {
        }
    }

    private void putObject(String key, byte[] payload, String contentType) throws Exception {
        URI uri = buildObjectUri(key);
        HttpRequest request = signRequest("PUT", uri, payload, contentType);
        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() >= 300) {
            throw new RuntimeException("S3 PUT xatoligi: HTTP " + response.statusCode());
        }
    }

    private byte[] getObject(String key) throws Exception {
        URI uri = buildObjectUri(key);
        HttpRequest request = signRequest("GET", uri, new byte[0], null);
        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() == 404) {
            throw new ResourceNotFoundException("Fayl topilmadi: " + key);
        }
        if (response.statusCode() >= 300) {
            throw new RuntimeException("S3 GET xatoligi: HTTP " + response.statusCode());
        }
        return response.body();
    }

    private void deleteObject(String key) throws Exception {
        URI uri = buildObjectUri(key);
        HttpRequest request = signRequest("DELETE", uri, new byte[0], null);
        httpClient.send(request, HttpResponse.BodyHandlers.discarding());
    }

    private URI buildObjectUri(String key) {
        String cleanKey = key.replaceAll("^/+", "");
        String base = !endpoint.isEmpty() ? endpoint : ("https://s3." + region + ".amazonaws.com");
        return URI.create(base + "/" + bucket + "/" + cleanKey);
    }

    private HttpRequest signRequest(String method, URI uri, byte[] payload, String contentType) throws Exception {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        String amzDate = AMZ_DATE_FORMAT.format(now);
        String dateStamp = SHORT_DATE_FORMAT.format(now);
        String host = uri.getHost() + (uri.getPort() > 0 ? ":" + uri.getPort() : "");
        String payloadHash = sha256Hex(payload);

        String canonicalUri = uri.getRawPath();
        String canonicalHeaders = "host:" + host + "\n"
                + "x-amz-content-sha256:" + payloadHash + "\n"
                + "x-amz-date:" + amzDate + "\n";
        String signedHeaders = "host;x-amz-content-sha256;x-amz-date";

        String canonicalRequest = method + "\n"
                + canonicalUri + "\n"
                + "\n"
                + canonicalHeaders + "\n"
                + signedHeaders + "\n"
                + payloadHash;

        String credentialScope = dateStamp + "/" + region + "/s3/aws4_request";
        String stringToSign = "AWS4-HMAC-SHA256\n"
                + amzDate + "\n"
                + credentialScope + "\n"
                + sha256Hex(canonicalRequest.getBytes(StandardCharsets.UTF_8));

        byte[] signingKey = getSignatureKey(secretKey, dateStamp, region, "s3");
        String signature = HexFormat.of().formatHex(hmacSha256(signingKey, stringToSign));

        String authorizationHeader = "AWS4-HMAC-SHA256 Credential=" + accessKey + "/" + credentialScope
                + ", SignedHeaders=" + signedHeaders
                + ", Signature=" + signature;

        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(30))
                .header("x-amz-date", amzDate)
                .header("x-amz-content-sha256", payloadHash)
                .header("Authorization", authorizationHeader);

        if (contentType != null && !contentType.isBlank()) {
            builder.header("Content-Type", contentType);
        }

        if ("PUT".equals(method)) {
            builder.PUT(HttpRequest.BodyPublishers.ofByteArray(payload));
        } else if ("DELETE".equals(method)) {
            builder.DELETE();
        } else {
            builder.GET();
        }
        return builder.build();
    }

    private static String sha256Hex(byte[] data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(data));
    }

    private static byte[] hmacSha256(byte[] key, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] getSignatureKey(String key, String dateStamp, String regionName, String serviceName) throws Exception {
        byte[] kSecret = ("AWS4" + key).getBytes(StandardCharsets.UTF_8);
        byte[] kDate = hmacSha256(kSecret, dateStamp);
        byte[] kRegion = hmacSha256(kDate, regionName);
        byte[] kService = hmacSha256(kRegion, serviceName);
        return hmacSha256(kService, "aws4_request");
    }

    private void validateExtension(String filename) {
        String ext = StringUtils.getFilenameExtension(filename);
        if (ext != null) {
            String lower = ext.toLowerCase();
            if (FORBIDDEN_EXTENSIONS.contains(lower)) {
                throw new BadRequestException("Bunday turdagi fayllarni yuklash taqiqlangan!");
            }
            if (!ALLOWED_EXTENSIONS.contains(lower)) {
                throw new BadRequestException("Faqat ruxsat etilgan rasm yoki hujjat formatlari qabul qilinadi!");
            }
        }
    }
}
