package com.novofy.config;

import com.novofy.jwt.JwtUtil;
import com.novofy.model.User;
import com.novofy.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

  private final UserRepository userRepository;
  private final JwtUtil jwtUtil;

  public OAuth2SuccessHandler(UserRepository userRepository, JwtUtil jwtUtil) {
    this.userRepository = userRepository;
    this.jwtUtil = jwtUtil;
  }

  @Override
  public void onAuthenticationSuccess(HttpServletRequest request,
                                      HttpServletResponse response,
                                      Authentication authentication) throws IOException, ServletException {

    OAuth2User oUser = (OAuth2User) authentication.getPrincipal();
    Map<String, Object> attrs = oUser.getAttributes();

    String email = str(attrs.get("email"));
    String name = firstNonEmpty(
        str(attrs.get("name")),
        str(attrs.get("given_name")),
        str(attrs.get("preferred_username"))
    );

    String provider = null;
    if (authentication instanceof OAuth2AuthenticationToken t) {
      provider = t.getAuthorizedClientRegistrationId();
    }

    if (email != null) {
      Optional<User> existing = userRepository.findByEmail(email);
      if (existing.isEmpty()) {
        User u = User.builder()
            .email(email)
            .username(name != null ? name : email)
            .provider(provider)
            .role("USER")
            .createdAt(LocalDate.now())
            .profilePictureUrl("https://www.shutterstock.com/image-vector/blank-avatar-photo-place-holder-600nw-1095249842.jpg")
            .build();
        userRepository.save(u);
      }
    }

    // Issue JWT and redirect to SPA callback
    String jwtToken = jwtUtil.generateToken(email, "USER");
    String frontend = "https://nafaverse-uc38.vercel.app/auth/callback";
    String target = frontend
        + "?token=" + enc(jwtToken)
        + (email != null ? "&email=" + enc(email) : "")
        + (name != null ? "&name=" + enc(name) : "");
    response.sendRedirect(target);
  }

  private static String str(Object o) { return o == null ? null : String.valueOf(o); }
  private static String firstNonEmpty(String... vals) {
    for (String v : vals) if (v != null && !v.isBlank()) return v;
    return null;
  }
  private static String enc(String v) { return URLEncoder.encode(v, StandardCharsets.UTF_8); }
}