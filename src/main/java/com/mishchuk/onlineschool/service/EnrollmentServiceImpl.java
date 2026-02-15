package com.mishchuk.onlineschool.service;

import com.mishchuk.onlineschool.controller.dto.EnrollmentCreateDto;
import com.mishchuk.onlineschool.controller.dto.EnrollmentDto;
import com.mishchuk.onlineschool.mapper.EnrollmentMapper;
import com.mishchuk.onlineschool.repository.CourseRepository;
import com.mishchuk.onlineschool.repository.EnrollmentRepository;
import com.mishchuk.onlineschool.repository.PersonRepository;
import com.mishchuk.onlineschool.repository.entity.CourseEntity;
import com.mishchuk.onlineschool.repository.entity.EnrollmentEntity;
import com.mishchuk.onlineschool.repository.entity.PersonEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EnrollmentServiceImpl implements EnrollmentService {

        private final EnrollmentRepository enrollmentRepository;
        private final PersonRepository personRepository;
        private final CourseRepository courseRepository;
        private final EnrollmentMapper enrollmentMapper;
        private final com.mishchuk.onlineschool.service.email.EmailService emailService;
        private final NotificationService notificationService;

        @Override
        @Transactional
        public void createEnrollment(EnrollmentCreateDto dto) {
                if (enrollmentRepository.findByStudentIdAndCourseId(dto.studentId(), dto.courseId()).isPresent()) {
                        throw new RuntimeException("Enrollment already exists");
                }

                PersonEntity student = personRepository.findById(dto.studentId())
                                .orElseThrow(() -> new RuntimeException("Student not found"));
                CourseEntity course = courseRepository.findById(dto.courseId())
                                .orElseThrow(() -> new RuntimeException("Course not found"));

                EnrollmentEntity entity = enrollmentMapper.toEntity(dto);
                entity.setStudent(student);
                entity.setCourse(course);
                enrollmentRepository.save(entity);

                // Send access granted email
                emailService.sendCourseAccessGrantedEmail(student.getEmail(),
                                student.getFirstName() + " " + student.getLastName(),
                                course.getName());

                // Notify Admins
                notificationService.broadcastToAdmins(
                                "Нова покупка курсу",
                                "Користувач " + student.getFirstName() + " " + student.getLastName() + " ("
                                                + student.getEmail()
                                                + ") купив курс \"" + course.getName() + "\"",
                                com.mishchuk.onlineschool.repository.entity.NotificationType.COURSE_PURCHASED);

                // Notify Student
                notificationService.createNotification(
                                student.getId(),
                                "Успішна покупка!",
                                "Ви успішно придбали курс \"" + course.getName() + "\". Бажаємо успішного навчання!",
                                com.mishchuk.onlineschool.repository.entity.NotificationType.COURSE_PURCHASED);
        }

        @Override
        public List<EnrollmentDto> getEnrollmentsByStudent(UUID studentId) {
                return enrollmentRepository.findByStudentId(studentId)
                                .stream()
                                .map(enrollmentMapper::toDto)
                                .toList();
        }

        @Override
        public List<EnrollmentDto> getAllEnrollments() {
                return enrollmentRepository.findAll()
                                .stream()
                                .map(enrollmentMapper::toDto)
                                .toList();
        }
}
