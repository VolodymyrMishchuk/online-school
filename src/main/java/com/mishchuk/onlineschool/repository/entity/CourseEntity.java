package com.mishchuk.onlineschool.repository.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Data
@Entity
@Table(name = "courses")
public class CourseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private java.util.UUID id;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "modules_number")
    private Integer modulesNumber;

    @Column(name = "access_duration")
    private Integer accessDuration;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private CourseStatus status = CourseStatus.DRAFT;

    @Column(name = "version")
    private String version = "1.0";

    @Column(name = "price")
    private java.math.BigDecimal price;

    @Column(name = "discount_amount")
    private java.math.BigDecimal discountAmount;

    @Column(name = "discount_percentage")
    private Integer discountPercentage;

    @Column(name = "promotional_discount_percentage")
    private Integer promotionalDiscountPercentage;

    @Column(name = "promotional_discount_amount")
    private java.math.BigDecimal promotionalDiscountAmount;

    @OneToOne(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private CourseCoverEntity coverImage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "next_course_id")
    private CourseEntity nextCourse;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<ModuleEntity> modules;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private PersonEntity createdBy;

    @CreationTimestamp
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
