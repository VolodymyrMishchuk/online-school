package com.mishchuk.onlineschool.repository;

import com.mishchuk.onlineschool.repository.entity.FileEntity;
import com.mishchuk.onlineschool.repository.entity.LessonEntity;
import com.mishchuk.onlineschool.repository.entity.ModuleEntity;
import com.mishchuk.onlineschool.repository.entity.PersonEntity;
import com.mishchuk.onlineschool.repository.entity.CourseEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FileRepositoryTest extends AbstractRepositoryTest {

    @Autowired private FileRepository       fileRepository;
    @Autowired private PersonRepository     personRepository;
    @Autowired private CourseRepository     courseRepository;
    @Autowired private ModuleRepository     moduleRepository;
    @Autowired private LessonRepository     lessonRepository;

    private PersonEntity uploader;
    private LessonEntity lesson;

    @BeforeEach
    void setUp() {
        uploader = personRepository.save(person("uploader@test.com"));

        CourseEntity course = courseRepository.save(course("Java 101"));
        ModuleEntity module = moduleRepository.save(module(course));
        lesson = lessonRepository.save(lesson(module, "Lesson 1"));
    }

    // ─────────────────────── findByRelatedEntityTypeAndRelatedEntityId ───────────────────────

    @Test
    @DisplayName("findByRelatedEntityTypeAndRelatedEntityId — повертає файли за типом і id сутності")
    void findByRelatedEntity_returnsMatchingFiles() {
        UUID entityId = UUID.randomUUID();
        fileRepository.save(file("file1.jpg", "APPEAL", entityId));
        fileRepository.save(file("file2.jpg", "APPEAL", entityId));
        fileRepository.save(file("other.jpg", "APPEAL", UUID.randomUUID())); // інший id

        List<FileEntity> result = fileRepository
                .findByRelatedEntityTypeAndRelatedEntityId("APPEAL", entityId);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(FileEntity::getRelatedEntityId)
                .containsOnly(entityId);
    }

    @Test
    @DisplayName("findByRelatedEntityTypeAndRelatedEntityId — повертає порожній список якщо немає збігів")
    void findByRelatedEntity_noMatch_returnsEmpty() {
        List<FileEntity> result = fileRepository
                .findByRelatedEntityTypeAndRelatedEntityId("APPEAL", UUID.randomUUID());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByRelatedEntityTypeAndRelatedEntityId — тип сутності фільтрує коректно")
    void findByRelatedEntity_wrongType_returnsEmpty() {
        UUID entityId = UUID.randomUUID();
        fileRepository.save(file("img.jpg", "APPEAL", entityId));

        List<FileEntity> result = fileRepository
                .findByRelatedEntityTypeAndRelatedEntityId("COURSE", entityId);

        assertThat(result).isEmpty();
    }

    // ─────────────────────── findByUploadedBy ───────────────────────

    @Test
    @DisplayName("findByUploadedBy — повертає файли завантажені конкретним користувачем")
    void findByUploadedBy_returnsUploaderFiles() {
        PersonEntity other = personRepository.save(person("other@test.com"));

        fileRepository.save(fileByUploader("a.jpg", uploader));
        fileRepository.save(fileByUploader("b.jpg", uploader));
        fileRepository.save(fileByUploader("c.jpg", other));

        List<FileEntity> result = fileRepository.findByUploadedBy(uploader);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(f -> f.getUploadedBy().getId())
                .containsOnly(uploader.getId());
    }

    // ─────────────────────── findByLesson / findByLessonId ───────────────────────

    @Test
    @DisplayName("findByLesson — повертає файли прив'язані до уроку")
    void findByLesson_returnsLessonFiles() {
        LessonEntity other = lessonRepository.save(lesson(lesson.getModule(), "Other Lesson"));

        fileRepository.save(fileForLesson("l1.pdf", lesson));
        fileRepository.save(fileForLesson("l2.pdf", lesson));
        fileRepository.save(fileForLesson("other.pdf", other));

        List<FileEntity> result = fileRepository.findByLesson(lesson);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(f -> f.getLesson().getId())
                .containsOnly(lesson.getId());
    }

    @Test
    @DisplayName("findByLessonId — повертає файли за id уроку")
    void findByLessonId_returnsLessonFiles() {
        fileRepository.save(fileForLesson("x.pdf", lesson));

        List<FileEntity> result = fileRepository.findByLessonId(lesson.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLesson().getId()).isEqualTo(lesson.getId());
    }

    // ─────────────────────── findLessonFilesOrdered (@Query) ───────────────────────

    @Test
    @DisplayName("findLessonFilesOrdered — повертає файли уроку відсортовані за uploadedAt ASC")
    void findLessonFilesOrdered_returnsSortedAscending() {
        FileEntity f1 = fileForLesson("first.pdf", lesson);
        FileEntity f2 = fileForLesson("second.pdf", lesson);
        // Зберігаємо по черзі — @PrePersist встановить uploadedAt
        fileRepository.save(f1);
        // невелика затримка щоб uploadedAt відрізнявся
        fileRepository.save(f2);
        fileRepository.flush();

        List<FileEntity> result = fileRepository.findLessonFilesOrdered(lesson.getId());

        assertThat(result).hasSize(2);
        // перевіряємо що впорядковані ASC
        assertThat(result.get(0).getUploadedAt())
                .isBeforeOrEqualTo(result.get(1).getUploadedAt());
    }

    @Test
    @DisplayName("findLessonFilesOrdered — повертає порожній список для уроку без файлів")
    void findLessonFilesOrdered_noFiles_returnsEmpty() {
        List<FileEntity> result = fileRepository.findLessonFilesOrdered(lesson.getId());
        assertThat(result).isEmpty();
    }

    // ─────────────────────── helpers ───────────────────────

    private PersonEntity person(String email) {
        PersonEntity p = new PersonEntity();
        p.setEmail(email);
        p.setPassword("pass");
        return p;
    }

    private CourseEntity course(String name) {
        CourseEntity c = new CourseEntity();
        c.setName(name);
        return c;
    }

    private ModuleEntity module(CourseEntity course) {
        ModuleEntity m = new ModuleEntity();
        m.setCourse(course);
        m.setName("Module 1");
        return m;
    }

    private LessonEntity lesson(ModuleEntity module, String name) {
        LessonEntity l = new LessonEntity();
        l.setModule(module);
        l.setName(name);
        return l;
    }

    private FileEntity file(String name, String entityType, UUID entityId) {
        FileEntity f = new FileEntity();
        f.setFileName(name);
        f.setOriginalName(name);
        f.setContentType("image/jpeg");
        f.setFileSize(1024L);
        f.setMinioObjectName("minio/" + name);
        f.setBucketName("test-bucket");
        f.setRelatedEntityType(entityType);
        f.setRelatedEntityId(entityId);
        return f;
    }

    private FileEntity fileByUploader(String name, PersonEntity uploader) {
        FileEntity f = file(name, "PERSON", uploader.getId());
        f.setUploadedBy(uploader);
        return f;
    }

    private FileEntity fileForLesson(String name, LessonEntity lesson) {
        FileEntity f = file(name, "LESSON", lesson.getId());
        f.setLesson(lesson);
        return f;
    }
}
