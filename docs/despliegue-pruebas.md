# Publicar la versión nueva en un entorno de pruebas

Publica la versión nueva (Spring Boot e interfaz `v1`) en un subdominio aparte, con Coolify. **La aplicación actual no se toca:** es otro recurso, con su propia base de datos.

> **Solo para pruebas.** No uses documentos ni dinero reales hasta tener la revisión legal y contable pendiente.

## 1. Antes de empezar

- Un servidor con Coolify y el repositorio conectado.
- Un subdominio apuntando al servidor, por ejemplo `pruebas.tudominio.co`.
- **HTTPS es obligatorio:** sin él no funcionan la sesión ni la ubicación del celular.
- Al menos 2 GB de memoria libre para construir la imagen.

## 2. Generar los secretos

Genera **tres valores distintos**. En Git Bash o Linux:

```bash
openssl rand -hex 32       # DB_PASSWORD
openssl rand -hex 32       # JWT_SECRET
openssl rand -base64 32    # FILES_KEY (debe ser base64 de 32 bytes)
```

En PowerShell, cada ejecución produce un valor válido para cualquiera de las tres:

```powershell
$b = New-Object byte[] 32
[Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b)
[Convert]::ToBase64String($b)
```

Guárdalos en un gestor de contraseñas. **Si pierdes `FILES_KEY`, los documentos subidos no se pueden recuperar.**

## 3. Crear el recurso en Coolify

1. **+ New** → **Application** → **Docker Compose**. Elige el repositorio y la rama `main`.
2. **Docker Compose Location:** `/docker-compose.pruebas.yml`. **Base Directory:** `/`.
3. Pulsa **Load Compose File**: deben aparecer los servicios `db` y `app`.
4. En el servicio `app`, asigna el dominio `https://pruebas.tudominio.co` con puerto interno **8081**.

## 4. Variables de entorno

| Variable | Obligatoria | Valor |
|---|---|---|
| `DB_PASSWORD` | Sí | Secreto generado |
| `JWT_SECRET` | Sí | Secreto generado (32 caracteres o más) |
| `FILES_KEY` | Sí | 32 bytes en base64 |
| `BOOTSTRAP_ADMIN_EMAIL` | Primer despliegue | Tu correo |
| `BOOTSTRAP_ADMIN_PHONE` | Primer despliegue | Un celular de 10 dígitos |
| `BOOTSTRAP_ADMIN_PASSWORD` | Primer despliegue | 12 caracteres o más, con letras y números |
| `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`, `MAIL_FROM` | No | Datos SMTP. Exige TLS (STARTTLS), normalmente puerto `587` |
| `DELIVERY_BASE_FEE_COP`, `DELIVERY_PER_KM_COP`, `DELIVERY_MAX_KM` | No | `3000`, `1000`, `8` |
| `MERCHANT_COMMISSION_BPS`, `COURIER_COMMISSION_BPS`, `DEBT_LIMIT_COP` | No | `1000` (10 %), `1000`, `50000` |
| `LEGAL_OPERATOR_NAME`, `_ID`, `_ADDRESS`, `_PHONE`, `_EMAIL` | Antes de usar datos reales | Quien opera; sin ellos los documentos muestran «PENDIENTE» |
| `LEGAL_REVIEWED` | No | `true` solo cuando un abogado apruebe los documentos; quita el aviso de borrador |

Sin las variables de correo la aplicación funciona, pero la verificación de correo y la recuperación de contraseña responden «no disponible».

## 5. Desplegar y comprobar

1. Pulsa **Deploy**. La primera construcción tarda varios minutos. La base de datos se crea sola.
2. Comprueba el despliegue desde tu computador (Git Bash):

   ```bash
   sh scripts/verificar-despliegue.sh https://pruebas.tudominio.co
   ```

   No crea ni modifica datos. Debe terminar con `Todo en orden`.
3. Ingresa con el superadministrador.
4. **Borra `BOOTSTRAP_ADMIN_PASSWORD`** de Coolify y vuelve a desplegar.

## 6. Prueba con celulares reales

Usa datos inventados y, si puedes, tres celulares.

- [ ] Registrar un restaurante, un domiciliario y un cliente.
- [ ] El comercio completa sus datos, marca su ubicación **estando en el local** y crea productos.
- [ ] El domiciliario sube imágenes de prueba; el administrador las revisa y aprueba ambas cuentas.
- [ ] El comercio empieza a recibir pedidos y el domiciliario se conecta.
- [ ] El cliente pide con domiciliario usando su ubicación y revisa la tarifa.
- [ ] Recorrer la entrega: oferta, recogida con código, foto, PIN.
- [ ] Anotar la distancia real recorrida y compararla con la calculada (línea recta).
- [ ] Bloquear la pantalla del domiciliario y anotar cuánto tarda en dejar de verse su posición.
- [ ] Revisar las comisiones del comercio y del domiciliario, y registrar un pago de prueba.
- [ ] Dejar un pedido sin responder 15 minutos y confirmar que se cancela solo.
- [ ] Abrir un caso desde un pedido, responderlo y resolverlo como administrador.
- [ ] Registrar una entrega fallida y revisar el caso que se abre solo.
- [ ] Calificar una entrega y ocultar la opinión desde la administración.
- [ ] Subir fotos de productos **tomadas con el celular** (verticales y horizontales) y revisar que se vean derechas.
- [ ] Definir un horario y comprobar que fuera de él no se puede pedir.
- [ ] Crear una promoción de un día y confirmar que al siguiente vuelve el precio normal.
- [ ] Guardar una dirección estando en casa y usarla en otro pedido.
- [ ] Revisar los documentos legales, registrarse aceptándolos y eliminar una cuenta de prueba.
- [ ] Dejar vencer el plazo de entrega de un pedido y cancelarlo desde el cliente.

## 7. Problemas frecuentes

| Síntoma | Causa probable |
|---|---|
| `app` no arranca | Falta `JWT_SECRET` o `FILES_KEY`, o no tienen el formato pedido. Revisa los registros |
| El ingreso responde 403 | El dominio que ve la aplicación no coincide con el de la barra del navegador. Corre el comprobador |
| No pide permiso de ubicación | El sitio no está en HTTPS |
| El mapa sale en blanco | El celular no alcanza `tile.openstreetmap.org` |
| No llegan correos | Datos SMTP incorrectos o el proveedor no admite STARTTLS |

## 8. Copias de seguridad y límites

- Respalda juntos los volúmenes `pruebas_db` y `pruebas_files`, y guarda `FILES_KEY` aparte.
- Ejecuta **una sola instancia** de `app`: los avisos en vivo y el límite de intentos viven en memoria.
- Para actualizar, vuelve a desplegar: las migraciones se aplican al arrancar.
- El mapa usa las teselas públicas de OpenStreetMap, aptas solo para poco tráfico.
