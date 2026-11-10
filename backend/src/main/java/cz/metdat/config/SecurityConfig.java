package cz.metdat.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.metdat.domain.AppUser;
import cz.metdat.exception.ApiError;
import cz.metdat.i18n.Messages;
import cz.metdat.repository.AppUserRepository;
import cz.metdat.security.JwtService;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Stateless bearer-token security. Tokens are minted only by the server (password login, registration or demo login) and
 * signed with HS256. The caller's role is NOT trusted from the token: it is re-read from the database on every request,
 * so role changes and deactivation take effect immediately.
 * Reads need any authenticated user; every write needs the Editor role (decided before request-body validation),
 * and write methods additionally carry @PreAuthorize("hasRole('EDITOR')").
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    private static final String[] PUBLIC_AUTH = {
        "/api/auth/config", "/api/auth/demo-users", "/api/auth/demo-login", "/api/auth/login", "/api/auth/register"};

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, ObjectMapper mapper, Messages messages, AppUserRepository users) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)   // no cookies/sessions: bearer tokens only
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(PUBLIC_AUTH).permitAll()
                .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/**").authenticated()
                .requestMatchers("/api/**").hasRole("EDITOR")
                .anyRequest().permitAll())   // static SPA assets
            .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(jwtConverter(users)))
                .authenticationEntryPoint((req, res, ex) -> write(mapper, messages, res, req.getRequestURI(), HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "error.unauthenticated"))
                .accessDeniedHandler((req, res, ex) -> write(mapper, messages, res, req.getRequestURI(), HttpStatus.FORBIDDEN, "FORBIDDEN", "error.forbidden")))
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req, res, ex) -> write(mapper, messages, res, req.getRequestURI(), HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "error.unauthenticated"))
                .accessDeniedHandler((req, res, ex) -> write(mapper, messages, res, req.getRequestURI(), HttpStatus.FORBIDDEN, "FORBIDDEN", "error.forbidden")));
        return http.build();
    }

    private static void write(ObjectMapper mapper, Messages messages, HttpServletResponse res, String path, HttpStatus status,
                              String code, String messageKey) throws IOException {
        res.setStatus(status.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        mapper.writeValue(res.getOutputStream(), new ApiError(Instant.now(), status.value(), code, messages.get(messageKey), path, List.of()));
    }

    /** Authorities come from the CURRENT database row of the token's subject; unknown or inactive users are rejected. */
    private JwtAuthenticationConverter jwtConverter(AppUserRepository users) {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            AppUser user = users.findByEmailIgnoreCase(jwt.getSubject()).filter(AppUser::isActive)
                .orElseThrow(() -> new BadCredentialsException("Unknown or inactive user"));
            return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        });
        return converter;
    }

    @Bean
    JwtDecoder jwtDecoder(JwtService jwtService) { return jwtService.decoder(); }

    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(10); }

    @Bean
    CorsConfigurationSource corsConfigurationSource(AppProperties props) {
        CorsConfiguration cfg = new CorsConfiguration();
        List<String> origins = props.cors() == null || props.cors().allowedOrigins() == null ? List.of() : props.cors().allowedOrigins();
        cfg.setAllowedOrigins(origins.stream().filter(o -> !o.isBlank()).toList());
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Accept-Language"));
        cfg.setExposedHeaders(List.of("Content-Disposition"));
        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/api/**", cfg);
        return src;
    }
}
