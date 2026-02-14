package com.mishchuk.onlineschool.service.email;

import java.time.LocalDate;

public interface EmailService {
    void sendWelcomeEmail(String to, String userName);

    void sendCoursePurchaseEmail(String to, String userName, String courseName);

    void sendCourseExpirationReminderEmail(String to, String userName, String courseName, LocalDate expirationDate);

    void sendPasswordResetEmail(String to, String userName, String resetLink);

    void sendAccessExtendedEmail(String to, String userName, String courseName, LocalDate expirationDate);
}
