# 🇨🇺 Qvacell

> Tu cell en Cuba. Cambiamos la letra, mantenemos el sonido.

[![Plataforma](https://img.shields.io/badge/plataforma-Android%208.0%2B-blue.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/kotlin-2.0%2B-orange.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM%202024.09-blue.svg)](https://developer.android.com/jetpack/compose)
![PRs Bienvenidos](https://img.shields.io/badge/PRs-bienvenidos-brightgreen)
[![Sincronización iOS](https://github.com/albertolicea00/Qvacell-apk/actions/workflows/cross-platform-sync-check.yml/badge.svg)](https://github.com/albertolicea00/Qvacell-apk/actions/workflows/cross-platform-sync-check.yml)

Una aplicación para Android para acceder rápidamente a los **códigos de servicio USSD de ETECSA (Cubacel)**: consulta tu saldo, compra paquetes de datos/voz/SMS, transfiere saldo y más — todo desde una lista limpia y organizada que envía el código directamente al marcador del sistema.

[Read English version](README.md)

## ⚠️ Descargo de Responsabilidad

> [!WARNING]
> Esta es una aplicación independiente hecha por la comunidad. **No está afiliada, respaldada ni patrocinada por ETECSA**.
> Los códigos pueden cambiar en cualquier momento a discreción del operador.

## ✨ Características

- 📋 **Catálogo USSD Completo** — Organizado por categorías (Saldo y Planes, Compras, Líneas de Ayuda, Utilidades) con solicitudes interactivas según entradas necesarias.
- 📞 **Marcado en Un Tap** — Abre el marcador del sistema mediante `ACTION_DIAL`, exigiendo la misma confirmación explícita del usuario que `tel://` en iOS.
- 👤 **Integración con Contactos** — Lee los contactos del dispositivo para llamar, transferir saldo o realizar llamadas a cobro revertido `*99` directamente.
- 🛜 **Directorio de Salas de Navegación y Wi-Fi** — Búsqueda offline de salas de navegación de ETECSA y puntos Wi-Fi públicos por provincia.
- ✉️ **Catálogo de Servicios SMS** — Examina y prellena consultas de servicios por SMS (noticias, tiempo, deportes, tarifas de servicios) sin envío silencioso.
- 👥 **Gestión de Cuentas y PIN** — Almacena el PIN de transferencia en un almacén local cifrado y gestiona números del Plan Amigo.
- 🔔 **Recordatorios Locales** — Programa alertas recurrentes para compras de planes, recargas de saldo o transferencias, con marcado en 1 toque, reprogramadas automáticamente tras reiniciar el dispositivo.
- 🔍 **Búsqueda Offline en Directorio** — Búsqueda inversa por número sobre una base de datos SQLite suministrada por el usuario (oculta por defecto, ver abajo).
- 🌗 **Personalización y Options** — Soporte para tema Claro/Oscuro, color de acento personalizado y pestaña de inicio configurable.

> [!NOTE]
> **App Nativa de Android**
> Esta aplicación está construida de forma nativa para Android con el fin de aprovechar funciones exclusivas de la plataforma (como servicios en segundo plano e integración con SMS) que no se podían replicar completamente en iOS. Por esta razón, existe como un repositorio nativo dedicado e independiente.

## 🛠️ Requisitos y Compatibilidad

- 🤖 Android Studio (última versión estable para desarrollo)
- 📱 **Versiones de Android compatibles:**
  - **Mínimo requerido:** Android 8.0 Oreo (API 26)
  - **Objetivo actual:** Android 14 (API 34)
  - **Probado y compatible:** Android 8.0, 9 (Pie), 10 (Q), 11, 12, 13, 14 y 15.
  - **Capas de personalización compatibles:** One UI (Samsung), HyperOS / MIUI (Xiaomi/Redmi), Android AOSP / Stock, entre otros.
- 🔑 **Firma de APK:** Compatible con esquemas V1 (JAR) y V2 (Full APK Signature) para asegurar instalación en dispositivos antiguos y ROMs de fabricantes.
- 💻 Kotlin 2.0+, Jetpack Compose

## 📦 Variantes de Compilación (Flavors) y Permisos

| Flavor | Distribución | Paridad `versionCode` | Permisos sensibles | Auto-actualización del dashboard |
| --- | --- | --- | --- | --- |
| **`store`** | Google Play | **Par** (`base * 2`) | Ninguno de los tres siguientes | Desactivada — solo marcado manual, igual que cualquier otro código de la app |
| **`unlocked`** | GitHub / APK manual | **Impar** (`base * 2 + 1`) | `RECEIVE_SMS`, `READ_SMS`, `READ_CALL_LOG` (más `CALL_PHONE`) | Activada (interruptores opcionales en Opciones) — captura en segundo plano |

> [!TIP]
> **Estrategia de Paridad de Versiones (`versionCode` Par/Impar)**:
> Ambos flavors comparten el mismo `applicationId` (`com.qvacell.app`). Para garantizar que un usuario pueda descargar la versión completa (`unlocked`) de GitHub e **instalarla directamente encima** de la versión de Google Play sin desinstalar ni perder datos:
> - La versión `store` recibe un número par (ej. base 3 $\rightarrow$ code 6).
> - La versión `unlocked` recibe el número impar inmediatamente superior (ej. base 3 $\rightarrow$ code 7).
> Al ser $7 > 6$, el sistema Android lo reconoce como una actualización válida. Además, nunca existirá colisión de códigos entre versiones futuras.

`store` existe específicamente para que la versión de Play Store nunca tenga que pasar la revisión de permisos restringidos de Google para `READ_SMS`/`READ_CALL_LOG` — se compila del mismo código exacto, solo que sin los source sets de captura (`app/src/store/`) que sí tiene `unlocked` (`app/src/unlocked/`). Ver [ARCHITECTURE.md § 15](ARCHITECTURE.md#15-dashboard-dynamic-data-pipeline) para cómo funciona la separación y qué hace la capa de captura de cada flavor.

**Por qué existe cada permiso** (solo `unlocked` — `store` no pide ninguno de estos):

| Permiso | Por qué |
| --- | --- |
| `CALL_PHONE` | Ya existía para el flujo de confirmación de compra en Compras (`ACTION_CALL`, marca directo una vez el usuario acepta la hoja de confirmación). `unlocked` también lo reutiliza para captura silenciosa de USSD (`TelephonyManager.sendUssdRequest`) — mismo permiso, uso más amplio, por eso ese camino es opcional detrás de una explicación en vez de reutilizar el permiso ya concedido sin avisar. |
| `READ_SMS` | Escaneo único del historial de SMS al instalar, para que una instalación nueva pueda rellenar el dashboard con mensajes de ETECSA que el usuario ya recibió, no solo los que lleguen a partir de ese momento. |
| `RECEIVE_SMS` | Captura en vivo de nuevos SMS entrantes de ETECSA (saldo, descuentos, fechas límite) a medida que llegan, vía un receiver — la mitad de "mantener el dashboard actualizado hacia adelante" de la captura de SMS. |
| `READ_CALL_LOG` | Alimenta el motor de estimación en segundo plano: cuenta/duración de llamadas a números cubanos desde la última lectura confirmada, para estimar el consumo entre sincronizaciones reales por USSD/SMS. |

Ninguno de estos se pide al instalar — cada uno se solicita en tiempo de ejecución, justo antes de la función que lo necesita (interruptores en Opciones, ambos apagados por defecto), con una explicación dentro de la app de por qué.

**Compilar y correr:**

```bash
# Builds de debug
./gradlew :app:assembleStoreDebug       # build seguro para Play Store, sin permisos sensibles
./gradlew :app:assembleUnlockedDebug    # build completo con el pipeline de captura del dashboard

# Instalar directo en un dispositivo/emulador conectado
./gradlew :app:installStoreDebug
./gradlew :app:installUnlockedDebug

# Builds de release (sin firmar a menos que haya un signing config configurado)
./gradlew :app:assembleStoreRelease
./gradlew :app:assembleUnlockedRelease

# Correr la suite de tests de un flavor específico
./gradlew :app:testStoreDebugUnitTest
./gradlew :app:testUnlockedDebugUnitTest
```

En Android Studio, usa el panel **Build Variants** (esquina inferior izquierda) para cambiar entre `storeDebug`/`unlockedDebug`/etc. antes de correr ▶️.

## 🚀 Primeros Pasos

```bash
git clone https://github.com/albertolicea00/qvacell-apk.git
cd qvacell-apk
```

Abre el proyecto en Android Studio y espera la sincronización — el Gradle wrapper se regenera automáticamente al abrirlo por primera vez. Compila y ejecuta en un dispositivo o emulador.

**El marcado USSD requiere un dispositivo físico con una SIM de Cubacel** 📲 — el emulador no tiene una pila de telefonía real y no puede realizar llamadas.

## 🗂️ Estructura del Proyecto

```
app/src/main/java/com/qvacell/app/
├── QvacellApplication.kt        # Punto de entrada de la app, canales de notificación
├── MainActivity.kt              # Host de Activity única, aplica el tema, solicita permiso de notificaciones
├── model/                       # Catálogo USSDCode/Category, CubanPhoneNumber, Reminder
├── data/                        # Base de datos Room, DAOs, DataStore de preferencias/opciones
├── service/                     # DialService, ContactsRepository, TransferPinStore,
│                                 # ReminderRepository/Scheduler, DirectoryDatabase
├── receiver/                    # Receivers de alarma/acción de recordatorios, reprogramación al reiniciar
└── ui/                          # Pantallas Compose, navegación, componentes compartidos

app/src/main/assets/
├── codes.json                   # Catálogo de códigos USSD incluido
└── wifi_navigation_rooms.json   # Directorio de salas de navegación/puntos Wi-Fi de ETECSA incluido
```

_El catálogo completo de códigos USSD se carga desde [`codes.json`](app/src/main/assets/codes.json)._ 📁

## ☎️ Marcado Directo vs. Confirmación

Los códigos de consulta gratuitos se marcan inmediatamente. Los códigos de compra de pago se detienen en el menú de confirmación de ETECSA por defecto; la opción **Acción sin Confirmación** sustituye el código por una variante que autoconfirma, con una advertencia visible en la interfaz.

## 📊 Firebase

La app usa Firebase Analytics para medir primeras aperturas, sesiones y vistas de pantallas (incluidos los dashboards Manual y Dinámico), Crashlytics para reportes de fallos y Performance Monitoring para tiempos de arranque y carga de contactos. Los reportes incluyen la variante `store` o `unlocked`. No se envían nombres de contactos, números telefónicos, PIN, códigos USSD ni respuestas USSD como eventos personalizados. Performance no mide directamente CPU, memoria ni batería.

Para compilar, registra la app Android `com.qvacell.app` en un proyecto Firebase con Analytics habilitado y coloca el archivo descargado en `app/google-services.json`. Git ignora este archivo; consérvalo localmente y comprueba que su clave de API esté restringida a las API de Firebase que usa la app. Ambas variantes comparten esa configuración. Consulta **Analytics › DebugView**, **Crashlytics** y **Performance** en la consola Firebase para validar los reportes. La métrica `first_open` cuenta la primera apertura tras instalar o reinstalar; las descargas de Google Play se consultan en Play Console.

## 🔍 Directorio Telefónico y Base de Datos Offline

En **Opciones › Utilidades**:

- **Buscar en Database**: Búsqueda inversa offline sobre una base de datos SQLite (`.db`) suministrada por el usuario, importada mediante el selector de archivos del sistema. Por seguridad y privacidad, esta función viene oculta por defecto (se desbloquea tocando 5 veces la versión en _Acerca de_) y la búsqueda es estrictamente solo por número (sin búsqueda por nombre).

## 📶 Red LTE (4G) en Android

En **Opciones › Utilidades › Forzar red a solo LTE (4G)**, Qvacell ofrece un acceso directo al menú de información de radio de Android. La app no cambia la red por sí misma: el usuario debe elegir manualmente **LTE Only / LTE_ONLY** en ese menú. El acceso puede estar bloqueado o no existir según la marca y el modelo.

Antes de cambiar el modo, anota el tipo de red preferida para poder restaurarlo. Algunos teléfonos muestran opciones como **LTE/TD-SCDMA/UMTS** o **LTE/TD-SCDMA/WCDMA**. El modo **Solo LTE** puede impedir recibir y realizar llamadas y enviar SMS; restablece el modo original al terminar.

No confundas este menú de Android con el **Field Test Mode** del iPhone: al marcar `*3001#12345#*` se muestran estadísticas técnicas de señal en tiempo real (por ejemplo, RSRP, bandas y frecuencias), pero no permite bloquear bandas ni seleccionar LTE Only. En Android, `*#*#4636#*#*` puede abrir información de teléfono/red en algunos modelos, y algunos Samsung ofrecen su propio menú de selección de bandas con `*#2263#`; la disponibilidad depende del fabricante y del dispositivo.

## 🛜 Salas de Navegación y Wi-Fi Público

Incluye un directorio offline de salas de navegación oficiales de ETECSA y puntos Wi-Fi públicos por provincia.

## 🔄 Sincronización entre Plataformas

[`cross-platform-sync-check.yml`](.github/workflows/cross-platform-sync-check.yml) se ejecuta en cada push a `main` que modifique `codes.json` o `wifi_navigation_rooms.json`, y compara la copia de este repo contra la de [qvacell-ios](https://github.com/albertolicea00/Qvacell-ios). Si han divergido, abre (o actualiza) un issue en el _otro_ repositorio para que se actualice la plataforma que quedó atrás. Solo se compara la **estructura** en `codes.json` (ids, cadenas de marcado, tipo de acción, manejo de entrada, ubicación en categoría/grupo) — los campos cosméticos (icono, precio, redacción del título, etc.) pueden diferir entre plataformas. Ver [ARCHITECTURE.md § 14](ARCHITECTURE.md#14-cross-platform-catalog-sync-check) para el detalle exacto de qué se compara y cómo.

## 🚧 Limitaciones Conocidas

- **La telemetría de Firebase puede estar limitada desde Cuba (22 de octubre de 2026).** Qvacell está dirigida a usuarios cubanos. En una prueba con LlamaCon99, otra app Android que usa los mismos servicios de Firebase, Analytics envió correctamente el evento sin VPN (HTTP 204), mientras que Crashlytics y Performance Monitoring recibieron HTTP 403. Con VPN, ambos servicios aceptaron sus datos de prueba (HTTP 200). Por tanto, pueden faltar informes de errores y rendimiento de usuarios que se conecten desde Cuba sin VPN. La prueba corresponde a un Samsung SM-A155M y una conexión concreta; queda pendiente verificar este comportamiento específicamente en Qvacell y en otras redes cubanas.
- **Es posible que el número telefónico de la SIM no se detecte automáticamente.** La mayoría de las tarjetas SIM (especialmente Cubacel / ETECSA) no vienen con el número de teléfono (MSISDN) grabado directamente en el chip por la operadora. En tales casos, las APIs de telefonía de Android devuelven `null` o vacío, por lo que la aplicación recurre a mostrar el nombre de la operadora o la ranura (ej. `SIM 1` / `SIM 2`).
- **Los marcadores de operador/fabricante pueden interceptar los códigos USSD** antes de que el intent `ACTION_DIAL` de esta app llegue al módem, según el dispositivo y la ROM. Este es un comportamiento de la plataforma/fabricante de Android fuera del control de la app.
- **Puede aparecer un aviso USSD del sistema si se pierde la señal durante una consulta del dashboard.** Qvacell detiene las consultas restantes y muestra su propio estado, pero la API pública `sendUssdRequest` de Android no permite cancelar una petición ya enviada a telefonía ni cerrar un aviso que muestre el sistema o el operador. Activar el modo avión durante una consulta reproduce esta limitación.

## 🤝 Contribuir

Consulta [CONTRIBUTING.md](CONTRIBUTING.md). Por favor, sigue el [Código de Conducta](CODE_OF_CONDUCT.md).

> ⚠️ **Los Issues, descripciones de PR y mensajes de commit deben escribirse en inglés.**
> La interfaz de la app está intencionalmente en español (está dirigida a usuarios cubanos). Toda la comunicación técnica sigue las convenciones en inglés.

## 📚 Fuentes

Los códigos se compilaron a partir de los siguientes sitios:

- https://galixpay.com/recargas-a-cuba/
- https://www.fonoma.com/blog/codigos-ussd-cuba
- https://www.etecsa.cu/es/taxonomy/term/1445
- https://www.etecsa.cu/en/rooms-public-spaces
- https://www.etecsa.cu/es/empresas/movil-combos
- https://www.ecured.cu/Entumovil
- https://www.escambray.cu/2017/etecsa-informa-sobre-nuevos-servicios-de-telefonia-movil-para-clientes-prepago-infografia/
- https://www.entumovil.cu/#:~:text=Para%20activar%20las%20siguientes%20prestaciones%2C,portal%20el%20de%20su%20preferencia.

---

_Desarrollado por @albertolicea00 — port a Android de [qvacell-ios](https://github.com/albertolicea00/qvacell-ios)._
