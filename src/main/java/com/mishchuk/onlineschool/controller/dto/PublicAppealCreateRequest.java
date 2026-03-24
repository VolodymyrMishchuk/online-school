package com.mishchuk.onlineschool.controller.dto;

import com.mishchuk.onlineschool.repository.entity.ContactMethod;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Data
public class PublicAppealCreateRequest {

    @NotBlank(message = "Name cannot be blank")
    @Size(max = 255, message = "Name must be less than 255 characters")
    private String name;

    @NotNull(message = "Contact method cannot be null")
    private ContactMethod contactMethod;

    @NotBlank(message = "Contact details cannot be blank")
    @Size(max = 255, message = "Contact details must be less than 255 characters")
    private String contactDetails;

    @NotBlank(message = "Message cannot be blank")
    @Size(max = 2000, message = "Message must be less than 2000 characters")
    private String message;
}
