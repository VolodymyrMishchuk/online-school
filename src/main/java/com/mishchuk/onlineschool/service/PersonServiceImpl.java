package com.mishchuk.onlineschool.service;

import com.mishchuk.onlineschool.controller.dto.PersonCreateDto;
import com.mishchuk.onlineschool.controller.dto.PersonDto;
import com.mishchuk.onlineschool.controller.dto.PersonUpdateDto;
import com.mishchuk.onlineschool.exception.EmailAlreadyExistsException;
import com.mishchuk.onlineschool.exception.ResourceNotFoundException;
import com.mishchuk.onlineschool.mapper.PersonMapper;
import com.mishchuk.onlineschool.repository.CourseRepository;
import com.mishchuk.onlineschool.repository.EnrollmentRepository;
import com.mishchuk.onlineschool.repository.PersonRepository;
import com.mishchuk.onlineschool.repository.entity.CourseEntity;
import com.mishchuk.onlineschool.repository.entity.EnrollmentEntity;
import com.mishchuk.onlineschool.repository.entity.PersonEntity;
import com.mishchuk.onlineschool.repository.entity.PersonStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PersonServiceImpl implements PersonService {

    private final PersonRepository personRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final PersonMapper personMapper;
    private final PasswordEncoder passwordEncoder;
    private final com.mishchuk.onlineschool.service.email.EmailService emailService;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public void createPerson(PersonCreateDto dto) {
        log.info("Attempting to register new user with email: {}", dto.email());

        if (personRepository.findByEmail(dto.email()).isPresent()) {
            log.warn("Registration failed: email {} already exists", dto.email());
            throw new EmailAlreadyExistsException("Email already exists: " + dto.email());
        }

        PersonEntity entity = personMapper.toEntity(dto);
        entity.setPassword(passwordEncoder.encode(entity.getPassword()));
        personRepository.save(entity);

        log.info("Successfully registered user with email: {} and ID: {}", dto.email(), entity.getId());

        try {
            emailService.sendWelcomeEmail(entity.getEmail(), entity.getFirstName());
        } catch (Exception e) {
            log.error("Failed to send welcome email to {}", entity.getEmail(), e);
        }

        // Notify admins about new user
        try {
            notificationService.broadcastToAdmins(
                    "Новий користувач",
                    "Зареєстровано нового користувача: " + entity.getFirstName() + " " + entity.getLastName() + " ("
                            + entity.getEmail() + ")",
                    com.mishchuk.onlineschool.repository.entity.NotificationType.NEW_USER_REGISTRATION);
        } catch (Exception e) {
            log.error("Failed to notify admins about new user {}", entity.getEmail(), e);
        }

        if (dto.courseIds() != null && !dto.courseIds().isEmpty()) {
            for (UUID courseId : dto.courseIds()) {
                try {
                    addCourseAccess(entity.getId(), courseId);
                } catch (Exception e) {
                    log.error("Failed to enroll user {} to course {}", entity.getEmail(), courseId, e);
                }
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PersonDto> getPerson(UUID id) {
        return personRepository.findById(id)
                .map(personMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PersonDto> getAllPersons() {
        return personRepository.findAll().stream()
                .map(personMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public void updatePerson(UUID id, PersonUpdateDto dto) {
        PersonEntity entity = personRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Person not found with id: " + id));
        personMapper.updateEntityFromDto(dto, entity);
        personRepository.save(entity);
    }

    @Override
    @Transactional
    public void deletePerson(UUID id) {
        personRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<com.mishchuk.onlineschool.controller.dto.PersonWithEnrollmentsDto> getAllPersonsWithEnrollments() {
        return personRepository.findAll().stream()
                .map(personMapper::toDtoWithEnrollments)
                .toList();
    }

    @Override
    @Transactional
    public void updatePersonStatus(UUID id, String status) {
        PersonEntity person = personRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Person not found with id: " + id));
        try {
            person.setStatus(PersonStatus.valueOf(status));
            personRepository.save(person);

            // Notify User
            String statusMessage = PersonStatus.BLOCKED.name().equals(status)
                    ? "Ваш обліковий запис було заблоковано адміністратором. Зверніться до підтримки для деталей."
                    : "Ваш обліковий запис активовано. Ви можете користуватися всіма функціями платформи.";

            try {
                notificationService.createNotification(
                        person.getId(),
                        "Зміна статусу облікового запису",
                        statusMessage,
                        com.mishchuk.onlineschool.repository.entity.NotificationType.SYSTEM);

                // Notify Admins
                notificationService.broadcastToAdmins(
                        "Зміна статусу користувача",
                        "Статус користувача " + person.getFirstName() + " " + person.getLastName() + " змінено на "
                                + status,
                        com.mishchuk.onlineschool.repository.entity.NotificationType.SYSTEM);
            } catch (Exception e) {
                log.error("Failed to send notifications for status change of user {}", person.getEmail(), e);
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status: " + status);
        }
    }

    @Override
    @Transactional
    public void addCourseAccess(UUID personId, UUID courseId) {
        PersonEntity person = personRepository.findById(personId)
                .orElseThrow(() -> new ResourceNotFoundException("Person not found with id: " + personId));
        CourseEntity course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));

        if (enrollmentRepository.findByStudentIdAndCourseId(personId, courseId).isPresent()) {
            throw new IllegalArgumentException("User already enrolled in this course");
        }

        EnrollmentEntity enrollment = new EnrollmentEntity();
        enrollment.setStudent(person);
        enrollment.setCourse(course);
        enrollment.setStatus("ACTIVE");
        enrollmentRepository.save(enrollment);

        try {
            emailService.sendCourseAccessGrantedEmail(person.getEmail(), person.getFirstName(), course.getName());
        } catch (Exception e) {
            log.error("Failed to send course enrollment email to {}", person.getEmail(), e);
        }

        // Create in-app notification
        try {
            notificationService.createNotification(
                    person.getId(),
                    "Доступ до курсу відкрито",
                    "Вам надано доступ до курсу \"" + course.getName() + "\". Успішного навчання!",
                    com.mishchuk.onlineschool.repository.entity.NotificationType.COURSE_PURCHASED,
                    "/dashboard/my-courses");

            // Notify admins
            notificationService.broadcastToAdmins(
                    "Нове зарахування на курс",
                    "Користувач " + person.getFirstName() + " " + person.getLastName() + " отримав доступ до курсу \""
                            + course.getName() + "\"",
                    com.mishchuk.onlineschool.repository.entity.NotificationType.SYSTEM);
        } catch (Exception e) {
            log.error("Failed to create notifications for enrollment of {} to {}", person.getEmail(), course.getId(),
                    e);
        }
    }

    @Override
    @Transactional
    public void removeCourseAccess(UUID personId, UUID courseId) {
        EnrollmentEntity enrollment = enrollmentRepository
                .findByStudentIdAndCourseId(personId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));

        PersonEntity student = enrollment.getStudent();
        CourseEntity course = enrollment.getCourse();

        enrollmentRepository.delete(enrollment);

        // Send Email
        try {
            emailService.sendCourseAccessRevokedEmail(student.getEmail(), student.getFirstName(), course.getName());
        } catch (Exception e) {
            log.error("Failed to send access revocation email to {}", student.getEmail(), e);
        }

        // Notify User
        try {
            notificationService.createNotification(
                    student.getId(),
                    "Доступ до курсу скасовано",
                    "Ваш доступ до курсу \"" + course.getName() + "\" було скасовано адміністратором.",
                    com.mishchuk.onlineschool.repository.entity.NotificationType.SYSTEM);

            // Notify Admins
            notificationService.broadcastToAdmins(
                    "Доступ до курсу скасовано",
                    "Адміністратор скасував доступ користувача " + student.getFirstName() + " " + student.getLastName()
                            + " (" + student.getEmail() + ") до курсу \"" + course.getName() + "\"",
                    com.mishchuk.onlineschool.repository.entity.NotificationType.SYSTEM);
        } catch (Exception e) {
            log.error("Failed to send notifications for access revocation of user {} to course {}", student.getEmail(),
                    course.getId(), e);
        }
    }

    @Override
    @Transactional
    public void changePassword(UUID personId, String oldPassword, String newPassword) {
        PersonEntity person = personRepository.findById(personId)
                .orElseThrow(() -> new ResourceNotFoundException("Person not found with id: " + personId));

        if (!passwordEncoder.matches(oldPassword, person.getPassword())) {
            throw new IllegalArgumentException("Invalid old password");
        }

        person.setPassword(passwordEncoder.encode(newPassword));
        personRepository.save(person);
    }
}
