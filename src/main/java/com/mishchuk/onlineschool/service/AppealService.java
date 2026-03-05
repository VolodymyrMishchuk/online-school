package com.mishchuk.onlineschool.service;

import com.mishchuk.onlineschool.controller.dto.AppealCreateRequest;
import com.mishchuk.onlineschool.controller.dto.AppealResponse;
import com.mishchuk.onlineschool.repository.entity.AppealStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface AppealService {
    AppealResponse createAppeal(UUID userId, AppealCreateRequest request, List<MultipartFile> photos);

    Page<AppealResponse> getAppeals(Pageable pageable);

    AppealResponse getAppeal(UUID id);

    AppealResponse updateAppealStatus(UUID id, AppealStatus status);

    void deleteAppeal(UUID id);
}
