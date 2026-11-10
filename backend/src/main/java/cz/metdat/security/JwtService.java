package cz.metdat.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import cz.metdat.config.AppProperties;
import cz.metdat.domain.AppUser;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private final SecretKeySpec key;
    private final Duration ttl;
    private final JwtEncoder encoder;

    public JwtService(AppProperties props) {
        String secret = props.security() == null ? null : props.security().jwtSecret();
        byte[] bytes;
        if (secret == null || secret.isBlank()) {
            bytes = new byte[48];
            new SecureRandom().nextBytes(bytes);
            log.warn("APP_JWT_SECRET is not set: using a random per-start secret; issued tokens become invalid on restart.");
        } else {
            bytes = secret.getBytes(StandardCharsets.UTF_8);
            if (bytes.length < 32) throw new IllegalStateException("APP_JWT_SECRET must be at least 32 characters");
        }
        this.key = new SecretKeySpec(bytes, "HmacSHA256");
        this.ttl = props.security() != null && props.security().tokenTtl() != null ? props.security().tokenTtl() : Duration.ofHours(12);
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
    }

    public record Token(String value, Instant expiresAt) {}

    public Token issue(AppUser user) {
        Instant now = Instant.now(), exp = now.plus(ttl);
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer("met-dat").subject(user.getEmail()).issuedAt(now).expiresAt(exp)
            .claim("uid", user.getId()).claim("name", user.getName()).claim("role", user.getRole().name()).build();
        String v = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new Token(v, exp);
    }

    public JwtDecoder decoder() { return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build(); }
}
