# Auditoría técnica inicial — CIM v6.0

**Fecha:** 28 de julio de 2026  
**Alcance revisado:** configuración Gradle, código Kotlin Android, red TCP/BLE, protocolo CIM, manifiestos, pruebas y firmware ESP32.  
**Limitación de ejecución:** el entorno no contiene JDK (`JAVA_HOME` no está definido y `java` no existe), por lo que no fue posible ejecutar Gradle ni las pruebas automatizadas. Se realizaron comprobaciones estáticas y estructurales.

> El archivo de escenarios indicado por el usuario no estuvo disponible dentro del sistema de archivos del agente durante esta ejecución. Este informe debe complementarse con la trazabilidad escenario → prueba cuando el contenido esté accesible.

## Correcciones aplicadas

### Bloqueantes de compilación

1. **Uso inválido de `Log.e` en bloques `catch`.**
   Un cambio masivo había convertido bloques `catch` a la forma inválida `Log.e(... ) { ... }` en el coordinador, manufactura y `core-network`. Se restauraron los bloques válidos, conservando el log y la acción original.
2. **Importaciones faltantes de `android.util.Log`.**
   Se añadieron donde el código restaurado lo necesitaba.
3. **Código residual inválido en `TcpServer.kt`.**
   Al final del archivo existían funciones no utilizadas que referenciaban `CimProtocol.PASSWORD_HASH`, `clientSockets` y `clientThreads`, símbolos inexistentes. Se retiró ese bloque obsoleto; la implementación activa del servidor finaliza en la clase `TcpServer`.
4. **Importaciones duplicadas masivas.**
   Se eliminaron importaciones idénticas repetidas, especialmente `withTimeout`, `Date`, `SimpleDateFormat`, `Before` y `After`. Esto reduce conflictos/ruido de compilación.
5. **Paquete inconsistente en `AppControl.kt`.**
   Se alineó a `com.industria.plc`, que corresponde al `namespace` y a la ruta del módulo PLC.
6. **Pruebas instrumentadas con package name antiguo.**
   Se actualizaron las aserciones para `com.industria.calidad`, `com.industria.plc` y `com.industria.coordinacion`.

## Riesgos y defectos pendientes priorizados

### Críticos

| Hallazgo | Evidencia | Impacto / recomendación |
|---|---|---|
| Credencial CIM embebida y compartida | `CimProtocol.PASSWORD_ACTUAL = "UBB_CIM_PRO_SECURE_2024"` | Cualquiera que extraiga una APK puede autenticarse. Sustituir por aprovisionamiento por equipo, secreto fuera del binario y autenticación mutua; no enviar ni comparar contraseña en texto plano. |
| Firmado release con contraseña expuesta y keystore inexistente | `app-coordinador` y `app-almacen` incluyen `cimkeystorepass`; no hay `release.keystore` en el repositorio | Un `assembleRelease` fallará por falta del archivo; si se crea con esos valores, la clave queda comprometida. Usar propiedades de entorno/archivo local ignorado y configurar CI segura. |
| Tráfico TCP sin cifrado y cleartext habilitado | Manifiestos de Coordinador y Almacén usan `usesCleartextTraffic="true"`; TCP en puerto 8888 | Contraseña, comandos de movimiento y estado son interceptables/modificables en la red. Usar TLS con validación de certificado o una red industrial aislada con controles equivalentes. |
| Autorización ligada a MAC declarada por el cliente | `TcpServer` usa `cim.sourceMac` y `handleMacMapping` | Una estación maliciosa puede suplantar otra MAC y, con `PREFER_NEW`, cerrar la sesión legítima. No usar MAC enviada por payload como identidad; asociar una identidad criptográfica al canal autenticado. |

### Altos

| Hallazgo | Evidencia | Impacto / recomendación |
|---|---|---|
| Identificadores de estación incompatibles | `CimProtocol.STATION_UUIDS` usa `CIM-ST-…-X*`, mientras las apps usan `CIM-ALM-01`, `CIM-MAN-02`, `CIM-CAL-03`, `CIM-PLC-04` | Registro, autorización y correlación de escenarios pueden fallar según el camino usado. Definir una única fuente de verdad y usarla en todas las estaciones, pruebas y firmware. |
| Dos familias de firmware en paralelo y respuestas incompatibles | `3_FIRMWARE_ESP32/*.ino` y `esp32/firmware/*.ino` | No está claro cuál se debe flashear. Sus nombres BLE y respuestas difieren (`ACK`, `ACTUATOR`, `UNKNOWN`, etc.), lo que rompe integración. Declarar una rama/carpeta canónica y pruebas de compatibilidad del protocolo. |
| Firmware sin control industrial de seguridad | Los comandos BLE ejecutan simulaciones/relés sin autenticación, watchdog, parada de emergencia ni validación de parámetros | Riesgo físico si se conecta a actuadores reales. Implementar E-stop físico, estado seguro ante desconexión, límites, watchdog y validación estricta antes de energizar salidas. |
| Callbacks potencialmente concurridos | `TcpServer` y `TcpClient` invocan callbacks desde `Dispatchers.IO` | La UI/estado Compose puede actualizarse fuera del hilo principal. Exponer `StateFlow` y actualizar UI mediante `viewModelScope`/Main. |

### Medios

| Hallazgo | Evidencia | Impacto / recomendación |
|---|---|---|
| Backups habilitados | `android:allowBackup="true"` en todas las apps | Puede incluir configuración, logs o datos operativos. Deshabilitar en producción o excluir datos sensibles explícitamente. |
| Permisos amplios y obsoletos | `WRITE_EXTERNAL_STORAGE`, `READ_EXTERNAL_STORAGE`, ubicación de fondo y permisos Bluetooth sin límites de SDK en varios manifiestos | Aumenta superficie de privacidad y puede dificultar aprobación/ejecución en Android moderno. Solicitar sólo permisos requeridos y definir `maxSdkVersion` cuando corresponda. |
| Pruebas no cubren todo el producto | Sólo hay pruebas unitarias para core, PLC, Coordinador y una parte de Calidad; no para Almacén/Manufactura ni firmware real | Los flujos completos no están demostrados. Agregar pruebas de contrato TCP/BLE, integración y fallos de red por escenario. |
| Tarea `testAllModules` no ejecuta todas las pruebas | `config/build.gradle.kts` sólo depende de core, coordinador y PLC | Calidad, Manufactura y Almacén quedan fuera de la tarea declarada como global. Incluir los cinco módulos y separar instrumentadas de unitarias. |
| Validación de APK por tamaño rígido | `validateApks` exige 100–200 MB por APK | Un build legítimo más pequeño fallará. Validar presencia, firma, `applicationId`, versión y checksum; usar umbrales justificados si son realmente necesarios. |

## Validaciones realizadas

- `git diff --check`: sin errores de espacios.
- Exploración de símbolos residuales eliminados (`PASSWORD_HASH`, `clientSockets`, `clientThreads`): sin referencias Kotlin activas.
- Revisión estructural de llaves en fuentes Kotlin modificadas: sin llaves desbalanceadas detectadas.
- Gradle/pruebas: **no ejecutables** hasta instalar/configurar JDK 17 y definir `JAVA_HOME`.

## Siguiente paso recomendado

1. Proveer el contenido de los escenarios y convertirlos a una matriz: escenario, precondiciones, pasos, mensaje/protocolo esperado, criterio de aceptación y prueba automatizada.
2. Ejecutar desde `config/` con JDK 17: `./gradlew testAllModules` y `./gradlew buildAllApks`.
3. Resolver primero autenticación/cifrado, configuración de firmado y unificación de identificadores/protocolo antes de ensayos con hardware real.
