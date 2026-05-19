package com.mishchuk.onlineschool.service;

import com.mishchuk.onlineschool.controller.dto.EnrollmentCreateDto;
import com.mishchuk.onlineschool.dto.PaymentRequestDto;
import com.mishchuk.onlineschool.dto.PaymentResponseDto;
import com.mishchuk.onlineschool.exception.ResourceNotFoundException;
import com.mishchuk.onlineschool.mapper.PaymentMapper;
import com.mishchuk.onlineschool.repository.CourseRepository;
import com.mishchuk.onlineschool.repository.EnrollmentRepository;
import com.mishchuk.onlineschool.repository.PaymentRepository;
import com.mishchuk.onlineschool.repository.PersonRepository;
import com.mishchuk.onlineschool.repository.entity.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PersonRepository personRepository;
    private final CourseRepository courseRepository;
    private final PaymentMapper paymentMapper;
    private final EmailService emailService;
    private final PdfService pdfService;
    private final NotificationService notificationService;
    private final EnrollmentService enrollmentService;
    private final PromoCodeService promoCodeService;
    private final EnrollmentRepository enrollmentRepository;

    @Override
    @Transactional
    public PaymentResponseDto processPayment(PaymentRequestDto request, String currentUsername) {
        log.info("Processing payment for user: {}, courseId: {}", currentUsername, request.getCourseId());

        PersonEntity person = personRepository.findByEmail(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        CourseEntity course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

        // Calculate amount (apply promo code if provided)
        BigDecimal finalAmount = course.getPrice();
        if (finalAmount == null) {
            finalAmount = BigDecimal.ZERO;
        }

        if (request.getPromoCode() != null && !request.getPromoCode().isBlank()) {
            try {
                promoCodeService.usePromoCode(request.getPromoCode(), course.getId(), currentUsername);
                // The usePromoCode will validate and log usage, but doesn't return the new price.
                // Assuming we use promo code to get discount, wait: PromoCodeCheckResponseDto has it.
                var checkResponse = promoCodeService.checkPromoCode(request.getPromoCode(), currentUsername);
                if (checkResponse != null && checkResponse.getDiscounts() != null) {
                    var discountOpt = checkResponse.getDiscounts().stream()
                            .filter(d -> course.getId().equals(d.getCourseId()))
                            .findFirst();
                            
                    if (discountOpt.isPresent()) {
                        var discount = discountOpt.get();
                        if (discount.getDiscountType() == DiscountType.FIXED_AMOUNT) {
                            finalAmount = finalAmount.subtract(discount.getDiscountValue());
                        } else if (discount.getDiscountType() == DiscountType.PERCENTAGE) {
                            BigDecimal discountAmt = finalAmount.multiply(discount.getDiscountValue()).divide(BigDecimal.valueOf(100));
                            finalAmount = finalAmount.subtract(discountAmt);
                        } else if (discount.getDiscountType() == DiscountType.FIXED_PRICE) {
                            finalAmount = discount.getDiscountValue();
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Failed to apply promo code: {}", e.getMessage());
                // We could fail the payment, but for mock let's just proceed or throw
            }
        }
        
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            finalAmount = BigDecimal.ZERO;
        }

        // Create Payment Entity
        PaymentEntity payment = new PaymentEntity();
        payment.setPerson(person);
        payment.setCourse(course);
        payment.setAmount(finalAmount);
        payment.setCurrency("USD"); // Default currency
        payment.setPaymentSystem(request.getPaymentSystem());
        payment.setStatus(PaymentStatus.SUCCESS); // Mocking success
        payment.setCountry(request.getCountry());

        payment = paymentRepository.save(payment);

        // Generate PDF receipt
        byte[] pdfReceipt = pdfService.generateReceiptPdf(payment);

        // Send Email
        emailService.sendPaymentReceiptEmail(person.getEmail(), person.getFirstName(), course.getName(), pdfReceipt);

        // Notify Admin
        notificationService.broadcastToAdmins(
                "Нова оплата",
                String.format("Користувач %s оплатив курс %s на суму %s %s",
                        person.getEmail(), course.getName(), payment.getAmount(), payment.getCurrency()),
                NotificationType.PAYMENT_SUCCESSFUL
        );

        // Notify User
        notificationService.createNotification(
                person.getId(),
                "Оплата пройшла успішно",
                String.format("Ви успішно придбали курс %s. Чек відправлено на вашу пошту", course.getName()),
                NotificationType.PAYMENT_SUCCESSFUL
        );

        // Enroll User to Course
        if (!enrollmentService.isEnrolled(person.getId(), course.getId())) {
            EnrollmentCreateDto enrollmentDto = new EnrollmentCreateDto(person.getId(), course.getId());
            enrollmentService.createEnrollment(enrollmentDto);
        } else {
            log.info("User {} is already enrolled in course {}, skipping enrollment.", person.getEmail(), course.getName());
        }

        return paymentMapper.toDto(payment);
    }

    private Specification<PaymentEntity> buildSearchSpecification(String search, LocalDate startDate, LocalDate endDate) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.trim().isEmpty()) {
                String likePattern = "%" + search.toLowerCase() + "%";
                Join<PaymentEntity, PersonEntity> personJoin = root.join("person", JoinType.LEFT);
                Join<PaymentEntity, CourseEntity> courseJoin = root.join("course", JoinType.LEFT);
                
                predicates.add(cb.or(
                        cb.like(cb.lower(personJoin.get("firstName")), likePattern),
                        cb.like(cb.lower(personJoin.get("lastName")), likePattern),
                        cb.like(cb.lower(personJoin.get("email")), likePattern),
                        cb.like(cb.lower(courseJoin.get("name")), likePattern),
                        cb.like(cb.lower(root.get("country")), likePattern),
                        cb.like(cb.lower(root.get("paymentSystem").as(String.class)), likePattern)
                ));
            }
            
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate.atStartOfDay().atOffset(ZoneOffset.UTC)));
            }
            
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDate.atTime(LocalTime.MAX).atOffset(ZoneOffset.UTC)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @Override
    public Page<PaymentResponseDto> getAllPayments(String search, String sortKey, String sortDir, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt"); // default
        
        if (sortKey != null && !sortKey.trim().isEmpty()) {
            Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
            switch (sortKey) {
                case "personName":
                    sort = Sort.by(direction, "person.firstName").and(Sort.by(direction, "person.lastName"));
                    break;
                case "courseName":
                    sort = Sort.by(direction, "course.name");
                    break;
                case "amount":
                case "paymentSystem":
                case "status":
                case "country":
                case "createdAt":
                    sort = Sort.by(direction, sortKey);
                    break;
                default:
                    break;
            }
        }
        
        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
        Specification<PaymentEntity> spec = buildSearchSpecification(search, startDate, endDate);
        
        return paymentRepository.findAll(spec, sortedPageable).map(paymentMapper::toDto);
    }

    @Override
    public Page<PaymentResponseDto> getMyPayments(Pageable pageable, String currentUsername) {
        PersonEntity person = personRepository.findByEmail(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Page<PaymentEntity> paymentsPage = paymentRepository.findAllByPersonId(person.getId(), pageable);
        return paymentsPage.map(payment -> {
            PaymentResponseDto dto = paymentMapper.toDto(payment);
            if (payment.getCourse() != null && payment.getStatus() == PaymentStatus.SUCCESS) {
                enrollmentRepository
                        .findByStudentIdAndCourseId(person.getId(), payment.getCourse().getId())
                        .ifPresent(enrollment -> dto.setAccessExpiresAt(enrollment.getExpiresAt()));
            }
            return dto;
        });
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] getPaymentReceipt(java.util.UUID paymentId, String currentUsername) {
        PaymentEntity payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + paymentId));

        PersonEntity currentUser = personRepository.findByEmail(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (currentUser.getRole() != PersonRole.ADMIN && !payment.getPerson().getEmail().equals(currentUsername)) {
            throw new org.springframework.security.access.AccessDeniedException("You don't have access to this receipt");
        }

        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new IllegalStateException("Cannot generate receipt for unsuccessful payment");
        }

        return pdfService.generateReceiptPdf(payment);
    }
}
