# 🇨🇺 Qvacell

> Tu cell en Cuba. Cambiamos la letra, mantenemos el sonido.

[![Plataforma](https://img.shields.io/badge/plataforma-Android%208.0%2B-blue.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/kotlin-2.0%2B-orange.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM%202024.09-blue.svg)](https://developer.android.com/jetpack/compose)
[![Licencia](https://img.shields.io/badge/licencia-MIT-green.svg)](LICENSE)
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
- 🆔 **Identificador de Llamadas (`*99`)** — Un `CallScreeningService` desenvuelve el número de cobro revertido de ETECSA y muestra el nombre real del contacto mediante una notificación emergente. Ver [Limitaciones Conocidas](#-limitaciones-conocidas) — Android no permite que una app que no sea el marcador predeterminado inyecte un nombre en la interfaz de llamada del sistema como sí lo hace CallKit en iOS.
- 🛜 **Directorio de Salas de Navegación y Wi-Fi** — Búsqueda offline de salas de navegación de ETECSA y puntos Wi-Fi públicos por provincia.
- ✉️ **Catálogo de Servicios SMS** — Examina y prellena consultas de servicios por SMS (noticias, tiempo, deportes, tarifas de servicios) sin envío silencioso.
- 👥 **Gestión de Cuentas y PIN** — Almacena el PIN de transferencia en un almacén local cifrado y gestiona números del Plan Amigo.
- 🔔 **Recordatorios Locales** — Programa alertas recurrentes para compras de planes, recargas de saldo o transferencias, con marcado en 1 toque, reprogramadas automáticamente tras reiniciar el dispositivo.
- 🔍 **Búsqueda Offline en Directorio** — Búsqueda inversa por número sobre una base de datos SQLite suministrada por el usuario (oculta por defecto, ver abajo).
- 🌗 **Personalización y Ajustes** — Soporte para tema Claro/Oscuro, color de acento personalizado y pestaña de inicio configurable.

> [!NOTE]
> **App Nativa de Android**
> Esta aplicación está construida de forma nativa para Android con el fin de aprovechar funciones exclusivas de la plataforma (como servicios en segundo plano, identificación de llamadas e integración con SMS) que no se podían replicar completamente en iOS. Por esta razón, existe como un repositorio nativo dedicado e independiente.

## 🛠️ Requisitos

- 🤖 Android Studio (última versión estable)
- 📱 Android 8.0+ (API 26)
- Kotlin 2.0+, Jetpack Compose

## 📦 Variantes de Compilación (Flavors) y Permisos

**Dos Versiones Disponibles**
Este proyecto utiliza "flavors" de compilación para ofrecer dos versiones distintas de la aplicación:
- **Versión de App Store**: Una versión restringida diseñada para cumplir con las estrictas políticas de la tienda respecto a permisos sensibles (como SMS y Accesibilidad).
- **Versión Independiente (Instalación Manual)**: La experiencia completa y sin restricciones. Esta versión utiliza permisos de SMS y Accesibilidad para leer automáticamente los mensajes del operador y las respuestas USSD en segundo plano. Esto permite mantener el panel (dashboard) de saldo y datos actualizado casi en tiempo real.

## 🚀 Primeros Pasos

```bash
git clone https://github.com/albertolicea00/qvacell-apk.git
cd qvacell-apk
```

Abre el proyecto en Android Studio y espera la sincronización — el Gradle wrapper se regenera automáticamente al abrirlo por primera vez. Compila y ejecuta en un dispositivo o emulador.

**El marcado USSD requiere un dispositivo físico con una SIM de Cubacel** 📲 — el emulador no tiene una pila de telefonía real y no puede realizar llamadas.

Para activar el Identificador de Llamadas para llamadas `*99`, abre **Ajustes › Acerca de › Identificador de Llamadas** en la app y concede el rol de selección de llamadas cuando se solicite (`RoleManager.ROLE_CALL_SCREENING`). Este es un ajuste manual de Android que se realiza una sola vez — ninguna app puede activarlo automáticamente. Consulta [ARCHITECTURE.md § 11](ARCHITECTURE.md#11-caller-id-callscreeningservice-99-collect-call-identification) para entender por qué el resultado es distinto al de la extensión CallKit de iOS.

## 🗂️ Estructura del Proyecto

```
app/src/main/java/com/qvacell/app/
├── QvacellApplication.kt        # Punto de entrada de la app, canales de notificación
├── MainActivity.kt              # Host de Activity única, aplica el tema, solicita permiso de notificaciones
├── model/                       # Catálogo USSDCode/Category, CubanPhoneNumber, Reminder, WrappedCaller
├── data/                        # Base de datos Room, DAOs, DataStore de ajustes
├── service/                     # DialService, ContactsRepository, TransferPinStore,
│                                 # ReminderRepository/Scheduler, DirectoryDatabase, CallerIdScreeningService
├── receiver/                    # Receivers de alarma/acción de recordatorios, reprogramación al reiniciar
└── ui/                          # Pantallas Compose, navegación, componentes compartidos

app/src/main/assets/
├── codes.json                   # Catálogo de códigos USSD incluido (compartido con la app iOS)
└── wifi_navigation_rooms.json   # Directorio de salas de navegación/puntos Wi-Fi de ETECSA incluido
```

_El catálogo completo de códigos USSD se carga desde [`codes.json`](app/src/main/assets/codes.json), el mismo archivo que usa la app iOS, manteniendo ambas plataformas sincronizadas._ 📁

## ☎️ Marcado Directo vs. Confirmación

Los códigos de consulta gratuitos se marcan inmediatamente. Los códigos de compra de pago se detienen en el menú de confirmación de ETECSA por defecto; la opción **Acción sin Confirmación** sustituye el código por una variante que autoconfirma, con una advertencia visible en la interfaz.

## 🔍 Directorio Telefónico y Base de Datos Offline

En **Ajustes › Utilidades**:

- **Buscar en Database**: Búsqueda inversa offline sobre una base de datos SQLite (`.db`) suministrada por el usuario, importada mediante el selector de archivos del sistema. Por seguridad y privacidad, esta función viene oculta por defecto (se desbloquea tocando 5 veces la versión en _Acerca de_) y la búsqueda es estrictamente solo por número (sin búsqueda por nombre).

## 🛜 Salas de Navegación y Wi-Fi Público

Incluye un directorio offline de salas de navegación oficiales de ETECSA y puntos Wi-Fi públicos por provincia, empaquetado igual que en la app iOS.

## 🔄 Sincronización entre Plataformas

[`cross-platform-sync-check.yml`](.github/workflows/cross-platform-sync-check.yml) se ejecuta en cada push a `main` que modifique `codes.json` o `wifi_navigation_rooms.json`, y compara la copia de este repo contra la de [qvacell-ios](https://github.com/albertolicea00/Qvacell-ios). Si han divergido, abre (o actualiza) un issue en el _otro_ repositorio para que se actualice la plataforma que quedó atrás. Solo se compara la **estructura** en `codes.json` (ids, cadenas de marcado, tipo de acción, manejo de entrada, ubicación en categoría/grupo) — los campos cosméticos (icono, precio, redacción del título, etc.) pueden diferir entre plataformas. Ver [ARCHITECTURE.md § 14](ARCHITECTURE.md#14-cross-platform-catalog-sync-check) para el detalle exacto de qué se compara y cómo.

## 🚧 Limitaciones Conocidas

- **El Identificador de Llamadas no puede mostrar un nombre personalizado en la interfaz del sistema.** A diferencia de la extensión CallKit Call Directory de iOS, la API `CallScreeningService` de Android no permite que una app que no sea el marcador predeterminado inyecte un nombre en la pantalla de llamada entrante del propio sistema. Esta app en su lugar muestra el nombre resuelto mediante una notificación emergente cuando suena una llamada `*99` envuelta. Convertir la app en el marcador predeterminado del usuario para lograr la inyección completa del nombre se descartó deliberadamente — es un compromiso mucho mayor (reemplazar la interfaz principal del teléfono) por una sola función.
- **La base de datos offline no está integrada con el Identificador de Llamadas (`*99`).** Mismo razonamiento que en la app iOS: la base de datos importada por el usuario (`Buscar en Database`) se mantiene separada de la tabla de búsqueda de números envueltos, que solo se carga desde los Contactos propios del dispositivo.
- **Sin seguimiento de saldo en tiempo real.** Ninguna de las dos plataformas puede leer la respuesta USSD que muestra el propio marcador del operador — marcar un código transfiere la ejecución al marcador del sistema, donde el usuario ve la respuesta del operador directamente.
- **Los marcadores de operador/fabricante pueden interceptar los códigos USSD** antes de que el intent `ACTION_DIAL` de esta app llegue al módem, según el dispositivo y la ROM. Este es un comportamiento de la plataforma/fabricante de Android fuera del control de la app.

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
- https://www.ecured.cu/Entumovil
- https://www.escambray.cu/2017/etecsa-informa-sobre-nuevos-servicios-de-telefonia-movil-para-clientes-prepago-infografia/
- https://www.entumovil.cu/#:~:text=Para%20activar%20las%20siguientes%20prestaciones%2C,portal%20el%20de%20su%20preferencia.

---

_Desarrollado por @albertolicea00 — port a Android de [qvacell-ios](https://github.com/albertolicea00/qvacell-ios)._
