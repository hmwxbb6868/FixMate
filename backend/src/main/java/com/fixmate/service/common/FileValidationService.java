package com.fixmate.service.common;

import com.fixmate.exception.BusinessException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileValidationService {

    private static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;
    private static final long MAX_AUDIO_BYTES = 20L * 1024 * 1024;
    private static final long MAX_AUDIO_SECONDS = 120;
    private static final int HEADER_BYTES = 16;

    private static final Set<String> IMAGE_PURPOSES = Set.of("REPAIR_MEDIA", "NAME_PLATE", "AVATAR");
    private static final Set<String> AUDIO_PURPOSES = Set.of("AUDIO");

    public ValidatedFile validate(MultipartFile file, String purpose) {
        PreparedUpload prepared = prepare(file, purpose);
        try {
            return prepared.file();
        } finally {
            prepared.delete();
        }
    }

    /**
     * 流式校验：边读边算 SHA-256 与大小，落到受限临时文件；不把整个文件读进堆。
     * 调用方负责在使用后调用 {@link PreparedUpload#delete()}。
     */
    public PreparedUpload prepare(MultipartFile file, String purpose) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.validation("文件为空");
        }
        boolean image = IMAGE_PURPOSES.contains(purpose);
        boolean audio = AUDIO_PURPOSES.contains(purpose);
        if (!image && !audio) {
            throw BusinessException.validation("不支持的上传用途");
        }
        long limit = image ? MAX_IMAGE_BYTES : MAX_AUDIO_BYTES;
        if (file.getSize() > limit) {
            throw tooLarge(image);
        }

        String ext = extensionOf(file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
        Path temp = null;
        try {
            // 音频时长解析依赖扩展名，临时文件必须保留原始后缀
            String suffix = ext.matches("[a-z0-9]{1,8}") ? "." + ext : ".tmp";
            temp = Files.createTempFile("fixmate-upload-", suffix);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            long total = copyWithLimit(file, temp, digest, limit, image);

            byte[] header = readHeader(temp);
            String mime = image ? detectImageMime(header) : detectAudioMime(header);
            if (mime == null || !matchesExtension(ext, mime)) {
                throw image
                        ? BusinessException.validation("仅支持 jpg/png/webp 图片")
                        : BusinessException.validation("仅支持 mp3/wav/m4a 音频");
            }
            if (!typeConsistent(file.getContentType(), mime)) {
                throw BusinessException.validation("文件类型与内容不一致");
            }
            if (audio) {
                long duration = audioDurationSeconds(temp);
                if (duration <= 0) {
                    throw BusinessException.validation("无法读取音频时长");
                }
                if (duration > MAX_AUDIO_SECONDS) {
                    throw BusinessException.validation("音频不能超过 120 秒");
                }
            }
            ValidatedFile validated = new ValidatedFile(mime, HexFormat.of().formatHex(digest.digest()),
                    total, ext);
            PreparedUpload prepared = new PreparedUpload(validated, temp);
            temp = null; // 所有权转移给调用方
            return prepared;
        } catch (IOException ex) {
            throw BusinessException.validation("文件读取失败");
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        } finally {
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException ignored) {
                    // 临时文件由操作系统回收
                }
            }
        }
    }

    private long copyWithLimit(MultipartFile file, Path target, MessageDigest digest, long limit, boolean image)
            throws IOException {
        long total = 0;
        try (InputStream in = new BufferedInputStream(file.getInputStream());
             DigestInputStream digestIn = new DigestInputStream(in, digest);
             OutputStream out = new BufferedOutputStream(Files.newOutputStream(target))) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = digestIn.read(buffer)) != -1) {
                total += read;
                if (total > limit) {
                    throw tooLarge(image);
                }
                out.write(buffer, 0, read);
            }
        }
        return total;
    }

    private byte[] readHeader(Path path) throws IOException {
        byte[] header = new byte[HEADER_BYTES];
        try (InputStream in = Files.newInputStream(path)) {
            int read = in.readNBytes(header, 0, HEADER_BYTES);
            if (read < HEADER_BYTES) {
                byte[] shorter = new byte[read];
                System.arraycopy(header, 0, shorter, 0, read);
                return shorter;
            }
        }
        return header;
    }

    private BusinessException tooLarge(boolean image) {
        return BusinessException.validation(image ? "图片不能超过 10 MB" : "音频不能超过 20 MB");
    }

    /**
     * 客户端声明类型只做"互相矛盾"拦截：小程序、curl 等常传 application/octet-stream，
     * 此时以 Magic Number 为准；显式声明为另一种具体类型则拒绝。
     */
    private boolean typeConsistent(String declaredType, String detectedMime) {
        if (declaredType == null || declaredType.isBlank()) {
            return true;
        }
        String declared = declaredType.toLowerCase(Locale.ROOT).split(";")[0].trim();
        if (declared.equals("application/octet-stream") || declared.equals("*/*")
                || declared.equals("binary/octet-stream")) {
            return true;
        }
        return declared.equalsIgnoreCase(detectedMime);
    }

    private String extensionOf(String name) {
        int idx = name.lastIndexOf('.');
        return idx < 0 ? "" : name.substring(idx + 1).toLowerCase(Locale.ROOT);
    }

    private boolean matchesExtension(String ext, String mime) {
        return switch (mime) {
            case "image/jpeg" -> ext.equals("jpg") || ext.equals("jpeg");
            case "image/png" -> ext.equals("png");
            case "image/webp" -> ext.equals("webp");
            case "audio/mpeg" -> ext.equals("mp3");
            case "audio/wav" -> ext.equals("wav");
            case "audio/mp4" -> ext.equals("m4a");
            default -> false;
        };
    }

    /** PNG 必须匹配完整 8 字节签名，只比对前四字节会放过伪造文件。 */
    private String detectImageMime(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                && b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A) {
            return "image/png";
        }
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return "image/webp";
        }
        return null;
    }

    private String detectAudioMime(byte[] b) {
        if (b.length >= 3 && b[0] == 'I' && b[1] == 'D' && b[2] == '3') {
            return "audio/mpeg";
        }
        if (b.length >= 2 && (b[0] & 0xFF) == 0xFF && ((b[1] & 0xFF) == 0xFB || (b[1] & 0xFF) == 0xF3
                || (b[1] & 0xFF) == 0xF2)) {
            return "audio/mpeg";
        }
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'A' && b[10] == 'V' && b[11] == 'E') {
            return "audio/wav";
        }
        if (b.length >= 12 && b[4] == 'f' && b[5] == 't' && b[6] == 'y' && b[7] == 'p') {
            return "audio/mp4";
        }
        return null;
    }

    /** 读取音频时长（秒）；无法解析时返回 0，由调用方按拒绝处理。 */
    private long audioDurationSeconds(Path path) {
        try {
            org.jaudiotagger.audio.AudioFile audioFile =
                    org.jaudiotagger.audio.AudioFileIO.read(path.toFile());
            return audioFile.getAudioHeader().getTrackLength();
        } catch (Exception ex) {
            return 0;
        }
    }

    public record ValidatedFile(String mimeType, String sha256, long size, String extension) {
    }

    public record PreparedUpload(ValidatedFile file, Path tempPath) {
        public void delete() {
            try {
                Files.deleteIfExists(tempPath);
            } catch (IOException ignored) {
                // 由操作系统回收
            }
        }
    }
}
