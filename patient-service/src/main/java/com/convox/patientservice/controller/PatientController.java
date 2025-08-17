package com.convox.patientservice.controller;

import com.convox.patientservice.dto.PatientRequestDTO;
import com.convox.patientservice.dto.PatientResponseDTO;
import com.convox.patientservice.dto.validators.CreatePatientValidationGroup;
import com.convox.patientservice.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.groups.Default;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/patients")
@Validated
@RequiredArgsConstructor
@Tag(name = "Patient", description = "Endpoints for managing patient data")
public class PatientController {
    private final PatientService patientService;
    private final Logger logger = LoggerFactory.getLogger(PatientController.class);

    @GetMapping
    @Operation(summary = "Retrieve all patients", description = "Returns a paginated list of patients")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of patients retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class)))
    })
    public ResponseEntity<Page<PatientResponseDTO>> getAllPatients(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        logger.debug("Received request to fetch patients, page: {}, size: {}", page, size);
        Page<PatientResponseDTO> patients = patientService.getPatients(page, size);
        return ResponseEntity.ok().body(patients);
    }

    @PostMapping
    @Operation(summary = "Create a new patient", description = "Adds a new patient to the system")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Patient created successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PatientResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content)
    })
    public ResponseEntity<PatientResponseDTO> createPatient(
            @Validated({Default.class, CreatePatientValidationGroup.class})
            @RequestBody PatientRequestDTO patientRequestDTO) {
        logger.debug("Received request to create patient with email: {}", patientRequestDTO.getEmail());
        PatientResponseDTO response = patientService.createPatient(patientRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update existing patient", description = "Modifies the details of an existing patient")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Patient updated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PatientResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Patient not found"),
            @ApiResponse(responseCode = "400", description = "Validation error")
    })
    public ResponseEntity<PatientResponseDTO> updatePatient(
            @Parameter(description = "UUID of the patient to update") @PathVariable UUID id,
            @Validated({Default.class}) @RequestBody PatientRequestDTO patientRequestDTO) {
        logger.debug("Received request to update patient with ID: {}", id);
        PatientResponseDTO response = patientService.updatePatient(id, patientRequestDTO);
        return ResponseEntity.ok().body(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a patient", description = "Deletes a patient record by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Patient deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Patient not found")
    })
    public ResponseEntity<Void> deletePatient(
            @Parameter(description = "UUID of the patient to delete") @PathVariable UUID id) {
        logger.debug("Received request to delete patient with ID: {}", id);
        patientService.deletePatient(id);
        return ResponseEntity.noContent().build();
    }
}
