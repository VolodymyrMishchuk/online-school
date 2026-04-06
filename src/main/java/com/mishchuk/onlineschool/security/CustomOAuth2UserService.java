package com.mishchuk.onlineschool.security;

import com.mishchuk.onlineschool.repository.PersonRepository;
import com.mishchuk.onlineschool.repository.entity.*;
import com.mishchuk.onlineschool.service.EmailService;
import com.mishchuk.onlineschool.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final PersonRepository personRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        try {
            return processOAuth2User(userRequest, oAuth2User);
        } catch (Exception ex) {
            log.error("Error processing OAuth2 user", ex);
            throw new OAuth2AuthenticationException(ex.getMessage());
        }
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oAuth2User) {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        
        String email = oAuth2User.getAttribute("email");
        if (email == null) {
            throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
        }

        String langToSet = extractLanguage(oAuth2User);

        Optional<PersonEntity> personOptional = personRepository.findByEmail(email);
        PersonEntity person;
        if (personOptional.isPresent()) {
            person = personOptional.get();
            boolean changed = false;
            if (person.getProvider() == null || person.getProvider() == AuthProvider.LOCAL) {
                person.setProvider(getAuthProvider(registrationId));
                person.setProviderId(oAuth2User.getName()); // subject/id
                changed = true;
            }
            if (person.getAvatarUrl() == null && oAuth2User.getAttribute("picture") != null) {
                person.setAvatarUrl(oAuth2User.getAttribute("picture"));
                changed = true;
            }
            if (person.getLanguage() == null && langToSet != null) {
                person.setLanguage(langToSet);
                changed = true;
            }
            if (changed) {
                personRepository.save(person);
            }
        } else {
            person = registerNewOAuth2User(registrationId, oAuth2User, langToSet);
        }

        return oAuth2User; // We can just return the default, SuccessHandler will use the email
    }

    private PersonEntity registerNewOAuth2User(String registrationId, OAuth2User oAuth2User, String langToSet) {
        PersonEntity person = new PersonEntity();
        person.setProvider(getAuthProvider(registrationId));
        person.setProviderId(oAuth2User.getName());
        person.setEmail(oAuth2User.getAttribute("email"));
        
        String firstName = oAuth2User.getAttribute("given_name");
        String lastName = oAuth2User.getAttribute("family_name");
        
        // Fallback for name representation
        if (firstName == null) {
            String name = oAuth2User.getAttribute("name");
            if (name != null) {
                String[] parts = name.split(" ");
                firstName = parts[0];
                lastName = parts.length > 1 ? parts[1] : "";
            } else {
                firstName = "User";
                lastName = "OAuth2";
            }
        }
        
        person.setFirstName(firstName);
        person.setLastName(lastName);
        person.setRole(PersonRole.USER);
        person.setStatus(PersonStatus.ACTIVE);
        person.setPassword(UUID.randomUUID().toString()); // set some random password
        
        person.setLanguage(langToSet);
        person.setAvatarUrl(oAuth2User.getAttribute("picture"));

        PersonEntity savedPerson = personRepository.save(person);

        try {
            emailService.sendWelcomeEmail(savedPerson.getEmail(), savedPerson.getFirstName());
        } catch (Exception e) {
            log.error("Failed to send welcome email to {}", savedPerson.getEmail(), e);
        }

        try {
            notificationService.broadcastToAdmins(
                    "Новий користувач (OAuth2)",
                    "Зареєстровано нового користувача через "
                            + registrationId
                            + ": "
                            + savedPerson.getFirstName()
                            + " "
                            + savedPerson.getLastName()
                            + " (" + savedPerson.getEmail()
                            + ")",
                    NotificationType.NEW_USER_REGISTRATION);
        } catch (Exception e) {
            log.error("Failed to notify admins about new user {}", savedPerson.getEmail(), e);
        }

        return savedPerson;
    }

    private AuthProvider getAuthProvider(String registrationId) {
        if ("google".equalsIgnoreCase(registrationId)) {
            return AuthProvider.GOOGLE;
        } else if ("apple".equalsIgnoreCase(registrationId)) {
            return AuthProvider.APPLE;
        }
        return AuthProvider.LOCAL;
    }

    private String extractLanguage(OAuth2User oAuth2User) {
        String langToSet = null;
        try {
            ServletRequestAttributes attr =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attr != null && attr.getRequest().getCookies() != null) {
                for (jakarta.servlet.http.Cookie c : attr.getRequest().getCookies()) {
                    if ("frontend_lang".equals(c.getName())) {
                        langToSet = c.getValue();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not read frontend_lang cookie: {}", e.getMessage());
        }

        if (langToSet == null) {
            String locale = oAuth2User.getAttribute("locale");
            langToSet = "en"; // default fallback
            if (locale != null) {
                String localeLower = locale.toLowerCase();
                if (localeLower.startsWith("uk") || localeLower.startsWith("ru")) {
                    langToSet = "uk";
                } else if (localeLower.startsWith("de")) {
                    langToSet = "de";
                }
            }
        }
        return langToSet;
    }
}
