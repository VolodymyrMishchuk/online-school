package com.mishchuk.onlineschool.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mishchuk.onlineschool.dto.PromoCodeCheckResponseDto;
import com.mishchuk.onlineschool.dto.PromoCodeCreateDto;
import com.mishchuk.onlineschool.dto.PromoCodeResponseDto;
import com.mishchuk.onlineschool.exception.GlobalExceptionHandler;
import com.mishchuk.onlineschool.repository.entity.PromoCodeScope;
import com.mishchuk.onlineschool.repository.entity.PromoCodeStatus;
import com.mishchuk.onlineschool.security.CustomUserDetailsService;
import com.mishchuk.onlineschool.security.JwtUtils;
import com.mishchuk.onlineschool.service.PromoCodeService;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PromoCodeController.class)
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
class PromoCodeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PromoCodeService promoCodeService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @MockBean
    private JwtUtils jwtUtils;

    // --- GET /promo-codes/paginated ---

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "FAKE_ADMIN"})
    @DisplayName("GET /promo-codes/paginated — авторизований ADMIN → 200 OK")
    void getPaginatedPromoCodes_authorized_returns200(String role) throws Exception {
        Page<PromoCodeResponseDto> page = new PageImpl<>(List.of(promoCodeResponseDto()));
        when(promoCodeService.getPaginatedPromoCodes(any(), any(), any(), any(), any(Pageable.class), eq("admin")))
                .thenReturn(page);

        mockMvc.perform(get("/promo-codes/paginated")
                        .with(user("admin").roles(role)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].code").value("CODE123"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"USER", "FAKE_USER"})
    @DisplayName("GET /promo-codes/paginated — неавторизований USER → 403 Forbidden")
    void getPaginatedPromoCodes_unauthorized_returns403(String role) throws Exception {
        mockMvc.perform(get("/promo-codes/paginated")
                        .with(user("user").roles(role)))
                .andExpect(status().isForbidden());
    }

    // --- POST /promo-codes ---

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "FAKE_ADMIN"})
    @DisplayName("POST /promo-codes — авторизований ADMIN → 201 Created")
    void createPromoCode_authorized_returns201(String role) throws Exception {
        when(promoCodeService.createPromoCode(any(PromoCodeCreateDto.class), eq("admin")))
                .thenReturn(promoCodeResponseDto());

        mockMvc.perform(post("/promo-codes")
                        .with(user("admin").roles(role))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("CODE123"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"USER", "FAKE_USER"})
    @DisplayName("POST /promo-codes — неавторизований USER → 403 Forbidden")
    void createPromoCode_unauthorized_returns403(String role) throws Exception {
        mockMvc.perform(post("/promo-codes")
                        .with(user("user").roles(role))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto())))
                .andExpect(status().isForbidden());
    }

    // --- GET /promo-codes/check ---

    @Test
    @DisplayName("GET /promo-codes/check — перевірка коду → 200 OK")
    @WithMockUser(username = "user")
    void checkPromoCode_returns200() throws Exception {
        PromoCodeCheckResponseDto responseDto = new PromoCodeCheckResponseDto("CODE123", List.of(), List.of());
        when(promoCodeService.checkPromoCode("CODE123", "user")).thenReturn(responseDto);

        mockMvc.perform(get("/promo-codes/check")
                        .param("code", "CODE123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("CODE123"));
    }

    // --- POST /promo-codes/use ---

    @Test
    @DisplayName("POST /promo-codes/use — застосування коду → 200 OK")
    @WithMockUser(username = "user")
    void usePromoCode_returns200() throws Exception {
        mockMvc.perform(post("/promo-codes/use")
                        .param("code", "CODE123")
                        .param("courseId", UUID.randomUUID().toString()))
                .andExpect(status().isOk());

        verify(promoCodeService, times(1)).usePromoCode(eq("CODE123"), any(UUID.class), eq("user"));
    }

    // --- PUT /promo-codes/{id} ---

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "FAKE_ADMIN"})
    @DisplayName("PUT /promo-codes/{id} — авторизований ADMIN → 200 OK")
    void updatePromoCode_authorized_returns200(String role) throws Exception {
        UUID id = UUID.randomUUID();
        when(promoCodeService.updatePromoCode(eq(id), any(PromoCodeCreateDto.class), eq("admin")))
                .thenReturn(promoCodeResponseDto());

        mockMvc.perform(put("/promo-codes/{id}", id)
                        .with(user("admin").roles(role))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("CODE123"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"USER", "FAKE_USER"})
    @DisplayName("PUT /promo-codes/{id} — неавторизований USER → 403 Forbidden")
    void updatePromoCode_unauthorized_returns403(String role) throws Exception {
        mockMvc.perform(put("/promo-codes/{id}", UUID.randomUUID())
                        .with(user("user").roles(role))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto())))
                .andExpect(status().isForbidden());
    }

    // --- DELETE /promo-codes/{id} ---

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "FAKE_ADMIN"})
    @DisplayName("DELETE /promo-codes/{id} — авторизований ADMIN → 204 No Content")
    void deletePromoCode_authorized_returns204(String role) throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(promoCodeService).deletePromoCode(id, "admin");

        mockMvc.perform(delete("/promo-codes/{id}", id)
                        .with(user("admin").roles(role)))
                .andExpect(status().isNoContent());

        verify(promoCodeService, times(1)).deletePromoCode(id, "admin");
    }

    @ParameterizedTest
    @ValueSource(strings = {"USER", "FAKE_USER"})
    @DisplayName("DELETE /promo-codes/{id} — неавторизований USER → 403 Forbidden")
    void deletePromoCode_unauthorized_returns403(String role) throws Exception {
        mockMvc.perform(delete("/promo-codes/{id}", UUID.randomUUID())
                        .with(user("user").roles(role)))
                .andExpect(status().isForbidden());
    }

    // ХЕЛПЕРИ

    @NotNull
    private PromoCodeCreateDto createDto() {
        PromoCodeCreateDto dto = new PromoCodeCreateDto();
        dto.setCode("CODE123");
        dto.setStatus(PromoCodeStatus.ACTIVE);
        dto.setScope(PromoCodeScope.GLOBAL);
        dto.setTargetPersonIds(Set.of());
        dto.setValidFrom(LocalDateTime.now());
        dto.setValidUntil(LocalDateTime.now().plusDays(30));
        dto.setDiscounts(List.of());
        return dto;
    }

    @NotNull
    private PromoCodeResponseDto promoCodeResponseDto() {
        PromoCodeResponseDto dto = new PromoCodeResponseDto();
        dto.setId(UUID.randomUUID());
        dto.setCode("CODE123");
        dto.setStatus(PromoCodeStatus.ACTIVE);
        dto.setScope(PromoCodeScope.GLOBAL);
        dto.setTargetPersons(List.of());
        dto.setValidFrom(LocalDateTime.now());
        dto.setValidUntil(LocalDateTime.now().plusDays(30));
        dto.setValidFromDisplay("Now");
        dto.setValidUntilDisplay("Later");
        dto.setPendingActivation(false);
        dto.setDiscounts(List.of());
        return dto;
    }
}
