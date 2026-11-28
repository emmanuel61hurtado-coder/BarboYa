package com.barboya.couriers;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class CourierDtos {

    private CourierDtos() {
    }

    public record ProfileRequest(
            @NotBlank @Pattern(regexp = "^[A-Za-z0-9]{5,20}$", message = "Numero de documento no valido")
            String documentNumber,
            @NotNull VehicleType vehicleType,
            @Pattern(regexp = "^$|^[A-Za-z0-9-]{5,8}$", message = "Placa no valida") String plate,
            @NotNull @AssertTrue(message = "Debes autorizar el tratamiento de tus datos")
            Boolean acceptsDataProcessing) {

        @Override
        public String toString() {
            return "ProfileRequest[vehicleType=" + vehicleType + "]";
        }
    }

    public record ReviewRequest(@NotNull CourierDocument.Status decision, @Size(max = 300) String note) {
    }

    public record Profile(String documentNumber, VehicleType vehicleType, String plate, Instant consentAt) {
    }

    public record Requirement(DocumentType type, String label, boolean required, boolean expires) {
    }

    public record Document(
            UUID id, DocumentType type, CourierDocument.Status status, String contentType, LocalDate expiresOn,
            boolean expired, String reviewNote, Instant uploadedAt) {
    }

    /** {@code pending} explica que falta para poder aprobar la cuenta. */
    public record CourierView(
            Profile profile, List<Requirement> requirements, List<Document> documents, List<String> pending) {
    }

    public record FileContent(byte[] content, String contentType) {
    }
}
