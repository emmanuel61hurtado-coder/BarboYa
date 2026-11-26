package com.barboya.couriers;

import com.barboya.audit.AuditAction;
import com.barboya.audit.AuditService;
import com.barboya.common.ApiException;
import com.barboya.common.ClientInfo;
import com.barboya.couriers.CourierDocument.Status;
import com.barboya.couriers.CourierDtos.CourierView;
import com.barboya.couriers.CourierDtos.Document;
import com.barboya.couriers.CourierDtos.FileContent;
import com.barboya.couriers.CourierDtos.Profile;
import com.barboya.couriers.CourierDtos.ProfileRequest;
import com.barboya.couriers.CourierDtos.Requirement;
import com.barboya.couriers.CourierDtos.ReviewRequest;
import com.barboya.files.FileStorage;
import com.barboya.files.Uploads;
import com.barboya.users.ActivationRequirement;
import com.barboya.users.Role;
import com.barboya.users.User;
import com.barboya.users.UserRepository;
import com.barboya.users.UserStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Verificacion de domiciliarios. Los cambios bloquean la fila del usuario, igual
 * que la aprobacion, para que nunca se apruebe algo que esta cambiando.
 */
@Service
@Transactional
public class CourierService implements ActivationRequirement {

    private static final ZoneId COLOMBIA = ZoneId.of("America/Bogota");

    private final CourierProfileRepository profiles;
    private final CourierDocumentRepository documents;
    private final UserRepository users;
    private final FileStorage storage;
    private final AuditService audit;
    private final Clock clock;

    public CourierService(CourierProfileRepository profiles, CourierDocumentRepository documents,
                          UserRepository users, FileStorage storage, AuditService audit, Clock clock) {
        this.profiles = profiles;
        this.documents = documents;
        this.users = users;
        this.storage = storage;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public CourierView view(UUID userId) {
        CourierProfile profile = profiles.findById(userId).orElse(null);
        List<CourierDocument> uploaded = documents.findByUserId(userId);
        LocalDate today = today();
        List<Requirement> requirements = profile == null ? List.of()
                : DocumentType.acceptedFor(profile.getVehicleType()).stream()
                        .map(type -> new Requirement(type, type.label(),
                                DocumentType.requiredFor(profile.getVehicleType()).contains(type), type.expires()))
                        .toList();
        return new CourierView(
                profile == null ? null : new Profile(profile.getDocumentNumber(), profile.getVehicleType(),
                        profile.getPlate(), profile.getConsentAt()),
                requirements,
                uploaded.stream()
                        .sorted(Comparator.comparing(CourierDocument::getType))
                        .map(d -> new Document(d.getId(), d.getType(), d.getStatus(), d.getContentType(),
                                d.getExpiresOn(), d.isExpired(today), d.getReviewNote(), d.getUploadedAt()))
                        .toList(),
                pending(profile, uploaded));
    }

    public CourierView saveProfile(UUID userId, ProfileRequest request, ClientInfo client) {
        User user = lock(userId);
        String number = request.documentNumber().trim().toUpperCase();
        String plate = request.vehicleType() == VehicleType.BICYCLE || request.plate() == null
                || request.plate().isBlank() ? null : request.plate().trim().toUpperCase();
        if (request.vehicleType() != VehicleType.BICYCLE && plate == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PLATE_REQUIRED", "Escribe la placa del vehiculo");
        }
        if (profiles.existsByDocumentNumberAndUserIdNot(number, userId)) {
            throw new ApiException(HttpStatus.CONFLICT, "DOCUMENT_NUMBER_TAKEN",
                    "Ese numero de documento ya esta registrado en otra cuenta");
        }
        CourierProfile existing = profiles.findById(userId).orElse(null);
        boolean changed = existing != null && !(existing.getDocumentNumber().equals(number)
                && existing.getVehicleType() == request.vehicleType() && Objects.equals(existing.getPlate(), plate));
        CourierProfile profile = existing != null ? existing : CourierProfile.create(userId, clock.instant());
        profile.setDocumentNumber(number);
        profile.setVehicleType(request.vehicleType());
        profile.setPlate(plate);
        profile.setUpdatedAt(clock.instant());
        profiles.saveAndFlush(profile);
        if (changed) {
            reopen(user, "cambio de datos", client);
        }
        return view(userId);
    }

    public CourierView upload(UUID userId, DocumentType type, MultipartFile file, LocalDate expiresOn,
                              ClientInfo client) {
        User user = lock(userId);
        CourierProfile profile = profiles.findById(userId).orElseThrow(() -> new ApiException(
                HttpStatus.CONFLICT, "PROFILE_REQUIRED", "Primero guarda tus datos y tu autorizacion"));
        if (!DocumentType.acceptedFor(profile.getVehicleType()).contains(type)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "DOCUMENT_NOT_APPLICABLE",
                    "Ese documento no aplica a tu vehiculo");
        }
        if (type.expires() && (expiresOn == null || expiresOn.isBefore(today()))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_EXPIRY",
                    "Indica una fecha de vencimiento vigente");
        }
        var upload = Uploads.read(file, type != DocumentType.FACE_PHOTO);

        documents.findByUserIdAndType(userId, type).ifPresent(this::remove);
        documents.flush();
        String key = storage.store(upload.content());
        deleteFileIfRolledBack(key);
        CourierDocument document = documents.saveAndFlush(new CourierDocument(userId, type, key,
                upload.contentType(), upload.content().length, type.expires() ? expiresOn : null, clock.instant()));
        audit.record(AuditAction.DOCUMENT_UPLOADED, userId, userId, type.name() + " " + document.getId(), client);
        if (DocumentType.requiredFor(profile.getVehicleType()).contains(type)) {
            reopen(user, "documento nuevo: " + type, client);
        }
        return view(userId);
    }

    public CourierView delete(UUID userId, UUID documentId, ClientInfo client) {
        User user = lock(userId);
        CourierDocument document = own(userId, documentId);
        remove(document);
        documents.flush();
        audit.record(AuditAction.DOCUMENT_DELETED, userId, userId, document.getType().name(), client);
        CourierProfile profile = profiles.findById(userId).orElseThrow();
        if (DocumentType.requiredFor(profile.getVehicleType()).contains(document.getType())) {
            reopen(user, "documento eliminado: " + document.getType(), client);
        }
        return view(userId);
    }

    @Transactional(readOnly = true)
    public FileContent ownFile(UUID userId, UUID documentId) {
        return content(own(userId, documentId));
    }

    /** Cada consulta de un documento por el equipo queda registrada. */
    public FileContent reviewFile(UUID reviewerId, UUID documentId, ClientInfo client) {
        CourierDocument document = documents.findById(documentId).orElseThrow(CourierService::notFound);
        audit.record(AuditAction.DOCUMENT_VIEWED, reviewerId, document.getUserId(),
                document.getType().name() + " " + documentId, client);
        return content(document);
    }

    public CourierView review(UUID reviewerId, UUID documentId, ReviewRequest request, ClientInfo client) {
        UUID ownerId = documents.findById(documentId).orElseThrow(CourierService::notFound).getUserId();
        User owner = lock(ownerId);
        CourierDocument document = documents.findById(documentId).orElseThrow(CourierService::notFound);
        String note = request.note() == null || request.note().isBlank() ? null : request.note().trim();
        if (request.decision() == Status.PENDING) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_DECISION", "Aprueba o rechaza el documento");
        }
        if (request.decision() == Status.REJECTED && (note == null || note.length() < 5)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "REASON_REQUIRED", "Explica por que se rechaza");
        }
        if (request.decision() == Status.APPROVED && document.isExpired(today())) {
            throw new ApiException(HttpStatus.CONFLICT, "DOCUMENT_EXPIRED", "El documento esta vencido");
        }
        document.review(request.decision(), note, reviewerId, clock.instant());
        audit.record(AuditAction.DOCUMENT_REVIEWED, reviewerId, ownerId,
                document.getType() + " " + request.decision() + (note == null ? "" : ": " + note), client);
        CourierProfile profile = profiles.findById(ownerId).orElseThrow();
        if (request.decision() == Status.REJECTED
                && DocumentType.requiredFor(profile.getVehicleType()).contains(document.getType())) {
            reopen(owner, "documento rechazado: " + document.getType(), client);
        }
        documents.flush();
        return view(ownerId);
    }

    @Override
    @Transactional(readOnly = true)
    public void verify(User user) {
        if (user.getRole() != Role.COURIER) {
            return;
        }
        List<String> missing = pending(profiles.findById(user.getId()).orElse(null),
                documents.findByUserId(user.getId()));
        if (!missing.isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT, "DOCUMENTS_INCOMPLETE",
                    "No se puede activar todavia. Falta: " + String.join("; ", missing));
        }
    }

    /** Para trabajar: cuenta activa y documentos vigentes hoy. */
    @Transactional(readOnly = true)
    public void requireOperable(UUID userId) {
        boolean active = users.findById(userId).filter(u -> u.getStatus() == UserStatus.ACTIVE).isPresent();
        List<String> missing = pending(profiles.findById(userId).orElse(null), documents.findByUserId(userId));
        if (!active || !missing.isEmpty()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "COURIER_NOT_OPERABLE", missing.isEmpty()
                    ? "Tu cuenta debe estar aprobada para operar"
                    : "No puedes operar. Revisa: " + String.join("; ", missing));
        }
    }

    private List<String> pending(CourierProfile profile, List<CourierDocument> uploaded) {
        if (profile == null) {
            return List.of("datos y autorización del domiciliario");
        }
        Map<DocumentType, CourierDocument> byType = uploaded.stream()
                .collect(Collectors.toMap(CourierDocument::getType, Function.identity()));
        List<String> result = new ArrayList<>();
        LocalDate today = today();
        for (DocumentType type : DocumentType.requiredFor(profile.getVehicleType())) {
            CourierDocument document = byType.get(type);
            if (document == null) {
                result.add(type.label() + " (sin subir)");
            } else if (document.isExpired(today)) {
                result.add(type.label() + " (vencido)");
            } else if (document.getStatus() == Status.PENDING) {
                result.add(type.label() + " (por revisar)");
            } else if (document.getStatus() == Status.REJECTED) {
                result.add(type.label() + " (rechazado)");
            }
        }
        return result;
    }

    /** Una cuenta aprobada vuelve a revision cuando cambia lo que se aprobo. */
    private void reopen(User user, String reason, ClientInfo client) {
        if (user.getStatus() == UserStatus.ACTIVE) {
            Instant now = clock.instant();
            user.setStatus(UserStatus.PENDING_VERIFICATION);
            user.setUpdatedAt(now);
            audit.record(AuditAction.VERIFICATION_REOPENED, null, user.getId(), reason, client);
        }
    }

    private void remove(CourierDocument document) {
        documents.delete(document);
        String key = document.getStorageKey();
        // El archivo se borra solo si el cambio queda confirmado
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                storage.delete(key);
            }
        });
    }

    private void deleteFileIfRolledBack(String key) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    storage.delete(key);
                }
            }
        });
    }

    private FileContent content(CourierDocument document) {
        return new FileContent(storage.read(document.getStorageKey()), document.getContentType());
    }

    private CourierDocument own(UUID userId, UUID documentId) {
        return documents.findById(documentId)
                .filter(d -> d.getUserId().equals(userId))
                .orElseThrow(CourierService::notFound);
    }

    private User lock(UUID userId) {
        return users.lockById(userId).orElseThrow(CourierService::notFound);
    }

    private LocalDate today() {
        return LocalDate.ofInstant(clock.instant(), COLOMBIA);
    }

    private static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "DOCUMENT_NOT_FOUND", "Documento no encontrado");
    }
}
