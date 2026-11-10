package cz.metdat.storage;

import cz.metdat.config.AppProperties;
import cz.metdat.exception.ValidationException;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/** Extension allow-list + declared MIME check + magic-byte sniffing + size limit + safe stored name. */
@Component
public class AttachmentValidator {
    public record ValidatedUpload(String originalFilename, String storedFilename, String contentType, byte[] bytes) {}

    static final Map<String, String> CANONICAL = Map.of(
        "pdf", "application/pdf", "csv", "text/csv", "txt", "text/plain",
        "xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "png", "image/png", "jpg", "image/jpeg", "jpeg", "image/jpeg");

    private static final Map<String, Set<String>> ACCEPTED_DECLARED = Map.of(
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
        String ext = original.contains(".") ? original.substring(original.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT) : "";
        var allowed = props.upload().allowedExtensions().stream().map(s -> s.trim().toLowerCase(Locale.ROOT)).toList();
        if (ext.isEmpty() || !allowed.contains(ext) || !CANONICAL.containsKey(ext)) {
            throw new ValidationException("files", "File '" + original + "' has an unsupported type. Allowed extensions: " + String.join(", ", allowed) + ".");
        }
        long max = props.upload().maxFileSize().toBytes();
        if (file.isEmpty()) throw new ValidationException("files", "File '" + original + "' is empty.");
        if (file.getSize() > max) {
            throw new ValidationException("files", "File '" + original + "' is too large (" + (file.getSize() / 1024) + " KB). Maximum size is " + (max / 1024 / 1024) + " MB.");
        }
        String declared = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT).split(";")[0].trim();
        if (!ACCEPTED_DECLARED.get(ext).contains(declared)) {
            throw new ValidationException("files", "File '" + original + "': content type '" + declared + "' does not match the extension ." + ext + ".");
        }
        byte[] bytes;
        try { bytes = file.getBytes(); } catch (IOException e) { throw new ValidationException("files", "File '" + original + "' could not be read."); }
        if (!magicOk(ext, bytes)) {
            throw new ValidationException("files", "File '" + original + "': the content does not look like a valid ." + ext + " file.");
        }
        return new ValidatedUpload(original, UUID.randomUUID() + "." + ext, CANONICAL.get(ext), bytes);
    }

    /** Drops any directory part and control characters; the result is only ever used as a display/download name. */
    static String sanitize(String name) {
        String n = name == null ? "" : name.replace('\\', '/');
        n = n.substring(n.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "").trim();
        if (n.length() > 255) n = n.substring(n.length() - 255);
        if (n.isEmpty() || n.chars().allMatch(c -> c == '.')) throw new ValidationException("files", "Invalid file name.");
        return n;
    }

    static boolean magicOk(String ext, byte[] b) {
        return switch (ext) {
            case "pdf" -> startsWith(b, '%', 'P', 'D', 'F', '-');
            case "png" -> startsWith(b, 0x89, 'P', 'N', 'G');
            case "jpg", "jpeg" -> startsWith(b, 0xFF, 0xD8, 0xFF);
            case "xlsx", "docx" -> startsWith(b, 'P', 'K', 3, 4);
            default -> { // csv / txt: must be text (no NUL bytes in the first 8 KB)
                for (int i = 0; i < Math.min(b.length, 8192); i++) if (b[i] == 0) yield false;
                yield true;
            }
        };
    }

    private static boolean startsWith(byte[] b, int... sig) {
        if (b.length < sig.length) return false;
        for (int i = 0; i < sig.length; i++) if ((b[i] & 0xFF) != sig[i]) return false;
        return true;
    }
}
