# BarboYa: revisión legal en Colombia

Fecha de la revisión: 27 de noviembre de 2026. Alcance: la versión nueva (Spring Boot y React, sin publicar) y la aplicación que hoy está en funcionamiento (FastAPI).

> **Esto no es un concepto de abogado.** Lo preparó un asistente de inteligencia artificial, sin tarjeta profesional ni responsabilidad como apoderado. Sirve para saber qué revisar y qué corregir. Antes de operar con personas reales, un abogado y un contador deben validarlo. No se puede garantizar que no falte algo.

**Cómo leer las marcas**

| Marca | Significa |
|---|---|
| **[Texto]** | Leído en el texto de la norma en una compilación pública (no en el Diario Oficial) |
| **[Secundaria]** | Tomado de prensa, firmas o blogs; confirmar en la norma |
| **[Por confirmar]** | Conocimiento general no verificado en esta revisión |

## 1. Conclusión

**Hoy no se debe operar con usuarios reales**, ni con la versión nueva ni con ampliaciones de la actual. Los obstáculos principales:

| # | Riesgo | Gravedad |
|---|---|---|
| 1 | La app en funcionamiento ofrece **viajes en moto** (mototaxismo) | Muy alta |
| 2 | No hay términos, política de datos ni autorización válida | Muy alta |
| 3 | Faltan obligaciones de comercio electrónico y de portal de contacto | Alta |
| 4 | Domiciliarios sin marco contractual ni preparación para la Ley 2466 | Alta |
| 5 | Farmacias, alcohol y tabaco sin controles ni verificación de edad | Alta |
| 6 | Pagos con criptoactivos en la app actual | Alta |
| 7 | Empresa, facturación e impuestos sin definir | Alta |

Referencia de multas: el Estatuto del Consumidor (art. 61) y la ley de datos (art. 23) llegan a **2.000 salarios mínimos** (con el salario mínimo de 2026, $1.750.905, unos $3.500 millones). El consumidor además prevé cierre del sitio web y multas personales a quienes autoricen las conductas. **[Texto]**

## 1-bis. Cambios aplicados después de esta revisión

Hechos en el código; **entran en servicio solo cuando subas los cambios y se despliegue**. Las secciones siguientes describen cómo estaba el código antes.

| Cambio | Alcance |
|---|---|
| Viajes, envíos y recargas con cripto **suspendidos** (`ENABLE_TRIPS`, `ENABLE_CRYPTO_PAYMENTS`) | App actual. Los viajes en curso pueden terminarse; los pagos cripto ya reportados se siguen confirmando |
| Selfie opcional; casilla de términos sin marcar; retiradas cifras sin respaldo («34 verificados», «Soporte 24/7») y la oferta de transporte | App actual |
| Autorización de datos, términos y mayoría de edad con prueba; documentos publicados como **borrador** | Versión nueva |
| Identificación obligatoria del comercio, visible al comprador; enlace a la SIC; aviso de impuestos y retracto; acuse de recibo por correo | Versión nueva |
| Farmacia y registro de mototaxistas y conductores apagados | Versión nueva |
| Plazo máximo de entrega informado antes de comprar y cancelación por retraso (art. 50 c y h) | Versión nueva |
| Fecha límite en cada reclamo (15 días hábiles; 10 en datos personales) y categoría de datos personales | Versión nueva; sin alertas automáticas |
| Eliminación de cuenta por su titular, conservando la prueba de las operaciones | Versión nueva |

**Sigue pendiente:** datos del responsable y revisión de los documentos por un abogado; plazos de conservación y borrado automático; devolución de dinero cuando existan pagos en línea; fuentes alojadas en el propio servidor; todo lo de domiciliarios; y en la app actual, términos y política publicados, autorización registrada y las cuentas y datos ya recogidos sin esa autorización.

## 2. Lo urgente: la aplicación que está funcionando

| Hallazgo en el código | Problema | Qué hacer |
|---|---|---|
| Viajes en moto y automóvil particular (`trips`, tipo de vehículo `moto`) | El Estatuto de Transporte solo admite servicio público habilitado o transporte privado propio; el Gobierno considera ilegal el servicio por plataforma en vehículo particular y el mototaxismo **[Secundaria]**. La Supertransporte ha investigado plataformas por facilitar la infracción **[Secundaria]**. El conductor se expone a comparendo (Ley 769, art. 131 D.12) e inmovilización **[Secundaria]** | **Suspender los viajes** hasta tener concepto jurídico |
| Pagos con USDC para recargar el saldo del conductor | Colombia no tiene ley integral de criptoactivos y existen obligaciones de reporte ante la DIAN para quienes prestan servicios con ellos **[Secundaria]** | **Desactivar** hasta tener concepto |
| Saldo prepagado del conductor para cobrar comisiones | Es un anticipo recibido por la plataforma: requiere condiciones, reembolso y factura. Si algún día guarda dinero **de terceros**, cambian las reglas (ver 8) | Definir condiciones por escrito |
| Casilla «Acepto los términos y la política de privacidad» **marcada de antemano** (`AuthDashboard.tsx`), sin que existan esos documentos | La autorización debe ser previa, expresa e informada, y el silencio no cuenta (Ley 1581, art. 9; Decreto 1377, art. 7) **[Texto]** | No marcarla por defecto y publicar los documentos |
| Selfie de referencia y verificación facial periódica de conductores (`selfie_reference_url`, `face_check_due`) | Dato biométrico sensible: exige autorización explícita e informada y no condicionar sin base (ver 4). No encontré ningún registro de esa autorización en el código | Suspender la exigencia o implementar autorización, política y plazo de borrado, con concepto |
| La app se presenta con «mensajería» | La mensajería expresa de paquetes de hasta 5 kg exige habilitación de MinTIC (Ley 1369 de 2009) **[Secundaria]** | No ofrecer envío de paquetes sin concepto |

## 3. Consumidor y comercio electrónico (Ley 1480 de 2011, reformada por la Ley 2439 de 2024)

BarboYa es un **portal de contacto**: conecta comercios con compradores (art. 5, numeral 18, y art. 53). Los comercios son proveedores; la plataforma también asume deberes propios.

| Norma **[Texto]** | Exige | Estado en la versión nueva | Falta |
|---|---|---|---|
| Art. 53 | Registro de cada oferente con **nombre o razón social, documento, dirección de notificaciones y teléfonos**, consultable por el comprador y la autoridad | Razón social y NIT opcionales | Hacerlos obligatorios, pedir documento y dirección de notificación, y mostrarlos |
| Art. 50 a) | Identidad del proveedor: nombre, NIT, dirección judicial, teléfono, correo | Solo nombre y dirección; el teléfono aparece tras pedir | Ficha del comercio y de BarboYa |
| Art. 50 b) | Información suficiente del producto; disponibilidad y validez de la oferta | Descripción opcional | Exigir descripción; ingredientes y alérgenos según el producto |
| Art. 50 c) | Antes de comprar: medios de pago, **tiempo de entrega**, **retracto**, precio total con impuestos | Muestra pago y total | Tiempo de entrega, retracto y «precio con impuestos incluidos» |
| Art. 50 d) | Condiciones generales publicadas y descargables; **resumen imprimible**; aceptación expresa; **acuse de recibo a más tardar al día siguiente**; el silencio no obliga | Resumen en pantalla y botón de confirmación | Términos, resumen descargable y correo de confirmación |
| Art. 50 e) | Conservar prueba de la transacción | Pedidos e historial no se pueden borrar | Definir plazo de conservación **[Por confirmar]** |
| Art. 50 f) | Seguridad; responde por fallas | Controles implementados, sin auditoría externa | Revisión de seguridad antes de pagos en línea |
| Art. 50 g) | Canal de PQR **con radicado, fecha, hora y seguimiento** | Casos de soporte con número, fechas y estado | Tipificar PQR, mostrar plazo y permitir reclamar sin cuenta |
| Art. 50 h) | Entregar en el plazo aceptado (o máximo 30 días); si se incumple, el cliente puede **terminar** y recibir su dinero en 15 días | No se informa plazo; el cliente no puede cancelar un pedido aceptado | Informar plazo y permitir cancelar si se vence |
| Art. 50, parágrafo | **Enlace visible a la SIC** | No existe | Agregar |
| Art. 52 | Verificar la edad; con menores, autorización de los padres | Nada | Declaración de mayoría de edad y controles en productos restringidos |
| Art. 26 | Precio en pesos con todo incluido; ante dos precios se cobra el menor | Pesos; se cobra el rebajado | Indicar impuestos incluidos |
| Arts. 29 y 30 | Lo anunciado obliga; prohibida la publicidad engañosa | La app no comprueba que el precio tachado se haya cobrado antes | Guardar historial de precios y regla en términos |
| Art. 33 | La promoción obliga; informar condiciones y vigencia | **Corregido en esta revisión** (ver abajo) | Revisar el capítulo de promociones de la Circular Única de la SIC (Título II, actualizado en enero de 2026) **[Secundaria]** |
| Art. 31 | Advertencias en publicidad de productos nocivos | Nada | Prohibir o advertir (ver 7) |
| Arts. 37, 42 y 43 | Condiciones claras; son ineficaces las que limiten responsabilidad, hagan renunciar derechos, invierta la carga de la prueba, nieguen reintegros, etc. | Sin términos aún | Redactarlos respetando el art. 43 |
| Art. 47 | **Retracto**: 5 días hábiles en ventas a distancia; devolución máximo en 15 días. No aplica, entre otros, a perecederos, personalizados o de uso personal | No se informa ni se tramita | Informarlo por tipo de producto: la comida preparada suele estar exceptuada, un abarrote no perecedero no |
| Art. 51 | **Reversión del pago** electrónico (fraude, no recibido, defectuoso) en 5 días hábiles | No aplica: solo efectivo | Obligatorio antes de activar pagos en línea |
| Art. 58-5 | Responder la reclamación directa en **15 días hábiles** | Sin control de plazos | Fecha límite y alerta en cada caso |

**Corrección hecha:** la función de promociones permitía quitar una rebaja antes de su fecha final. Ahora una promoción vigente no se puede quitar, acortar ni subir, tampoco subir el precio normal mientras dure, y se avisa cuando depende de existencias. Queda un hueco: el comercio puede marcar el producto como no disponible; los términos deben prohibir usarlo para esquivar la promoción.

## 4. Datos personales (Ley 1581 de 2012 y Decreto 1377 de 2013, compilado en el Decreto 1074 de 2015) **[Texto]**

| Obligación | Estado | Falta |
|---|---|---|
| Autorización previa, expresa e informada, **con prueba** (arts. 9 y 12; Dec. arts. 5, 7 y 8) | El registro nuevo **no la pide** | Casilla sin marcar, versión aceptada, fecha y registro |
| Informar finalidad, derechos y quién es el responsable (art. 12) | No | Aviso de privacidad |
| Política de tratamiento: responsable, finalidades, derechos, encargado de reclamos, procedimiento, vigencia (Dec. art. 13) | No existe | Redactar y publicar |
| **Datos sensibles** (foto del rostro, biometría): autorización explícita, informar que no está obligado, y **no condicionar actividades** a entregarlos (arts. 5 y 6; Dec. art. 6) | La foto del rostro es **obligatoria** para ser domiciliario; el texto de autorización es incompleto | **Concepto jurídico**: justificarla o dar una alternativa de verificación; ampliar el texto |
| Menores: tratamiento prohibido salvo excepciones (art. 7; Dec. art. 12) | No se pregunta la edad | Exigir mayoría de edad |
| Consultas en **10 días hábiles** y reclamos en **15** (prorrogables 5 y 8) (arts. 14 y 15) | Sin canal propio | Canal de habeas data con plazos |
| Rectificar, suprimir y revocar (art. 8; Dec. art. 9) | Se borran direcciones y documentos; **no la cuenta** | Eliminación o anonimización de cuenta, respetando lo que deba conservarse |
| Conservar solo lo necesario (Dec. art. 11) | Fotos de entrega, ubicaciones de pedidos e IP sin plazo | Tabla de retención y borrado automático |
| Persona o área encargada (Dec. art. 23) | Sin designar | Designar |
| Responsabilidad demostrada y manual interno (art. 17-k; Dec. arts. 26 y 27) | No | Manual y registros |
| Reportar incidentes de seguridad a la SIC (art. 17-n) | Sin procedimiento | Procedimiento; el plazo de 15 días hábiles es **[Secundaria]** |
| Transmisión y transferencia internacional (art. 26; Dec. arts. 24 y 25) | Depende de dónde quede el servidor. Las fuentes de Google y los mapas de OpenStreetMap reciben la IP del usuario | Contrato con el proveedor de alojamiento, informarlo y alojar las fuentes en el propio servidor |
| Registro Nacional de Bases de Datos | Probablemente no aplica: umbral de 100.000 UVT en activos **[Secundaria]** | Confirmar según la empresa |

Nunca guardar datos personales en una cadena de bloques: no se podrían borrar.

## 5. Domiciliarios

### 5.1. Ley 2466 de 2025 (reforma laboral), arts. 24 a 30 **[Texto]**

- Admite dos modalidades: dependiente o independiente. Manda la **realidad**: si hay subordinación, hay contrato laboral aunque el papel diga otra cosa.
- Independientes: la plataforma aporta **60 %** de salud y pensión y el trabajador **40 %**; los **riesgos laborales** los paga solo la empresa; base de cotización del **40 %** de lo recibido. **Sin exclusividad** y con derecho al descanso.
- Informar modalidad y cambios de condiciones en lenguaje sencillo; identificar plenamente al trabajador y registrar los términos.
- Entregar cada mes un **reporte de horas de servicio efectivo**.
- Inscribirse ante MinTrabajo y reportar **cada trimestre** los trabajadores activos.
- **Explicar los algoritmos** que asignan pedidos, fijan ingresos o suspenden cuentas; ofrecer **revisión humana**; no suspender cuentas ni retener pagos sin justificación; no discriminar.
- **Vigencia (art. 67):** estos artículos rigen **12 meses después** de que el Gobierno cumpla su reglamentación.

### 5.2. Reglamentación

- **Decreto 991 del 4 de agosto de 2026** (aportes de repartidores independientes) **[Texto parcial]**: confirma 60/40 y riesgos 100 % a cargo de la plataforma, que además **retiene y paga** por PILA el 40 % del repartidor. Las obligaciones de cotización serían exigibles 12 meses después de que MinSalud certifique los ajustes de la PILA; las fuentes se contradicen en detalles y se anunció un proyecto para modificarlo **[Secundaria]**.
- El **registro** ante MinTrabajo seguía sin reglamentar; un tribunal ordenó hacerlo en septiembre de 2026 **[Secundaria]**.
- Que algo no sea exigible todavía **no elimina el riesgo laboral actual** por subordinación.

### 5.3. Qué choca con la app

| Punto | Riesgo o falta |
|---|---|
| Comisión, tope de deuda, filtro de ofertas por distancia y suspensión | Deben explicarse y permitir revisión humana |
| Solo se guarda la última ubicación | No permite el reporte de horas: habrá que registrar tiempos de servicio (con su autorización) |
| El domiciliario **paga los productos** al comercio y asume el riesgo | Pedir concepto: puede discutirse como carga indebida o indicio contractual **[Por confirmar]** |
| Entrega fallida: el dinero se arregla fuera de la app | Definir quién asume la pérdida |
| No se pide fecha de nacimiento | Exigir mayoría de edad |
| Sin contrato ni términos del domiciliario | Redactar antes de vincular a alguien |

### 5.4. Tránsito

- Licencia, SOAT y **revisión técnico-mecánica** vigentes; hoy la app trata la revisión como opcional. Exigirla cuando corresponda según la edad de la moto **[Por confirmar]**.
- Casco, y chaleco o prenda reflectiva entre las 6 p. m. y las 6 a. m. (Ley 769, arts. 94 y 96) **[Secundaria]**. La carga no puede restar visibilidad ni generar peligro.
- **Barbosa (Santander)** restringió motos las noches de fin de semana del 18 de septiembre al 18 de octubre de 2026, con excepción para domiciliarios **formalmente registrados**, y anunció controles al mototaxismo **[Secundaria]**. Consultar en la alcaldía los decretos vigentes y cómo se acredita el registro.

## 6. Transporte de pasajeros (Move) y paquetes

- **No activar Move** en moto ni en vehículo particular. Un proyecto del Gobierno para sancionar plataformas (PL 347 de 2026) fue retirado temporalmente y hay otro en la Cámara para regularlas (PL 430 de 2026); ninguno es ley, según las fuentes consultadas **[Secundaria]**.
- La única vía hoy sería intermediar para empresas de transporte **habilitadas**, con concepto previo.
- La versión nueva permite registrarse como «Mototaxista» y «Conductor de automóvil». Recomiendo **quitar esas opciones** (necesito tu autorización).
- Envío de paquetes entre personas: requiere definir si es servicio postal (habilitación MinTIC; Resolución 5881 de 2025) **[Secundaria]**. No encontré un concepto sobre domicilios de comercios.

## 7. Productos regulados

| Producto | Regla | Estado y recomendación |
|---|---|---|
| Alimentos | Buenas prácticas y transporte (Resolución 2674 de 2013): sin contacto con el piso, aislados de otros productos, temperaturas y vehículo identificado **[Secundaria]** | Exigir concepto sanitario al comercio; definir maletas térmicas y si aplica el rótulo a motos **[Por confirmar]** |
| Medicamentos | Venta a distancia solo por droguerías o farmacias, que responden aunque otro reparta; fórmula cuando se requiera (Resolución 1896 de 2023, art. 23) **[Secundaria]** | Existe la categoría «Farmacia» **sin ningún control**: deshabilitarla hasta tener concepto |
| Bebidas alcohólicas | Prohibido vender a menores (Ley 124 de 1994); posibles horarios y leyes secas locales **[Secundaria]** | Prohibirlas hasta verificar edad en la entrega |
| Tabaco y vapeadores | Prohibida la venta directa e indirecta a menores (Ley 1335 de 2009, reformada por la Ley 2354 de 2024) **[Secundaria]** | Prohibirlos |
| Armas, pólvora, sustancias controladas, animales y otros | **[Por confirmar]** | Lista de productos prohibidos y moderación por el administrador |

## 8. Pagos, dinero y blockchain

- **Efectivo** entre las partes: es lo menos regulado. La plataforma debe facturar su comisión.
- **Recibir dinero de los comercios** (pago en línea con dispersión): hacerlo mediante una pasarela o agregador autorizado. Actuar como agregador implica el Decreto 1692 de 2020, con registro ante la Superfinanciera y fondos en entidades vigiladas **[Secundaria]**. Captar dinero del público está reservado a entidades vigiladas **[Por confirmar]**.
- **Billetera de clientes**: no implementarla sin concepto.
- **Contratos inteligentes**: un depósito en garantía implica custodiar valor y convertir pesos en activos digitales. Solo en red de pruebas hasta tener concepto.
- **Prevención de lavado de activos** (SAGRILAFT): aplica a sociedades sobre ciertos topes **[Por confirmar]**.

## 9. Empresa e impuestos (para el contador)

- Definir si opera una **persona natural o una sociedad**; registro mercantil, RUT, actividades económicas y matrícula en Cámara de Comercio **[Por confirmar]**.
- **Factura electrónica** de las comisiones; documento soporte de pagos a quienes no facturan **[Por confirmar]**.
- **IVA** sobre la comisión: en principio un servicio gravado al 19 % **[Por confirmar]**; una persona natural puede no ser responsable por debajo de 3.500 UVT de ingresos gravados **[Secundaria]**.
- **Industria y comercio** del municipio, retenciones y reporte de información exógena, incluido el régimen para operadores de plataformas digitales **[Secundaria]**.
- Aclarar quién presta el servicio de domicilio (BarboYa, el comercio o el domiciliario): cambia impuestos, factura y responsabilidad.
- Los comercios facturan sus propias ventas; los términos deben decirlo.

## 10. Otros temas

| Tema | Qué hacer |
|---|---|
| Mensajes comerciales y cobros (Ley 2300 de 2023) | Canales autorizados y horarios (sin domingos ni festivos; sábados hasta las 3 p. m.) **[Secundaria]**. Aplica a publicidad y al cobro de comisiones atrasadas |
| Calificaciones | Ya se moderan con motivo y auditoría. Publicar reglas y canal para quien se sienta afectado en su buen nombre |
| Marca «BarboYa» | Buscar antecedentes y registrarla ante la SIC **[Por confirmar]** |
| Fotos de comercios | Autorización de uso y garantía de que son suyas; moderación |
| Mapas y fuentes | Mantener la atribución a OpenStreetMap; su servidor público no es para uso comercial intensivo: contratar un proveedor |
| Código abierto | Revisar licencias antes de publicar **[Por confirmar]** |
| Prueba electrónica (Ley 527 de 1999) | Conservar aceptación, versión de términos, fecha y origen |
| Delitos informáticos (Ley 1273 de 2009) y seguridad | Procedimiento de incidentes y denuncia **[Por confirmar]** |
| Botón de emergencia | No afirmar que avisa a autoridades (ya corregido) |
| Seguros | Evaluar responsabilidad civil, accidentes y carga **[Por confirmar]** |

## 11. Documentos por redactar

1. Términos para clientes (incluye retracto, entregas, cancelaciones, PQR y SIC).
2. Términos para comercios (registro del art. 53, promociones, productos prohibidos, sanidad, facturación, comisiones).
3. Términos o contrato de domiciliarios (modalidad, algoritmos, revisión humana, aportes, sin exclusividad).
4. Política de tratamiento de datos y avisos de privacidad (registro, domiciliarios, ubicación, rostro).
5. Reglas de calificaciones y de productos.
6. Contrato de transmisión de datos con el proveedor de alojamiento.
7. Manual interno: incidentes, retención, reclamos.

Puedo preparar **borradores** para que un abogado los revise. No se deben publicar con datos inventados.

## 12. Plan de acción

**Decisiones tuyas (primero)**
1. ¿Suspendes viajes y pagos cripto en la app actual?
2. ¿Quién opera BarboYa? Nombre o razón social, documento o NIT, dirección de notificaciones, teléfono, correo y municipio.
3. ¿Quién atiende reclamos y datos personales?
4. ¿Se quitan Farmacia y las opciones de conductor mientras tanto?

**Lo que puedo programar**
1. Aceptación de términos y autorización de datos con versión y prueba.
2. Registro obligatorio de comercios (art. 53) y ficha pública; enlace a la SIC.
3. Antes de confirmar: plazo de entrega, retracto e impuestos; resumen descargable y correo de confirmación; cancelación por retraso.
4. Mayoría de edad; productos prohibidos y moderación.
5. PQR y habeas data con fechas límite; eliminación de cuenta; borrado por plazos; fuentes propias.
6. Domiciliarios: horas de servicio, explicación de la asignación, solicitud de revisión, revisión técnico-mecánica.

**Con profesionales**
- Abogado: modalidad laboral, datos biométricos, transporte, términos y municipio.
- Contador: forma jurídica, IVA, facturación, ICA y reportes.

## 13. Límites de esta revisión

- Los sitios del Senado, Función Pública y Cancillería no respondieron. Las leyes marcadas **[Texto]** se leyeron en la compilación de Colpensiones, que no es el Diario Oficial.
- No se revisó jurisprudencia, conceptos completos de la SIC, DIAN o MinTransporte, ni decretos municipales.
- No se analizaron en detalle: tributación, laboral colectivo, competencia, accesibilidad, propiedad intelectual ni seguros.
- Las normas cambian: la Ley 2466 y su reglamentación están en movimiento. Repetir la revisión antes de lanzar.

## Fuentes

- [Ley 1480 de 2011, compilación de Colpensiones](https://normativa.colpensiones.gov.co/colpens/compilacion/docs/ley_1480_2011.htm)
- [Ley 1581 de 2012](https://normativa.colpensiones.gov.co/colpens/compilacion/docs/ley_1581_2012.htm) y [Decreto 1377 de 2013](https://normativa.colpensiones.gov.co/colpens/compilacion/docs/decreto_1377_2013.htm)
- [Ley 2466 de 2025](https://normativa.colpensiones.gov.co/colpens/compilacion/docs/ley_2466_2025.htm) y [Decreto 991 de 2026](https://normativa.colpensiones.gov.co/colpens/compilacion/docs/decreto_0991_2026.htm)
- [Ley 2439 de 2024, análisis](https://www.estradalawfirm.com.co/el-comercio-electronico-y-el-derecho-de-retracto-en-el-estatuto-del-consumidor-tras-la-ley-2439-de-2024-deberes-de-informacion-del-proveedor-resumen-del-pedido-plazo-de-entrega-devolucion-del-dine/)
- [Circular Única de la SIC](https://sic.gov.co/circular-unica)
- [Protección de datos 2026, Deloitte](https://www.deloitte.com/latam/es/services/legal/perspectives/co-proteccion-de-datos-2026.html)
- [Orden de reglamentar el registro de plataformas](https://www.radiohoy.com/2026/09/17/tribunal-ordeno-reglamentar-registro-de-empresas-de-domicilios-por-aplicacion/817943/)
- [Restricción de motos en Barbosa](https://www.vanguardia.com/santander/region/2026/10/17/barbosa-termina-la-restriccion-nocturna-para-motos-213-fueron-inmovilizadas-en-un-mes/)
- [Supertransporte e investigaciones a plataformas](https://mintransporte.gov.co/publicaciones/11027/supertransporte-sanciona-a-beat-didi-y-cabify-por-facilitar-el-incumplimiento-de-las-normas-del-sector-transporte-a-los-prestadores-de-servicios-que-no-cuentan-con-las-autorizaciones-correspondientes/)
- [Proyecto 430 de 2026, Cámara](https://www.camara.gov.co/wp-content/uploads/2026/10/proyectos-ley/documentos/proyecto-36512/P.L.430-2026SC-.pdf) y [retiro del proyecto 347](https://zonacero.com/generales/gobierno-retira-proyecto-que-sancionaba-conductores-y-usuarios-de-plataformas-de)
- [Código de Tránsito, art. 96](https://leyes.co/codigo_nacional_de_transito_terrestre/96.htm)
- [Mensajería expresa, MinTIC](https://mintic.gov.co/portal/inicio/Oferta-Institucional/Servicios-postales/79804:Mensajeria-Expresa)
- [Resolución 1896 de 2023, MinSalud](https://www.minsalud.gov.co/sites/rid/Lists/BibliotecaDigital/RIDE/DE/DIJ/resolucion-1896-de-2023.pdf) y [Resolución 2674 de 2013](https://minsalud.gov.co/sites/rid/Lists/BibliotecaDigital/RIDE/DE/DIJ/resolucion-2674-de-2013.pdf)
- [Ley 124 de 1994](https://normograma.invima.gov.co/compilacion/docs/ley_0124_1994.htm) y [Ley 2354 de 2024](https://www.asuntoslegales.com.co/actualidad/lo-que-debe-saber-acerca-de-la-sancion-a-la-ley-2354-que-regula-los-vapeadores-3862279)
- [Decreto 1692 de 2020](https://www.alcaldiabogota.gov.co/sisjur/normas/Norma1.jsp?i=104425)
- [Criptoactivos: mercado y regulación pendiente](https://www.semana.com/economia/articulo/criptoactivos-en-colombia-el-mercado-crece-pero-la-regulacion-sigue-pendiente/202654/)
- [Ley 2300 de 2023](https://normativa.colpensiones.gov.co/colpens/compilacion/docs/ley_2300_2023.htm)
- [Topes de IVA 2026](https://actualicese.com/montos-para-ser-no-responsables-de-iva-en-2026/) y [concepto DIAN sobre operadores de plataformas](https://normograma.dian.gov.co/dian/compilacion/docs/oficio_dian_4538_2026.htm)
