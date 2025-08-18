package com.convox.patientservice.service;

import com.convox.patientservice.dto.PatientRequestDTO;
import com.convox.patientservice.dto.PatientResponseDTO;
import com.convox.patientservice.exception.EmailAlreadyExistsException;
import com.convox.patientservice.exception.PatientNotFoundException;
import com.convox.patientservice.grpc.BillingServiceGrpcClient;
import com.convox.patientservice.mapper.PatientMapper;
import com.convox.patientservice.model.Patient;
import com.convox.patientservice.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PatientService {
    private final PatientRepository patientRepository;
    private final BillingServiceGrpcClient billingServiceGrpcClient;
    private final Logger logger = LoggerFactory.getLogger(PatientService.class);

    @Cacheable(value = "patients", key = "'all_patients_' + #page + '_' + #size")
    @Transactional(readOnly = true)
    public Page<PatientResponseDTO> getPatients(int page, int size) {
        logger.debug("Fetching patients with page: {}, size: {}", page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<Patient> patients = patientRepository.findAll(pageable);
        return patients.map(PatientMapper::toPatientResponseDTO);
    }

    @Transactional
    @CacheEvict(value = "patients", allEntries = true)
    public PatientResponseDTO createPatient(PatientRequestDTO patientRequestDTO) {
        logger.info("Creating patient with email: {}", patientRequestDTO.getEmail());
        if (patientRepository.existsByEmail(patientRequestDTO.getEmail())) {
            logger.warn("Email already exists: {}", patientRequestDTO.getEmail());
            throw new EmailAlreadyExistsException("A patient with this email already exists: " + patientRequestDTO.getEmail());
        }
        Patient newPatient = PatientMapper.toPatient(patientRequestDTO);
        newPatient.setRegisteredDate(LocalDate.now());
        newPatient = patientRepository.save(newPatient);
        billingServiceGrpcClient.createBillingAccount(newPatient.getId().toString(), newPatient.getName(), newPatient.getEmail());
        logger.info("Patient created with ID: {}", newPatient.getId());
        return PatientMapper.toPatientResponseDTO(newPatient);
    }

    @Transactional
    @CacheEvict(value = "patients", allEntries = true)
    public PatientResponseDTO updatePatient(UUID id, PatientRequestDTO patientRequestDTO) {
        logger.debug("Updating patient with ID: {}", id);

        // Validate DOB parsing early to avoid DB hit for invalid input
        LocalDate requestDob;
        try {
            requestDob = LocalDate.parse(patientRequestDTO.getDateOfBirth());
        } catch (Exception e) {
            logger.warn("Invalid date of birth format: {}", patientRequestDTO.getDateOfBirth());
            throw new IllegalArgumentException("Invalid date of birth format: " + patientRequestDTO.getDateOfBirth());
        }

        // Fetch patient (cached if possible)
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Patient not found with ID: {}", id);
                    return new PatientNotFoundException("Patient not found with ID: " + id);
                });

        // Skip email conflict check if email is unchanged
        if (!patient.getEmail().equals(patientRequestDTO.getEmail())) {
            if (patientRepository.existsByEmailAndIdNot(patientRequestDTO.getEmail(), id)) {
                logger.warn("Email conflict detected: {}", patientRequestDTO.getEmail());
                throw new EmailAlreadyExistsException("A patient with this email already exists: " + patientRequestDTO.getEmail());
            }
        }

        // Check for changes to avoid unnecessary updates
        boolean hasChanges = !patient.getName().equals(patientRequestDTO.getName())
                || !patient.getEmail().equals(patientRequestDTO.getEmail())
                || !patient.getAddress().equals(patientRequestDTO.getAddress())
                || !patient.getDateOfBirth().equals(requestDob);

        if (!hasChanges) {
            logger.debug("No changes detected for patient ID: {}", id);
            return PatientMapper.toPatientResponseDTO(patient);
        }

        // Update fields only if changes detected
        patient.setName(patientRequestDTO.getName());
        patient.setEmail(patientRequestDTO.getEmail());
        patient.setAddress(patientRequestDTO.getAddress());
        patient.setDateOfBirth(requestDob);
        patient = patientRepository.save(patient);
        logger.info("Patient updated with ID: {}", id);
        return PatientMapper.toPatientResponseDTO(patient);
    }

    @Transactional
    @CacheEvict(value = "patients", allEntries = true)
    public void deletePatient(UUID id) {
        logger.debug("Deleting patient with ID: {}", id);
        if(patientRepository.existsById(id)) {
            patientRepository.deleteById(id);
            logger.info("Patient deleted with ID: {}", id);
        }
        else {
            logger.warn("Patient not found with ID: {}", id);
        }
    }
}