package com.barboya.addresses;

import com.barboya.common.ApiException;
import com.barboya.users.UserRepository;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class AddressService {

    private static final int MAX_ADDRESSES = 10;

    public record AddressRequest(
            @NotBlank @Size(min = 2, max = 40) String label,
            @NotBlank @Size(min = 5, max = 200) String address,
            @Size(max = 300) String notes,
            @DecimalMin("-90") @DecimalMax("90") Double latitude,
            @DecimalMin("-180") @DecimalMax("180") Double longitude) {

        /** La direccion y la ubicacion no deben quedar en los registros. */
        @Override
        public String toString() {
            return "AddressRequest";
        }
    }

    public record AddressResponse(
            UUID id, String label, String address, String notes, Double latitude, Double longitude) {

        static AddressResponse from(Address a) {
            return new AddressResponse(a.getId(), a.getLabel(), a.getAddress(), a.getNotes(), a.getLatitude(),
                    a.getLongitude());
        }
    }

    private final AddressRepository addresses;
    private final UserRepository users;
    private final Clock clock;

    public AddressService(AddressRepository addresses, UserRepository users, Clock clock) {
        this.addresses = addresses;
        this.users = users;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> list(UUID userId) {
        return addresses.findByUserIdOrderByLabel(userId).stream().map(AddressResponse::from).toList();
    }

    public AddressResponse add(UUID userId, AddressRequest request) {
        // Bloquear a la persona evita saltarse el limite con peticiones simultaneas
        users.lockById(userId).orElseThrow(AddressService::notFound);
        if (addresses.findByUserIdOrderByLabel(userId).size() >= MAX_ADDRESSES) {
            throw new ApiException(HttpStatus.CONFLICT, "TOO_MANY_ADDRESSES",
                    "Puedes guardar maximo 10 direcciones");
        }
        return save(new Address(userId, clock.instant()), request);
    }

    public AddressResponse update(UUID userId, UUID id, AddressRequest request) {
        return save(own(userId, id), request);
    }

    /** Al eliminar la cuenta. */
    public void removeAll(UUID userId) {
        addresses.deleteAll(addresses.findByUserIdOrderByLabel(userId));
    }

    public void remove(UUID userId, UUID id) {
        addresses.delete(own(userId, id));
    }

    private AddressResponse save(Address target, AddressRequest request) {
        String label = request.label().trim();
        boolean taken = addresses.findByUserIdOrderByLabel(target.getUserId()).stream()
                .anyMatch(a -> !a.getId().equals(target.getId()) && a.getLabel().equalsIgnoreCase(label));
        if (taken) {
            throw new ApiException(HttpStatus.CONFLICT, "ADDRESS_LABEL_TAKEN",
                    "Ya tienes una direccion con ese nombre");
        }
        if ((request.latitude() == null) != (request.longitude() == null)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_LOCATION", "Ubicacion no valida");
        }
        String notes = request.notes() == null || request.notes().isBlank() ? null : request.notes().trim();
        target.update(label, request.address().trim(), notes, request.latitude(), request.longitude(),
                clock.instant());
        return AddressResponse.from(addresses.saveAndFlush(target));
    }

    /** Una direccion ajena responde igual que una inexistente. */
    private Address own(UUID userId, UUID id) {
        return addresses.findByIdAndUserId(id, userId).orElseThrow(AddressService::notFound);
    }

    private static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "ADDRESS_NOT_FOUND", "Direccion no encontrada");
    }
}
