package cz.metdat.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.metdat.exception.ApiError;
import cz.metdat.security.JwtService;
import java.time.Instant;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Stateless bearer-token security. Tokens are minted only by the server (POST /api/auth/demo-login) and signed with
 * HS256; the role claim is read from the AppUser row at login time, never from client input.
 * Read endpoints need any authenticated user; write endpoints are additionally guarded with @PreAuthorize("hasRole('EDITOR')").
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, ObjectMapper mapper) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)   // no cookies/sessions: bearer tokens only
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/auth/demo-users", "/api/auth/demo-login").permitAll()
                .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/**").authenticated()
                .requestMatchers("/api/**").hasRole("EDITOR")   // every write (POST/PUT/PATCH/DELETE): decided before any body validation
                .anyRequest().permitAll())   // static SPA assets
            .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(jwtConverter()))
                .authenticationEntryPoint((req, res, ex) -> write(mapper, res, req.getRequestURI(), HttpStatus.UNAUTHORIZED,
                        "UNAUTHENTICATED", "Authentication required. Sign in as a demo user first."))
                .accessDeniedHandler((req, res, ex) -> write(mapper, res, req.getRequestURI(), HttpStatus.FORBIDDEN,
                        "FORBIDDEN", "Your role is not allowed to perform this action.")))
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req, res, ex) -> write(mapper, res, req.getRequestURI(), HttpStatus.UNAUTHORIZED,
                        "UNAUTHENTICATED", "Authentication required. Sign in as a demo user first."))
                .accessDeniedHandler((req, res, ex) -> write(mapper, res, req.getRequestURI(), HttpStatus.FORBIDDEN,
                        "FORBIDDEN", "Your role is not allowed to perform this action.")));
        return http.build();
    }

    private static void write(ObjectMapper mapper, jakarta.servlet.http.HttpServletResponse res, String path,
                              HttpStatus status, String code, String message) throws java.io.IOException {
        res.setStatus(status.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(res.getOutputStream(), new ApiError(Instant.now(), status.value(), code, message, path, List.of()));
    }

    private JwtAuthenticationConverter jwtConverter() {
        JwtAuthenticationConverter c = new JwtAuthenticationConverter();
        c.setJwtGrantedAuthoritiesConverter(jwt -> {
            String role = jwt.getClaimAsString("role");
            return role == null ? List.<GrantedAuthority>of() : List.<GrantedAuthority>of(new SimpleGrantedAuthority("ROLE_" + role));
        });
        return c;
    }

    @Bean
    JwtDecoder jwtDecoder(JwtService jwtService) { return jwtService.decoder(); }

    @Bean
    CorsConfigurationSource corsConfigurationSource(AppProperties props) {
        CorsConfiguration cfg = new CorsConfiguration();
        List<String> origins = props.cors() == null || props.cors().allowedOrigins() == null ? List.of() : props.cors().allowedOrigins();
        cfg.setAllowedOrigins(origins.stream().filter(o -> !o.isBlank()).toList());
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        cfg.setExposedHeaders(List.of("Content-Disposition"));
        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/api/**", cfg);
        return src;
    }
}
