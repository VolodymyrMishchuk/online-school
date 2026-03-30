package com.mishchuk.onlineschool.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mishchuk.onlineschool.controller.dto.CourseCreateDto;
import com.mishchuk.onlineschool.controller.dto.CourseDto;
import com.mishchuk.onlineschool.controller.dto.CourseUpdateDto;
import com.mishchuk.onlineschool.exception.GlobalExceptionHandler;
import com.mishchuk.onlineschool.repository.PersonRepository;
import com.mishchuk.onlineschool.repository.entity.CourseStatus;
import com.mishchuk.onlineschool.repository.entity.PersonEntity;
import com.mishchuk.onlineschool.repository.entity.PersonRole;
import com.mishchuk.onlineschool.security.CustomUserDetailsService;
import com.mishchuk.onlineschool.security.JwtUtils;
import com.mishchuk.onlineschool.service.CourseService;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CourseController.class)
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
class CourseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CourseService courseService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private PersonRepository personRepository;

    // СЕКЦІЯ: POST /courses

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "FAKE_ADMIN"})
    @DisplayName("POST /courses — авторизована роль → 201 Created")
    void createCourse_authorizedRole_returns201(String role) throws Exception {
        MockMultipartFile coursePart = new MockMultipartFile(
                "course", "", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(createDto())
        );
        MockMultipartFile imagePart = new MockMultipartFile(
                "image", "image.jpg", MediaType.IMAGE_JPEG_VALUE, "content".getBytes()
        );

        mockMvc.perform(multipart("/courses")
                        .file(coursePart)
                        .file(imagePart)
                        .with(user("u").roles(role)))
                .andExpect(status().isCreated());

        verify(courseService, times(1)).createCourse(any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"USER", "FAKE_USER"})
    @DisplayName("POST /courses — неавторизована роль → 403 Forbidden")
    void createCourse_unauthorizedRole_returns403(String role) throws Exception {
        MockMultipartFile coursePart = new MockMultipartFile(
                "course", "", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(createDto())
        );

        mockMvc.perform(multipart("/courses")
                        .file(coursePart)
                        .with(user("u").roles(role)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(courseService);
    }

    @Test
    @DisplayName("POST /courses — анонімний → 403 Forbidden")
    void createCourse_anonymous_returns403() throws Exception {
        MockMultipartFile coursePart = new MockMultipartFile(
                "course", "", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(createDto())
        );

        mockMvc.perform(multipart("/courses")
                        .file(coursePart))
                .andExpect(status().isForbidden());
    }

    // СЕКЦІЯ: GET /courses/{id}

    @Test
    @DisplayName("GET /courses/{id} — знайдено → 200 OK")
    @WithMockUser
    void getCourse_found_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(courseService.getCourse(eq(id))).thenReturn(Optional.of(courseDto(id, "Course 1")));

        mockMvc.perform(get("/courses/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("Course 1"));
    }

    @Test
    @DisplayName("GET /courses/{id} — не знайдено → 404 Not Found")
    @WithMockUser
    void getCourse_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(courseService.getCourse(eq(id))).thenReturn(Optional.empty());

        mockMvc.perform(get("/courses/{id}", id))
                .andExpect(status().isNotFound());
    }

    // СЕКЦІЯ: GET /courses

    @Test
    @DisplayName("GET /courses — без userId, повертає список курсів → 200 OK")
    void getAllCourses_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(courseService.getAllCourses()).thenReturn(List.of(courseDto(id, "Course 2")));

        mockMvc.perform(get("/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(id.toString()))
                .andExpect(jsonPath("$[0].name").value("Course 2"));
    }

    @Test
    @DisplayName("GET /courses — порожній список → 204 No Content")
    void getAllCourses_empty_returns204() throws Exception {
        when(courseService.getAllCourses()).thenReturn(List.of());

        mockMvc.perform(get("/courses"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /courses?userId={id} — без авторизації → 401 Unauthorized")
    void getAllCoursesByUser_unauthenticated_returns401() throws Exception {
        UUID userId = UUID.randomUUID();
        mockMvc.perform(get("/courses").param("userId", userId.toString()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /courses?userId={id} — автентифікований, запит на чужі дані → 403 Forbidden")
    @WithMockUser(username = "other@test.com")
    void getAllCoursesByUser_otherUser_returns403() throws Exception {
        UUID userId = UUID.randomUUID();
        PersonEntity currentUser = personEntity(UUID.randomUUID(), PersonRole.USER);
        when(personRepository.findByEmail("other@test.com")).thenReturn(Optional.of(currentUser));

        mockMvc.perform(get("/courses").param("userId", userId.toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /courses?userId={id} — автентифікований ADMIN, запит на чужі дані → 200 OK")
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    void getAllCoursesByUser_adminAccess_returns200() throws Exception {
        UUID userId = UUID.randomUUID();
        PersonEntity adminUser = personEntity(UUID.randomUUID(), PersonRole.ADMIN);
        when(personRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
        when(courseService.getAllCoursesWithEnrollment(userId)).thenReturn(List.of(courseDto(userId, "Enrolled")));

        mockMvc.perform(get("/courses").param("userId", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Enrolled"));
    }

    @Test
    @DisplayName("GET /courses?userId={id} — власний запит → 200 OK")
    @WithMockUser(username = "user@test.com")
    void getAllCoursesByUser_ownData_returns200() throws Exception {
        UUID userId = UUID.randomUUID();
        PersonEntity currentUser = personEntity(userId, PersonRole.USER);
        when(personRepository.findByEmail("user@test.com")).thenReturn(Optional.of(currentUser));
        when(courseService.getAllCoursesWithEnrollment(userId)).thenReturn(List.of(courseDto(userId, "OwnCourse")));

        mockMvc.perform(get("/courses").param("userId", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("OwnCourse"));
    }

    // СЕКЦІЯ: PUT /courses/{id}

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "FAKE_ADMIN"})
    @DisplayName("PUT /courses/{id} — авторизована роль → 204 No Content")
    void updateCourse_authorizedRole_returns204(String role) throws Exception {
        UUID id = UUID.randomUUID();
        MockMultipartFile coursePart = new MockMultipartFile(
                "course", "", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(updateDto())
        );

        mockMvc.perform(multipart(HttpMethod.PUT, "/courses/{id}", id)
                        .file(coursePart)
                        .with(user("u").roles(role)))
                .andExpect(status().isNoContent());

        verify(courseService, times(1)).updateCourse(eq(id), any(), any());
    }

    @Test
    @DisplayName("PUT /courses/{id} — курс не знайдено (виняток) → 404 Not Found")
    @WithMockUser(roles = "ADMIN")
    void updateCourse_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        MockMultipartFile coursePart = new MockMultipartFile(
                "course", "", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(updateDto())
        );

        doThrow(new RuntimeException("Not found")).when(courseService).updateCourse(eq(id), any(), any());

        mockMvc.perform(multipart(HttpMethod.PUT, "/courses/{id}", id)
                        .file(coursePart))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"USER", "FAKE_USER"})
    @DisplayName("PUT /courses/{id} — неавторизована роль → 403 Forbidden")
    void updateCourse_unauthorizedRole_returns403(String role) throws Exception {
        MockMultipartFile coursePart = new MockMultipartFile(
                "course", "", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(updateDto())
        );

        mockMvc.perform(multipart(HttpMethod.PUT, "/courses/{id}", UUID.randomUUID())
                        .file(coursePart)
                        .with(user("u").roles(role)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(courseService);
    }

    @Test
    @DisplayName("PUT /courses/{id} — анонімний → 403 Forbidden")
    void updateCourse_anonymous_returns403() throws Exception {
        MockMultipartFile coursePart = new MockMultipartFile(
                "course", "", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(updateDto())
        );

        mockMvc.perform(multipart(HttpMethod.PUT, "/courses/{id}", UUID.randomUUID())
                        .file(coursePart))
                .andExpect(status().isForbidden());
    }

    // СЕКЦІЯ: GET /courses/{id}/cover

    @Test
    @DisplayName("GET /courses/{id}/cover — знайдено → 200 OK")
    @WithMockUser // Can be anonymous or authenticated
    void getCourseCover_found_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(courseService.getCourseCoverImage(eq(id))).thenReturn(Optional.of("image_bytes".getBytes()));

        mockMvc.perform(get("/courses/{id}/cover", id))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                .andExpect(content().bytes("image_bytes".getBytes()));
    }

    @Test
    @DisplayName("GET /courses/{id}/cover — не знайдено → 404 Not Found")
    void getCourseCover_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(courseService.getCourseCoverImage(eq(id))).thenReturn(Optional.empty());

        mockMvc.perform(get("/courses/{id}/cover", id))
                .andExpect(status().isNotFound());
    }

    // СЕКЦІЯ: DELETE /courses/{id}

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "FAKE_ADMIN"})
    @DisplayName("DELETE /courses/{id} — авторизована роль → 204 No Content")
    void deleteCourse_authorizedRole_returns204(String role) throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(courseService).deleteCourse(id);

        mockMvc.perform(delete("/courses/{id}", id).with(user("u").roles(role)))
                .andExpect(status().isNoContent());

        verify(courseService, times(1)).deleteCourse(id);
    }

    @ParameterizedTest
    @ValueSource(strings = {"USER", "FAKE_USER"})
    @DisplayName("DELETE /courses/{id} — неавторизована роль → 403 Forbidden")
    void deleteCourse_unauthorizedRole_returns403(String role) throws Exception {
        mockMvc.perform(delete("/courses/{id}", UUID.randomUUID())
                        .with(user("u").roles(role)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(courseService);
    }

    @Test
    @DisplayName("DELETE /courses/{id} — анонімний → 403 Forbidden")
    void deleteCourse_anonymous_returns403() throws Exception {
        mockMvc.perform(delete("/courses/{id}", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    // СЕКЦІЯ: POST /courses/{id}/extend-access

    @Test
    @DisplayName("POST /courses/{id}/extend-access — авторизований → 200 OK")
    @WithMockUser(username = "user@test.com")
    void extendAccess_authenticated_returns200() throws Exception {
        UUID courseId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        PersonEntity currentUser = personEntity(userId, PersonRole.USER);
        
        when(personRepository.findByEmail("user@test.com")).thenReturn(Optional.of(currentUser));
        
        MockMultipartFile videoPart = new MockMultipartFile(
                "video", "review.mp4", "video/mp4", "fake-video-content".getBytes()
        );

        mockMvc.perform(multipart("/courses/{id}/extend-access", courseId)
                        .file(videoPart))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /courses/{id}/extend-access — анонімний → 400 Bad Request")
    void extendAccess_anonymous_returns400() throws Exception {
        MockMultipartFile videoPart = new MockMultipartFile(
                "video", "review.mp4", "video/mp4", "fake-video-content".getBytes()
        );

        // Security filter expects authenticated user
        mockMvc.perform(multipart("/courses/{id}/extend-access", UUID.randomUUID())
                        .file(videoPart))
                .andExpect(status().isBadRequest());
    }

    // СЕКЦІЯ: POST /courses/{id}/clone

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "FAKE_ADMIN"})
    @DisplayName("POST /courses/{id}/clone — авторизована роль → 201 Created")
    void cloneCourse_authorizedRole_returns201(String role) throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(courseService).cloneCourse(id);

        mockMvc.perform(post("/courses/{id}/clone", id).with(user("u").roles(role)))
                .andExpect(status().isCreated());

        verify(courseService, times(1)).cloneCourse(id);
    }

    @ParameterizedTest
    @ValueSource(strings = {"USER", "FAKE_USER"})
    @DisplayName("POST /courses/{id}/clone — неавторизована роль → 403 Forbidden")
    void cloneCourse_unauthorizedRole_returns403(String role) throws Exception {
        mockMvc.perform(post("/courses/{id}/clone", UUID.randomUUID())
                        .with(user("u").roles(role)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(courseService);
    }

    @Test
    @DisplayName("POST /courses/{id}/clone — анонімний → 403 Forbidden")
    void cloneCourse_anonymous_returns403() throws Exception {
        mockMvc.perform(post("/courses/{id}/clone", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    // СЕКЦІЯ: PATCH /courses/{id}/status

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "FAKE_ADMIN"})
    @DisplayName("PATCH /courses/{id}/status — авторизована роль → 200 OK")
    void updateCourseStatus_authorizedRole_returns200(String role) throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(courseService).updateCourseStatus(id, CourseStatus.PUBLISHED);

        mockMvc.perform(patch("/courses/{id}/status", id)
                        .param("status", "PUBLISHED")
                        .with(user("u").roles(role)))
                .andExpect(status().isOk());

        verify(courseService, times(1)).updateCourseStatus(id, CourseStatus.PUBLISHED);
    }

    @ParameterizedTest
    @ValueSource(strings = {"USER", "FAKE_USER"})
    @DisplayName("PATCH /courses/{id}/status — неавторизована роль → 403 Forbidden")
    void updateCourseStatus_unauthorizedRole_returns403(String role) throws Exception {
        mockMvc.perform(patch("/courses/{id}/status", UUID.randomUUID())
                        .param("status", "PUBLISHED")
                        .with(user("u").roles(role)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(courseService);
    }
    
    @Test
    @DisplayName("PATCH /courses/{id}/status — анонімний → 403 Forbidden")
    void updateCourseStatus_anonymous_returns403() throws Exception {
        mockMvc.perform(patch("/courses/{id}/status", UUID.randomUUID())
                        .param("status", "PUBLISHED"))
                .andExpect(status().isForbidden());
    }

    // ХЕЛПЕРИ (FACTORY METHODS)

    @NotNull
    @Contract("_, _ -> new")
    private CourseDto courseDto(UUID id, String name) {
        return new CourseDto(id, name, "desc", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    @NotNull
    @Contract(" -> new")
    private CourseCreateDto createDto() {
        return new CourseCreateDto("Course", "desc", null, null, null, null, null, null, null, null);
    }

    @NotNull
    @Contract(" -> new")
    private CourseUpdateDto updateDto() {
        return new CourseUpdateDto("Course Updated", "desc", null, null, null, null, null, null, null, null, null, false);
    }

    @NotNull
    @Contract("_, _ -> new")
    private PersonEntity personEntity(UUID id, PersonRole role) {
        PersonEntity current = new PersonEntity();
        current.setId(id);
        current.setRole(role);
        return current;
    }
}