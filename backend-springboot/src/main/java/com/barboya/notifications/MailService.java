package com.barboya.notifications;

import com.barboya.common.ApiException;
import com.barboya.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final ObjectProvider<JavaMailSender> sender;
    private final String from;
    private final String host;

    public MailService(ObjectProvider<JavaMailSender> sender, AppProperties properties,
                       @Value("${spring.mail.host:}") String host) {
        this.sender = sender;
        this.from = properties.mail().from();
        this.host = host;
    }

    /** Una variable SMTP definida pero vacia no cuenta como configurada. */
    public boolean isConfigured() {
        return sender.getIfAvailable() != null && !from.isBlank() && !host.isBlank();
    }

    /** Falla de forma explicita cuando no hay como entregar el mensaje. */
    public void requireConfigured() {
        if (!isConfigured()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "MAIL_NOT_CONFIGURED",
                    "El envio de correos no esta disponible por ahora");
        }
    }

    /** Se envia despues de confirmar la transaccion y sin retrasar la respuesta. */
    @Async
    @TransactionalEventListener
    public void send(MailRequested mail) {
        if (!isConfigured()) {
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(mail.to());
        message.setSubject(mail.subject());
        message.setText(mail.body());
        try {
            sender.getObject().send(message);
        } catch (MailException ex) {
            // Sin destinatario ni contenido: pueden incluir codigos de acceso
            log.error("No se pudo enviar un correo: {}", ex.getClass().getSimpleName());
        }
    }
}
