package com.mishchuk.onlineschool.service;

import com.mishchuk.onlineschool.controller.dto.AuthRequest;
import com.mishchuk.onlineschool.controller.dto.AuthResultDto;
import com.mishchuk.onlineschool.controller.dto.PersonCreateDto;

public interface AuthService {
    AuthResultDto registerUser(PersonCreateDto request);

    AuthResultDto authenticateUser(AuthRequest request);

    AuthResultDto refreshToken(String refreshToken);

    void logout(String refreshToken);

    AuthResultDto magicLogin(String token);
}
