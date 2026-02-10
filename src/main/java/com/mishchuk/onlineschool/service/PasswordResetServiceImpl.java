package com.mishchuk.onlineschool.service;

import com.mishchuk.onlineschool.repository.PasswordResetTokenRepository;
import com.mishchuk.onlineschool.repository.PersonRepository;
import com.mishchuk.onlineschool.repository.entity.PasswordResetTokenEntity;
import com.mishchuk.onlineschool.repository.entity.PersonEntity;
import com.mishchuk.onlineschool.service.email.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetServiceImpl implements PasswordResetService {

    private final PersonRepository personRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Override
    @Transactional
    public void initiatePasswordReset(String email) {
        log.info("Initiating password reset for email: {}", email);
        PersonEntity user = personRepository.findByEmail(email)
                .orElse(null);

        if (user == null) {
            log.warn("Password reset requested for non-existent email: {}", email);
            // We don't want to reveal if the email exists or not
            return;
        }

        // Create token
        String token = UUID.randomUUID().toString();
        PasswordResetTokenEntity tokenEntity = new PasswordResetTokenEntity(
                user,
                token,
                OffsetDateTime.now().plusMinutes(15) // 15 minutes expiry
        );
        tokenRepository.save(tokenEntity);

        // Send email
        String resetLink = frontendUrl + "/auth/reset-password?token=" + token;
        emailService.sendPasswordResetEmail(user.getEmail(), user.getFirstName(), resetLink);
        log.info("Password reset email sent to: {}", email);
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
        log.info("Attempting to reset password with token: {}", token);
        PasswordResetTokenEntity tokenEntity = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired password reset token"));

        if (tokenEntity.isUsed()) {
            throw new IllegalArgumentException("Token has already been used");
        }

        if (tokenEntity.getExpiryDate().isBefore(OffsetDateTime.now())) {
            throw new IllegalArgumentException("Token has expired");
        }

        if (tokenEntity.getExpiryDate().isBefore(OffsetDateTime.now())) {
            throw new IllegalArgumentException("Token has expired");
        }

        // Fetch user explicitly to ensure we have the full entity and avoid proxy
        // issues
        UUID userId = tokenEntity.getUser().getId();
        PersonEntity user = personRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User associated with token not found"));

        String encodedPassword = passwordEncoder.encode(newPassword);
        user.setPassword(encodedPassword);
        personRepository.saveAndFlush(user);

        tokenEntity.setUsed(true);
        tokenRepository.save(tokenEntity);

        log.info("Password successfully reset for user: {}. Encoded password length: {}", user.getEmail(),
                encodedPassword.length());
    }
}
