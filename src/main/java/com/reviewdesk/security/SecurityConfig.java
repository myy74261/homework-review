package com.reviewdesk.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 账号会话、角色路由与 CSRF 防护配置。
 */
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/", "/login", "/register",
                                "/css/**", "/js/**", "/error").permitAll()
                        .requestMatchers("/app/**").authenticated()
                        .requestMatchers("/api/app/teacher/**").hasRole("TEACHER")
                        .requestMatchers("/api/app/student/**").hasRole("STUDENT")
                        .requestMatchers("/api/app/session").authenticated()
                        .requestMatchers("/api/app/account/**").authenticated()
                        .requestMatchers("/teacher/**", "/reviews/**", "/api/**")
                        .hasRole("TEACHER")
                        .requestMatchers("/student/**").hasRole("STUDENT")
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .failureUrl("/login?error")
                        .successHandler((request, response, authentication) -> {
                            boolean teacher = authentication.getAuthorities().stream()
                                    .anyMatch(authority ->
                                            "ROLE_TEACHER".equals(authority.getAuthority()));
                            response.sendRedirect(teacher
                                    ? "/app/teacher" : "/app/student");
                        })
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll())
                .sessionManagement(session -> session
                        .sessionFixation(fixation -> fixation.migrateSession()));
        return http.build();
    }
}

