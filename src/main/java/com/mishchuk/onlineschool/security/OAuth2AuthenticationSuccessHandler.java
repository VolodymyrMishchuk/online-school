package com.mishchuk.onlineschool.security;

import com.mishchuk.onlineschool.repository.entity.PersonEntity;
import com.mishchuk.onlineschool.repository.entity.RefreshTokenEntity;
import com.mishchuk.onlineschool.service.RefreshTokenService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtils jwtUtils;
    private final CustomUserDetailsService userDetailsService;
    private final RefreshTokenService refreshTokenService;

    @Value("${spring.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");

        if (email == null) {
            getRedirectStrategy().sendRedirect(request, response, frontendUrl + "/login?error=email_not_found");
            return;
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        String accessToken = jwtUtils.generateToken(userDetails);
        
        PersonEntity person = userDetailsService.getPerson(email);
        RefreshTokenEntity refreshToken = refreshTokenService.createRefreshToken(person.getId());

        // Set the refresh token as HttpOnly cookie
        Cookie cookie = new Cookie("refreshToken", refreshToken.getToken());
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // Should be true in production
        cookie.setPath("/");
        cookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);

        // Redirect to frontend with the access token and user info
        String targetUrl = frontendUrl + "/dashboard?token=" + accessToken 
            + "&userId=" + person.getId()
            + "&role=" + (person.getRole() != null ? person.getRole().name() : "USER")
            + "&firstName=" + java.net.URLEncoder.encode(person.getFirstName() != null ? person.getFirstName() : "", "UTF-8")
            + "&lastName=" + java.net.URLEncoder.encode(person.getLastName() != null ? person.getLastName() : "", "UTF-8")
            + "&email=" + java.net.URLEncoder.encode(email, "UTF-8")
            + (person.getAvatarUrl() != null ? "&avatarUrl=" + java.net.URLEncoder.encode(person.getAvatarUrl(), "UTF-8") : "");
        
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
