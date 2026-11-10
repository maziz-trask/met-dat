package cz.metdat.storage;

import cz.metdat.config.AppProperties;
import cz.metdat.exception.ValidationException;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/** Extension allow-list + declared MIME check + magic-byte sniffing + size limit + safe stored name. */
@Component
public class AttachmentValidator {
    /** A file that passed every check; {@code storedFilename} is server generated and never derived from user input. */
    public record ValidatedUpload(String originalFilename, String storedFilename, String contentType, byte[] bytes) {}

    private static final Map<String, String> CANONICAL_TYPE = Map.of(
        "pdf", "application/pdf", "csv", "text/csv", "txt", "text/plain",
        "xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "png", "image/png", "jpg", "image/jpeg", "jpeg", "image/jpeg");

    /** Browsers report different types for e.g. CSV, so several declared types are accepted per extension. */
    private static final Map<String, Set<String>> ACCEPTED_DECLARED_TYPES = Map.of(
        "pdf", Set.of("application/pdf"),
        "csv", Set.of("text/csv", "application/csv", "application/vnd.ms-excel", "text/plain"),
        "txt", Set.of("text/plain"),
        "xlsx", Set.of("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
        "docx", Set.of("application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
        "png", Set.of("image/png"), "jpg", Set.of("image/jpeg"), "jpeg", Set.of("image/jpeg"));

    private final AppProperties props;

    public AttachmentValidator(AppProperties props) { this.props = props; }

    public ValidatedUpload validate(MultipartFile file) {
        String original = sanitize(file.getOriginalFilename());
        String extension = extensionOf(original);
        List<String> allowed = props.upload().allowedExtensions().stream().map(s -> s.trim().toLowerCase(Locale.ROOT)).toList();
        if (extension.isEmpty() || !allowed.contains(extension) || !CANONICAL_TYPE.containsKey(extension)) {
            throw new ValidationException("files", "attachment.unsupportedType", original, String.join(", ", allowed));
        }
        if (file.isEmpty()) throw new ValidationException("files", "attachment.empty", original);
        long max = props.upload().maxFileSize().toBytes();
        if (file.getSize() > max) {
            throw new ValidationException("files", "attachment.tooLarge", original, String.valueOf(file.getSize() / 1024), String.valueOf(max / 1024 / 1024));
        }
        String declared = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT).split(";")[0].trim();
        if (!ACCEPTED_DECLARED_TYPES.get(extension).contains(declared)) {
            throw new ValidationException("files", "attachment.mimeMismatch", original, declared, extension);
        }
        byte[] bytes;
        try { bytes = file.getBytes(); } catch (IOException e) { throw new ValidationException("files", "attachment.unreadable", original); }
        if (!signatureMatches(extension, bytes)) throw new ValidationException("files", "attachment.badContent", original, extension);
        return new ValidatedUpload(original, UUID.randomUUID() + "." + extension, CANONICAL_TYPE.get(extension), bytes);
    }

    private static String extensionOf(String filename) {
        return filename.contains(".") ? filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT) : "";
    }

    /** Drops any directory part and control characters; the result is only ever used as a display/download name. */
    static String sanitize(String name) {
        String n = name == null ? "" : name.replace('\\', '/');
        n = n.substring(n.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "").trim();
        if (n.length() > 255) n = n.substring(n.length() - 255);
        if (n.isEmpty() || n.chars().allMatch(c -> c == '.')) throw new ValidationException("files", "attachment.badName");
        return n;
    }

    static boolean signatureMatches(String extension, byte[] b) {
        return switch (extension) {
            case "pdf" -> startsWith(b, '%', 'P', 'D', 'F', '-');
            case "png" -> startsWith(b, 0x89, 'P', 'N', 'G');
            case "jpg", "jpeg" -> startsWith(b, 0xFF, 0xD8, 0xFF);
            case "xlsx", "docx" -> startsWith(b, 'P', 'K', 3, 4);
            default -> isText(b);   // csv / txt
        };
    }

    private static boolean isText(byte[] b) {
        for (int i = 0; i < Math.min(b.length, 8192); i++) if (b[i] == 0) return false;
        return true;
    }

    private static boolean startsWith(byte[] b, int... signature) {
        if (b.length < signature.length) return false;
        for (int i = 0; i < signature.length; i++) if ((b[i] & 0xFF) != signature[i]) return false;
        return true;
    }
}
