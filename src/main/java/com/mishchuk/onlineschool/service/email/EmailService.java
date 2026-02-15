package com.mishchuk.onlineschool.service.email;

import java.time.LocalDate;

public interface EmailService {
    void sendWelcomeEmail(String to, String userName);

    void sendCourseAccessGrantedEmail(String to, String userName, String courseName);

    void sendCourseExpirationReminderEmail(String to, String userName, String courseName, LocalDate expirationDate);

    void sendPasswordResetEmail(String to, String userName, String resetLink);

    void sendAccessExtendedEmail(String to, String userName, String courseName, LocalDate expirationDate);

    void sendCourseAccessRevokedEmail(String to, String userName, String courseName);
}
