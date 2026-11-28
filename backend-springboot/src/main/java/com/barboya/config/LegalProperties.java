package com.barboya.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Quien opera la plataforma. {@code reviewed} se activa solo cuando un abogado
 * haya aprobado los documentos.
 */
@ConfigurationProperties(prefix = "barboya.legal")
public record LegalProperties(
        @DefaultValue("") String operatorName,
        @DefaultValue("") String operatorId,
        @DefaultValue("") String operatorAddress,
        @DefaultValue("") String operatorPhone,
        @DefaultValue("") String operatorEmail,
        @DefaultValue("false") boolean reviewed) {

    public boolean operatorComplete() {
        return !(operatorName.isBlank() || operatorId.isBlank() || operatorAddress.isBlank()
                || operatorPhone.isBlank() || operatorEmail.isBlank());
    }
}
