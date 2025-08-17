package com.convox.patientservice.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class PatientResponseDTO {
    private String id;
    private String name;
    private String email;
    private String address;
    private String dateOfBirth;

}
