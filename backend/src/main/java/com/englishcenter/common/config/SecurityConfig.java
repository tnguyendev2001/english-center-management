package com.englishcenter.common.config;

import com.englishcenter.auth.security.AccountJwtAuthenticationConverter;
import com.englishcenter.auth.security.JsonAccessDeniedHandler;
import com.englishcenter.auth.security.JsonAuthEntryPoint;
import com.englishcenter.auth.security.MustChangePasswordFilter;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties({
        CorsProperties.class,
        FinanceProperties.class,
        AppTimeProperties.class,
        JwtProperties.class,
        AdminBootstrapProperties.class
})
public class SecurityConfig {
    private static final int MIN_SECRET_BYTES = 32;

    private final CorsProperties corsProperties;
    private final JwtProperties jwtProperties;
    private final JsonAuthEntryPoint jsonAuthEntryPoint;
    private final JsonAccessDeniedHandler jsonAccessDeniedHandler;
    private final MustChangePasswordFilter mustChangePasswordFilter;
    private final AccountJwtAuthenticationConverter accountJwtAuthenticationConverter;

    public SecurityConfig(
            CorsProperties corsProperties,
            JwtProperties jwtProperties,
            JsonAuthEntryPoint jsonAuthEntryPoint,
            JsonAccessDeniedHandler jsonAccessDeniedHandler,
            MustChangePasswordFilter mustChangePasswordFilter,
            AccountJwtAuthenticationConverter accountJwtAuthenticationConverter
    ) {
        this.corsProperties = corsProperties;
        this.jwtProperties = jwtProperties;
        this.jsonAuthEntryPoint = jsonAuthEntryPoint;
        this.jsonAccessDeniedHandler = jsonAccessDeniedHandler;
        this.mustChangePasswordFilter = mustChangePasswordFilter;
        this.accountJwtAuthenticationConverter = accountJwtAuthenticationConverter;
        validateJwtSecret(jwtProperties.getSecret());
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthConverter =
                accountJwtAuthenticationConverter::convert;

        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/finance/**").hasRole("ADMIN")
                        .requestMatchers("/api/imports/**").hasRole("ADMIN")
                        .requestMatchers("/api/revenue/**").hasRole("ADMIN")
                        .requestMatchers("/api/auth/**").authenticated()
                        .requestMatchers("/api/me/**").authenticated()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter))
                        .authenticationEntryPoint(jsonAuthEntryPoint)
                        .accessDeniedHandler(jsonAccessDeniedHandler))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jsonAuthEntryPoint)
                        .accessDeniedHandler(jsonAccessDeniedHandler))
                .addFilterAfter(mustChangePasswordFilter, BearerTokenAuthenticationFilter.class)
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    JwtEncoder jwtEncoder() {
        SecretKey secretKey = secretKey();
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
    }

    @Bean
    JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey())
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        OAuth2TokenValidator<Jwt> defaultValidator = JwtValidators.createDefaultWithIssuer(jwtProperties.getIssuer());
        OAuth2TokenValidator<Jwt> audienceValidator = token -> {
            if (token.getAudience() != null && token.getAudience().contains(jwtProperties.getAudience())) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "Invalid audience", null)
            );
        };
        OAuth2TokenValidator<Jwt> requiredClaimsValidator = token -> {
            if (token.getClaim("username") == null
                    || token.getClaim("role") == null
                    || token.getClaim("ver") == null
                    || token.getId() == null) {
                return OAuth2TokenValidatorResult.failure(
                        new OAuth2Error("invalid_token", "Missing required claims", null)
                );
            }
            return OAuth2TokenValidatorResult.success();
        };

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                defaultValidator,
                audienceValidator,
                requiredClaimsValidator
        ));
        return decoder;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> allowedOrigins = corsProperties.getAllowedOriginsList();
        if (!allowedOrigins.isEmpty()) {
            configuration.setAllowedOrigins(allowedOrigins);
        }
        configuration.setAllowedMethods(List.of(
                HttpMethod.GET.name(),
                HttpMethod.POST.name(),
                HttpMethod.PUT.name(),
                HttpMethod.PATCH.name(),
                HttpMethod.DELETE.name(),
                HttpMethod.OPTIONS.name()
        ));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setExposedHeaders(List.of("Content-Disposition"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private SecretKey secretKey() {
        byte[] secretBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        return new SecretKeySpec(secretBytes, "HmacSHA256");
    }

    private static void validateJwtSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET is missing. Set security.jwt.secret / JWT_SECRET to a value of at least "
                            + MIN_SECRET_BYTES
                            + " bytes for HS256."
            );
        }
        int byteLength = secret.getBytes(StandardCharsets.UTF_8).length;
        if (byteLength < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET is too short (" + byteLength + " bytes). HS256 requires at least "
                            + MIN_SECRET_BYTES
                            + " bytes."
            );
        }
    }
}
