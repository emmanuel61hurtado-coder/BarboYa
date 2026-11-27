package com.barboya;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Solo se cargan los modulos listados aqui. El esqueleto anterior
 * ({@code com.barboya.barboya}) sigue en el repositorio como referencia, pero
 * queda fuera: sus rutas no comprobaban permisos. Cada modulo se incorpora a
 * esta lista cuando se reescribe y tiene pruebas.
 */
@SpringBootApplication(
        scanBasePackages = {
                "com.barboya.addresses",
                "com.barboya.admin",
                "com.barboya.audit",
                "com.barboya.auth",
                "com.barboya.businesses",
                "com.barboya.common",
                "com.barboya.config",
                "com.barboya.couriers",
                "com.barboya.files",
                "com.barboya.finance",
                "com.barboya.notifications",
                "com.barboya.orders",
                "com.barboya.products",
                "com.barboya.ratings",
                "com.barboya.realtime",
                "com.barboya.security",
                "com.barboya.support",
                "com.barboya.users",
                "com.barboya.web"
        },
        exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan("com.barboya.config")
@EntityScan({"com.barboya.addresses", "com.barboya.audit",
        "com.barboya.auth", "com.barboya.businesses", "com.barboya.couriers",
        "com.barboya.finance", "com.barboya.orders", "com.barboya.products", "com.barboya.ratings",
        "com.barboya.support",
        "com.barboya.users"})
@EnableJpaRepositories({"com.barboya.addresses", "com.barboya.audit",
        "com.barboya.auth", "com.barboya.businesses", "com.barboya.couriers",
        "com.barboya.finance", "com.barboya.orders", "com.barboya.products", "com.barboya.ratings",
        "com.barboya.support",
        "com.barboya.users"})
public class BarboyaApplication {

    public static void main(String[] args) {
        SpringApplication.run(BarboyaApplication.class, args);
    }
}
