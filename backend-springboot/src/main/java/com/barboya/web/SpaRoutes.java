package com.barboya.web;

/**
 * Pantallas de la interfaz. El servidor responde con index.html y React decide
 * que mostrar. Al agregar una ruta en el frontend, agregarla tambien aqui.
 */
public final class SpaRoutes {

    private SpaRoutes() {
    }

    public static final String[] PAGES = {
            "/", "/ingresar", "/registro", "/recuperar", "/seguridad",
            "/comercios", "/comercios/*", "/pedidos", "/pedidos/*",
            "/recibidos", "/productos", "/mi-comercio",
            "/verificacion", "/entregas", "/comisiones",
            "/admin/usuarios", "/admin/finanzas", "/admin/auditoria", "/admin/equipo"
    };

    public static final String[] FILES = {"/index.html", "/assets/**", "/favicon.ico"};
}
