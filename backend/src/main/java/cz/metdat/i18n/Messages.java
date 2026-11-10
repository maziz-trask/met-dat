package cz.metdat.i18n;

import cz.metdat.exception.AppException;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

/** Resolves message keys in the language of the current request (Accept-Language: cs or en, default en). */
@Component
public class Messages {
    private final MessageSource source;

    public Messages(MessageSource source) { this.source = source; }

    public String get(String key, Object... args) {
        Object[] resolved = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            resolved[i] = args[i] instanceof MsgRef ref ? get(ref.key()) : args[i];
        }
        return source.getMessage(key, resolved, key, LocaleContextHolder.getLocale());
    }

    public String get(AppException e) { return get(e.getKey(), e.getArgs()); }
}
