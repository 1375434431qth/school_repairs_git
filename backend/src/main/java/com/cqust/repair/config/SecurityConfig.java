package com.cqust.repair.config;

import com.cqust.repair.common.result.R;
import com.cqust.repair.common.result.ResultCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Spring Security 配置：JWT 无状态认证 + 角色方法级鉴权。
 *
 * @author school-repair
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // SSE 等异步请求完成时会再次派发，原始请求已鉴权，这里放行 ASYNC/ERROR 派发
                        .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()
                        // 跨域预检必须放行，否则前端 401
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // 图片预览免登录（img 标签无法带 Header）
                        .requestMatchers(HttpMethod.GET, "/api/v1/files/**").permitAll()
                        .requestMatchers(
                                "/api/v1/auth/login",
                                // refresh 可能发生在 access 过期后，必须免登录，由 refreshToken 自身校验
                                "/api/v1/auth/refresh",
                                "/webjars/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/actuator/health",
                                "/ws/**",
                                "/error"
                        ).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(handler -> handler
                        .authenticationEntryPoint(this::writeUnauthorized)
                        .accessDeniedHandler(this::writeForbidden))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable);
        return http.build();
    }

    /**
     * 密码加密器：BCrypt，strength=10。
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    /**
     * 未登录 / 凭证失效 → 401。
     */
    private void writeUnauthorized(HttpServletRequest request, HttpServletResponse response,
                                   org.springframework.security.core.AuthenticationException exception) throws IOException {
        writeJson(response, ResultCode.UNAUTHORIZED);
    }

    /**
     * 已登录但权限不足 → 403。
     */
    private void writeForbidden(HttpServletRequest request, HttpServletResponse response,
                                org.springframework.security.access.AccessDeniedException exception) throws IOException {
        writeJson(response, ResultCode.FORBIDDEN);
    }

    private void writeJson(HttpServletResponse response, ResultCode resultCode) throws IOException {
        response.setStatus(resultCode.getHttpStatus());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), R.error(resultCode));
    }
}
