package com.sist.rms.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * A SINGLE SHARED PASSWORD FOR THE WHOLE SITE.
 *
 * Why this exists
 * ---------------
 * The REST API has no authentication of its own: anyone who can reach it
 * may call DELETE /api/projects/1 and wipe the data. That is acceptable on
 * localhost, where only you can reach it. It is NOT acceptable once the
 * site is on the public internet.
 *
 * So before hosting the site, set two environment variables:
 *
 *     ACCESS_USERNAME=viva
 *     ACCESS_PASSWORD=some-long-password
 *
 * The browser then asks for that username and password once, and remembers
 * it for the visit. You give the pair to your examiner. Every page and
 * every API call is covered.
 *
 * When the two variables are empty - which is the default, and what happens
 * on your own machine - this filter does nothing at all and the site
 * behaves exactly as before.
 *
 * How it works (easy to explain in a viva)
 * ----------------------------------------
 * This is HTTP Basic authentication, which is built into every browser.
 * The browser sends a header:
 *
 *     Authorization: Basic dml2YTpzZWNyZXQ=
 *
 * where the part after "Basic " is "username:password" encoded with Base64.
 * We decode it and compare. If it does not match we reply 401 with a
 * WWW-Authenticate header, which is what makes the browser show its
 * login box.
 *
 * Note for the viva: Base64 is encoding, not encryption. HTTP Basic is only
 * safe over HTTPS, which the hosting services in docs/deployment.md all
 * provide automatically. This is a gate to keep strangers out of a student
 * demo, not a real user-account system.
 */
@Configuration
public class AccessGateFilter {

    @Bean
    public FilterRegistrationBean<Filter> accessGate(
            @Value("${app.access.username:}") String username,
            @Value("${app.access.password:}") String password) {

        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new BasicAuthFilter(username, password));
        registration.addUrlPatterns("/*");
        registration.setOrder(1);
        return registration;
    }

    /** The filter itself. */
    static class BasicAuthFilter implements Filter {

        private final String username;
        private final String password;

        BasicAuthFilter(String username, String password) {
            this.username = username == null ? "" : username.trim();
            this.password = password == null ? "" : password.trim();
        }

        /** The gate is only on when BOTH values have been set. */
        private boolean isEnabled() {
            return !username.isEmpty() && !password.isEmpty();
        }

        @Override
        public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
                throws IOException, ServletException {

            if (!isEnabled()) {
                chain.doFilter(servletRequest, servletResponse);   // local use: do nothing
                return;
            }

            HttpServletRequest request = (HttpServletRequest) servletRequest;
            HttpServletResponse response = (HttpServletResponse) servletResponse;

            if (matches(request.getHeader("Authorization"))) {
                chain.doFilter(servletRequest, servletResponse);
                return;
            }

            // tell the browser to show its username / password box
            response.setHeader("WWW-Authenticate", "Basic realm=\"Project Resource Allocation System\"");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"status\":401,\"error\":\"Unauthorized\","
                  + "\"messages\":[\"This demonstration site is password protected.\"]}");
        }

        /** Decodes "Basic dXNlcjpwYXNz" and compares it with the expected pair. */
        private boolean matches(String authorizationHeader) {
            if (authorizationHeader == null || !authorizationHeader.startsWith("Basic ")) {
                return false;
            }
            String decoded;
            try {
                byte[] bytes = Base64.getDecoder().decode(authorizationHeader.substring(6).trim());
                decoded = new String(bytes, StandardCharsets.UTF_8);
            } catch (IllegalArgumentException ex) {
                return false;      // not valid Base64
            }

            int separator = decoded.indexOf(':');
            if (separator < 0) {
                return false;
            }
            String givenUser = decoded.substring(0, separator);
            String givenPassword = decoded.substring(separator + 1);

            // constant-time comparison, so the reply time cannot leak the password
            return java.security.MessageDigest.isEqual(
                        givenUser.getBytes(StandardCharsets.UTF_8),
                        username.getBytes(StandardCharsets.UTF_8))
                 & java.security.MessageDigest.isEqual(
                        givenPassword.getBytes(StandardCharsets.UTF_8),
                        password.getBytes(StandardCharsets.UTF_8));
        }
    }
}
