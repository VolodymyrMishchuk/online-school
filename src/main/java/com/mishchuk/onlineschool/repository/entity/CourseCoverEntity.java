package com.mishchuk.onlineschool.repository.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Data
@Entity
@Table(name = "course_covers")
@NoArgsConstructor
public class CourseCoverEntity {

    @Id
    @Column(name = "course_id")
    private UUID id;

    @JdbcTypeCode(java.sql.Types.BINARY)
    @Column(name = "image_data")
    private byte[] imageData;

    @Column(name = "average_color")
    private String averageColor;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "course_id")
    private CourseEntity course;
}
