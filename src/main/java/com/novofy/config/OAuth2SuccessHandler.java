package com.novofy.config;

import com.novofy.jwt.JwtUtil;
import com.novofy.model.User;
import com.novofy.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.util.Optional;

@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    private final boolean secure = true; // true in production (HTTPS)

    public OAuth2SuccessHandler(UserRepository userRepository, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        String registrationId = null;
        if (authentication instanceof OAuth2AuthenticationToken) {
            registrationId = ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId();
        }

        Optional<User> existingUser = userRepository.findByEmail(email);
        if (existingUser.isEmpty()) {
            User newUser = User.builder()
                    .email(email)
                    .username(name)
                    .provider(registrationId)
                    .role("USER")
                    .createdAt(LocalDate.now())
                    .profilePictureUrl("https://www.shutterstock.com/image-vector/blank-avatar-photo-place-holder-600nw-1095249842.jpg")
                    .build();
            userRepository.save(newUser);
        }

        String jwtToken = jwtUtil.generateToken(email, "USER");

        // Set JWT as HTTP-only, Secure cookie
        Cookie tokenCookie = new Cookie("token", jwtToken);
        tokenCookie.setHttpOnly(true);
        tokenCookie.setSecure(secure);
        tokenCookie.setPath("/");
        tokenCookie.setMaxAge(7 * 24 * 60 * 60); // 7 days

        // Set username cookie (encode value)
        Cookie usernameCookie = new Cookie("username", URLEncoder.encode(name, "UTF-8"));
        usernameCookie.setHttpOnly(false);
        usernameCookie.setSecure(secure);
        usernameCookie.setPath("/");
        usernameCookie.setMaxAge(7 * 24 * 60 * 60);

        // Set email cookie (encode value)
        Cookie emailCookie = new Cookie("email", URLEncoder.encode(email, "UTF-8"));
        emailCookie.setHttpOnly(false);
        emailCookie.setSecure(secure);
        emailCookie.setPath("/");
        emailCookie.setMaxAge(7 * 24 * 60 * 60);

        response.addCookie(tokenCookie);
        response.addCookie(usernameCookie);
        response.addCookie(emailCookie);

        // Redirect to frontend home
        response.sendRedirect("http://localhost:5173/");
    }
}