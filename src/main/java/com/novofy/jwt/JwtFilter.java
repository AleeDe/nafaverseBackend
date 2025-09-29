package com.novofy.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.novofy.service.UserService;

import java.io.IOException;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtFilter.class);

    @Autowired
    private JwtUtil jwtUtil;

    @Lazy
    @Autowired
    private UserService userService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        logger.debug("Incoming request -> {} {}", request.getMethod(), request.getRequestURI());

        String token = null;
        String header = request.getHeader("Authorization");
        logger.debug("Authorization header: {}", header);

        if (header != null && header.startsWith("Bearer ")) {
            token = header.substring(7);
            logger.debug("Token extracted from Authorization header (len={}): {}", token.length(), token);
        }

        // Fallback to cookies (useful for browser or Postman cookie header)
        if (token == null) {
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie c : cookies) {
                    logger.debug("Cookie present: {}={}", c.getName(), c.getValue());
                    if ("token".equals(c.getName())) {
                        token = c.getValue();
                        logger.debug("Token extracted from cookie (len={}): {}", token.length(), token);
                        break;
                    }
                }
            } else {
                logger.debug("No cookies present on request");
            }
        }

        if (token != null) {
            try {
                boolean valid = jwtUtil.isValid(token);
                logger.debug("jwtUtil.isValid => {}", valid);
                if (valid) {
                    String email = jwtUtil.extractEmail(token);
                    logger.debug("Email from token => {}", email);
                    if (email != null) {
                        UserDetails user = userService.loadUserByUsername(email);
                        UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                        logger.debug("SecurityContext updated with user: {}", email);
                    } else {
                        logger.warn("Token valid but email extraction returned null");
                    }
                } else {
                    logger.debug("Token is invalid or expired");
                }
            } catch (Exception e) {
                logger.warn("Error while validating token: {}", e.getMessage(), e);
            }
        } else {
            logger.debug("No token found in header or cookies");
        }

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    @Override
protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    // Do NOT skip /api/auth/me
    return path.startsWith("/api/auth/login")
        || path.startsWith("/api/auth/signup")
        || path.startsWith("/api/auth/google/login")
        || path.startsWith("/api/password")
        || path.startsWith("/test")
        || path.startsWith("/oauth2")
        || path.startsWith("/login/oauth2/");
}

   
}
