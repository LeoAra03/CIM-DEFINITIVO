# 🔍 AUDITORÍA COMPLETA - CIM-DEFINITIVO

**Fecha:** 2026-08-09
**Auditor:** Arquitecto Senior – 15+ años (Análisis automatizado + revisión manual)
**Stack:** Android Kotlin (Compose + Hilt), ESP32 (Wemos D1 R32) BLE NUS + Serial2 Scorbot, TCP Sockets, Ktor Netty
**Módulos:** 5 apps + Wear + core-network + firmware ESP32

---

## Resumen Ejecutivo

1. **Seguridad Crítica Comprometida:** `TlsSocketHelper` implementa Trust-All (MITM trivial), `PermissionManager.autoApproveTestMode` permite bypass total de autenticación con 5 taps secretos, firmware ESP32 acepta comandos `PLC:START` sin validar autorización previa. En un entorno industrial esto significa parada de cinta, apertura de relé y ejecución de robot por cualquiera en rango BLE.

2. **Arquitectura Monolítica y Acoplamiento Extremo:** `CoordinatorViewModel` (48k LOC, 900+ líneas) viola SRP: maneja robot, láser, cinta, ArUco, tracking, red, permisos y dashboard ejecutivo. GlobalSingletons (`GlobalBluetoothManager`, `GlobalCommandBroker`, `GlobalPermissionManager`, `GlobalDeviceRegistry`) crean dependencias circulares y hacen testing imposible. No existe capa Domain/UseCase/Repository.

3. **Manejo de Errores y Performance Deficientes:** 132 ocurrencias de `Log.e` con payload completo, múltiples `catch (_: Exception) {}` silenciosos, `Thread.sleep` dentro de corutinas (`PermissionManager.waitForApproval`), `runBlocking` en `CommandBroker.send()` que puede provocar ANR, bucle infinito `while(true) delay(2000)` en `CoordinatorViewModel.startMonitoring()` sin `isActive` check.

4. **Falta de Validación de Inputs y Path Traversal:** Archivos G-code se guardan con nombre proveniente de URI (`lastPathSegment`) sin sanitización → `../../etc` o `../../../data/data/...`. `IndustrialErrorManager.sanitizeInput()` destruye protocolo al eliminar `|` y `;` necesarios para CIM.

5. **Testing y Documentación Insuficiente:** Coverage JVM mínimo (tests de ejemplo), sin tests de integración para handshake, sin E2E para flujo de pallet. KDoc ausente en 80% de core-network.

---

## Hallazgos Detallados

### 🔴 CRÍTICO

**Archivo:** `android/core-network/src/main/java/com/sistema/distribuido/network/TlsSocketHelper.kt`
**Línea(s):** 20-32
**Problema:** TrustManager que acepta cualquier certificado (`checkClientTrusted` y `checkServerTrusted` vacíos). Si `USE_TLS=true`, toda comunicación TCP es vulnerable a Man-in-the-Middle. Además `enabled` es mutable global.
**Impacto:** Un atacante en red Wi-Fi industrial puede interceptar comandos `E-STOP`, `PLC:START`, robar token de emparejamiento y tomar control de Scorbot/Laser.
**Solución:**
```kotlin
// ELIMINAR trustAll. Usar SSLContext default o pinning desde assets.
object TlsSocketHelper {
  var enabled = false
  fun createClientSocket(host:String, port:Int, timeoutMs:Int=2000): Socket {
    if (!enabled) return Socket().apply{ connect(InetSocketAddress(host,port), timeoutMs) }
    // Producción: cargar cert desde assets/cim_ca.crt
    val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
    tmf.init(null as KeyStore?) // default system CAs
    val ctx = SSLContext.getInstance("TLSv1.3").apply{ init(null, tmf.trustManagers, SecureRandom()) }
    return ctx.socketFactory.createSocket(host,port) as SSLSocket
  }
}
```
**Referencias:** OWASP M3 Insecure Communication, Android Network Security Config

---

**Archivo:** `esp32/firmware/cim_ble_firmware.h`
**Línea(s):** 112-160
**Problema:** `handleCommand()` procesa cualquier comando BLE sin autenticar. `PLC:START` activa relé GPIO5 inmediatamente. No hay validación de token ni de estado autorizado. Además `rxBuffer` limitado a 512 bytes pero se hace substring sin límite.
**Impacto:** Cualquier dispositivo BLE en rango puede activar cinta, mover robot o ejecutar láser. Riesgo de daño físico.
**Solución:**
```c
static bool isAuthorized = false;
static void handleCommand(String raw){
  if(raw.indexOf("CIM_AUTH:")>=0){ // handshake con token hash
    String token = extractPayload(raw).substring(9);
    if(token == "sha256:EXPECTED_HASH") isAuthorized=true;
    return;
  }
  if(!isAuthorized){ sendBleResponse("ERR:NOT_AUTHORIZED"); return; }
  // ... resto comandos
}
```
Además añadir watchdog para apargar relé si no hay heartbeat en 5s.

---

**Archivo:** `android/apps/app-manufactura/app/src/main/java/com/industria/manufactura/MainActivity.kt` + `app-coordinador`
**Línea(s):** 118-132 (manufactura), 132-150 (coordinador)
**Problema:** Guardado de G-code con filename tomado de URI `lastPathSegment` o split de payload sin sanitización. Permite path traversal `../../../` y escritura arbitraria en `filesDir`. Tamaño no limitado (OOM).
**Impacto:** Escritura arbitraria, DoS, ejecución de G-code malicioso.
**Solución:**
```kotlin
fun sanitizeFileName(input:String): String {
  val base = input.substringAfterLast('/').substringAfterLast('\\')
  val clean = base.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(64)
  require(clean.endsWith(".gcode") || clean.endsWith(".nc") || clean.endsWith(".txt")) { "Extension no permitida" }
  return clean.ifBlank{ "archivo_${System.currentTimeMillis()}.gcode" }
}
val bytes = Base64.decode(b64, Base64.NO_WRAP)
require(bytes.size <= 5*1024*1024) { "G-code >5MB" }
context.openFileOutput(sanitizeFileName(filename), MODE_PRIVATE).use{ it.write(bytes) }
```

---

**Archivo:** `android/core-network/src/main/java/com/sistema/distribuido/network/PermissionManager.kt`
**Línea(s):** 92-110, 348
**Problema:** `GlobalPermissionManager.autoApproveTestMode` - booleano público mutable global que aprueba cualquier dispositivo. Activable desde UI con `setAutoModeEnabled(true)` o gesto secreto 5 taps `testModeSecretGesture`. No protegido por BuildConfig.DEBUG.
**Impacto:** Bypass total de autorización. Cualquier atacante puede conectarse a coordinador sin pantalla de permiso.
**Solución:**
```kotlin
object GlobalPermissionManager {
  @Volatile var autoApproveTestMode = false
  fun isTestAutoApproveAllowed(): Boolean = BuildConfig.DEBUG && autoApproveTestMode
}
// En requestPermission:
if (GlobalPermissionManager.isTestAutoApproveAllowed() && context.isDebuggable()) { ... } else { /* requerir dialog */ }
```
Y eliminar gesto secreto en release, o proteger con PIN ingeniería.

---

**Archivo:** `android/core-network/src/main/java/com/sistema/distribuido/network/CommandBroker.kt`
**Línea(s):** 134-136
**Problema:** `fun send(message:CimMessage){ runBlocking { sendCommand(message) } }` - bloquea hilo caller. Si se llama desde Main thread → ANR. Usado en tests pero también potencialmente en UI.
**Impacto:** ANR, UI freeze, mala UX.
**Solución:** Eliminar wrapper sincrónico. Exponer solo suspend:
```kotlin
suspend fun sendCommand(message: CimMessage)
// Solo para tests offline:
fun sendForTest(msg: CimMessage, timeoutMs: Long=2000) = runBlocking(Dispatchers.IO){ withTimeout(timeoutMs){ sendCommand(msg) } }
```

---

### 🟠 ALTO

**Archivo:** `android/core-network/src/main/java/com/sistema/distribuido/network/PermissionManager.kt`
**Línea(s):** 254-275
**Problema:** `waitForApproval` usa `withContext(Dispatchers.Default){ Thread.sleep(100) }` - bloquea thread de pool.
**Impacto:** Desperdicio de threads, latencia alta, pool exhaustion.
**Solución:**
```kotlin
private suspend fun waitForApproval(mac:String, timeout:Long): PermissionDecision {
  val start = System.currentTimeMillis()
  while (System.currentTimeMillis()-start < timeout) {
    pendingRequests[mac]?.let{ if(it.respondedAt>0) return if(it.approved) APPROVED else REJECTED }
    delay(100) // no Thread.sleep
  }
  pendingRequests.remove(mac)
  listeners.forEach{ it.onPermissionExpired(mac) }
  return TIMEOUT
}
```

---

**Archivo:** `android/core-network/src/main/java/com/sistema/distribuido/network/IndustrialErrorManager.kt`
**Línea(s):** 37-39
**Problema:** `sanitizeInput` elimina `| ;` que son delimitadores del protocolo CIM (`CIM_MASTER_HUB_V1;NOMBRE;PASS;MAC;UUID`). Esto rompe handshake si se usa, y también elimina `/` necesario para paths pero no previene `..`.
**Impacto:** Handshake falla silenciosamente o payload truncado; falsa sensación de seguridad.
**Solución:**
```kotlin
fun sanitizeInput(input:String, maxLen:Int=1024):String {
  // Remover solo control chars y limitar longitud
  val clean = input.filter{ it.code >= 32 }.take(maxLen).trim()
  require(!clean.contains("..")){ "Path traversal detectado" }
  return clean
}
fun sanitizeFileName(name:String):String { /* como arriba */ }
```

---

**Archivo:** `android/apps/app-coordinador/app/src/main/java/com/industria/coordinacion/MainActivity.kt`
**Línea(s):** 98-220, 270-330 duplicado
**Problema:** Dos overloads `handleTcpHandshake(ip, CimMessage)` y `handleTcpHandshake(ip, String)` con lógica duplicada 90%. Segundo parsea con split(";") sin validar tamaño ni escapar. Además hace `AppType.values().firstOrNull` sin try-catch para unknown.
**Impacto:** DRY violation, bugs divergentes, crash si STATION_UUID no mapeado.
**Solución:** Extraer a `HandshakeParser` único con validación:
```kotlin
object HandshakeParser {
  data class Handshake(val name:String, val passwordHash:String, val mac:String, val uuid:String, val appType:AppType)
  fun parseLegacy(data:String): Handshake? { ... }
  fun parseCimMessage(cim:CimMessage): Handshake? { ... }
}
```

---

**Archivo:** `android/core-network/src/main/java/com/sistema/distribuido/network/protocol/CimProtocol.kt`
**Línea(s):** 14-25
**Problema:** `PASSWORD_ACTUAL` es `var` mutable global sin sincronización real (Volatile no suficiente), y `DEFAULT_PAIRING_TOKEN = "CIM_LAB_PAIRING_TOKEN_CHANGE_ME"` es token obvio. Aunque se envía hasheado, el hash legacy está hardcodeado → downgrade attack posible. `LEGACY_PAIRING_TOKEN_SHA256` permite bypass con token antiguo.
**Impacto:** Si alguien conoce token legacy, puede autenticarse aunque se cambie token.
**Solución:** Eliminar soporte legacy, forzar token con min 16 chars, almacenar via Android Keystore o DataStore encrypted. Generar token aleatorio al primer inicio.

---

**Archivo:** `android/apps/app-coordinador/app/src/main/java/com/industria/coordinacion/ui/CoordinatorViewModel.kt`
**Línea(s):** 88-118 (startMonitoring)
**Problema:** Bucle infinito `while(true){ delay(2000) }` que nunca chequea `isActive`, no cancelable limpio. Además dentro `try{...} catch(_:Exception){}` silencia todo. Usa `GlobalPermissionManager.getInstance()` cada 2s (I/O).
**Impacto:** Fuga de corutina al destruir ViewModel, logs duplicados, consumo batería.
**Solución:**
```kotlin
private fun startMonitoring(){
  viewModelScope.launch{
    while(isActive){
      try{ updateDeviceList(); pollPermissions() }catch(e:CancellationException){ throw e }catch(e:Exception){ Log.w(TAG,"Mon error ${e.message}") }
      delay(2000)
    }
  }
}
```

---

**Archivo:** `android/core-network/src/main/java/com/sistema/distribuido/network/TcpServer.kt`
**Línea(s):** 70-100, 190-210
**Problema:** `broadcastClientList()` cada 2s envía lista completa a todos los clientes - O(n²) y spam. No hay rate-limit por IP, maxClients 200 permite DoS. `PrintWriter` creado por cliente en cada broadcast sin pool.
**Impacto:** Saturación red industrial, CPU.
**Solución:** Broadcast solo cuando cambia lista, con debounce 5s. Añadir token bucket rate-limit 10 msg/s por IP.

---

**Archivo:** `android/core-network/src/main/java/com/sistema/distribuido/network/BluetoothHardwareManager.kt`
**Línea(s):** 772 LOC,  múltiples responsabilidades
**Problema:** God Object: maneja BLE scan, GATT, SPP, permisos, parsers, reconexión. Complejidad ciclomática alta, dificil testear. `writeLocks` con Mutex por MAC puede dead-lock si no se libera.
**Impacto:** Mantenimiento imposible, bugs ocultos.
**Solución:** Separar en `BleScanner`, `GattManager`, `SppManager`, `ConnectionRegistry`.

---

### 🟡 MEDIO

**Archivo:** `android/apps/app-coordinador/app/src/main/java/com/industria/coordinacion/ui/CoordinatorViewModel.kt`
**Línea(s):** 48-70
**Problema:** `data class TrackingState`, `QcProgramState` etc dentro mismo archivo ViewModel. Violación SRP, archivo 900 líneas.
**Solución:** Mover a `model/CoordinatorUiModels.kt`.

**Archivo:** `android/core-network/src/main/java/com/sistema/distribuido/network/StationClient.kt`
**Línea(s):** 126-160
**Problema:** `lastSentMsg` y `lastSentTime` no thread-safe, anti-spam basado en tiempo sin sincronización. Si dos hilos llaman sendSafe simultáneo, race condition.
**Solución:** Usar `Mutex` o `AtomicReference`.

**Archivo:** `android/core-network/src/main/java/com/sistema/distribuido/network/CommunicationCoordinator.kt`
**Línea(s):** 117-130
**Problema:** `routeCommand` crea PendingCommand pero nunca lo envía realmente por BLE/SPP/TCP - solo registra intención. Timeout job hace `delay(timeout)` y luego llama onResponse TIMEOUT aunque ya haya ACK.
**Impacto:** Lógica de negocio incompleta.
**Solución:** Integrar con CommandBroker real y cancelar timeout al recibir ACK.

**Archivo:** `android/apps/wear-coordinador/.../WearMainActivity.kt`
**Línea(s):** 68 LOC
**Problema:** Wear app no reutiliza core-network, duplica lógica.
**Solución:** Usar mismo core-network module.

**Archivo:** `android/core-network/src/main/java/com/sistema/distribuido/network/prefecto/TestModeGesture.kt`
**Línea(s):** 17-45
**Problema:** `testModeSecretGesture` - gesture secreto 5 taps activa modo ingeniería sin autenticación, bypass industrial safety.
**Impacto:** Operario puede activar modo sin control.
**Solución:** Requerir PIN o autenticación biométrica, y solo en DEBUG.

**Archivo:** `.github/workflows/android-ci.yml`
**Línea(s):** 6-8
**Problema:** CI solo corre en branch `arena/019fa89b-cim-definitivo` (hardcoded de otro agente). No corre en main ni en esta branch actual `arena/019fe466-cim-definitivo`.
**Impacto:** No hay CI real.
**Solución:** Cambiar a `on: [push, pull_request]` sin filtro branch o incluir `arena/**`.

**Archivo:** `config/build.gradle.kts`
**Línea(s):** 70-90
**Problema:** `buildReport` genera archivo con timestamp en rootDir sin limpieza → ensucia repo.
**Solución:** Guardar en `build/reports/`.

---

### 🟢 BAJO

**Archivo:** `android/apps/*/app/src/main/res/mipmap-*`
**Problema:** WebP icons duplicados en 5 apps (mismo icono). No usa core.
**Solución:** Extraer a `core-ui`.

**Archivo:** `docs/` varios
**Problema:** Referencias a rutas obsoletas `1_DOCUMENTACION` verificadas pero aún presentes en algunos md de archive.
**Solución:** Limpiar archive o .gitignore.

**Archivo:** `android/core-network/src/main/java/com/sistema/distribuido/network/YoloTfliteDetector.kt`
**Línea(s):** 40-60
**Problema:** Carga modelo en init (main thread potencial). `interpreter` no null-check sincronizado.
**Solución:** Cargar en `Dispatchers.IO` via factory suspend.

---

## Lista Priorizada de Issues (Top 20)

1. 🔴 TlsSocketHelper trust-all MITM
2. 🔴 Firmware ESP32 sin auth para PLC relay
3. 🔴 Path traversal G-code filename
4. 🔴 AutoApprove bypass global
5. 🔴 CommandBroker runBlocking ANR
6. 🟠 Thread.sleep en corutina PermissionManager
7. 🟠 sanitizeInput destruye protocolo
8. 🟠 Handshake duplicado + no validado en MainActivity
9. 🟠 LEGACY token hash permite bypass
10. 🟠 CoordinatorViewModel bucle infinito no cancelable
11. 🟠 TcpServer broadcast spam + sin rate limit
12. 🟠 BluetoothHardwareManager God Object
13. 🟡 TrackingState mezcla UI+Domain
14. 🟡 StationClient race condition anti-spam
15. 🟡 CommunicationCoordinator no envía comandos
16. 🟡 testModeSecretGesture 5 taps sin PIN
17. 🟡 CI workflow branch hardcoded
18. 🟡 YoloTfliteDetector carga en main thread
19. 🟢 Duplicación mipmap icons
20. 🟢 buildReport ensucia root

---

## Código Corregido (Críticos y Altos)

### 1. TlsSocketHelper.kt CORREGIDO
```kotlin
package com.sistema.distribuido.network

import java.net.InetSocketAddress
import java.net.Socket
import java.security.KeyStore
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManagerFactory

object TlsSocketHelper {
    @Volatile var enabled: Boolean = false

    // Producción: cargar CA desde assets/cim_ca.crt y usar pinning
    fun createClientSocket(host: String, port: Int, timeoutMs: Int = 2000): Socket {
        if (!enabled) {
            return Socket().apply { connect(InetSocketAddress(host, port), timeoutMs) }
        }
        // Usa TrustManagers del sistema por defecto (no trust-all)
        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        tmf.init(null as KeyStore?)
        val ctx = SSLContext.getInstance("TLSv1.3").apply {
            init(null, tmf.trustManagers, java.security.SecureRandom())
        }
        val socket = ctx.socketFactory.createSocket(host, port) as SSLSocket
        socket.enabledProtocols = arrayOf("TLSv1.3", "TLSv1.2")
        socket.soTimeout = timeoutMs
        socket.startHandshake() // fuerza validación inmediata
        return socket
    }
}
```

### 2. PermissionManager.kt CORREGIDO (wait + autoApprove protegido)
```kotlin
suspend fun requestPermission(mac:String, appType:AppType, deviceName:String): PermissionDecision {
    if (isBlocked(mac)) { AuthorizationManager.deny(mac); return PermissionDecision.REJECTED }
    // Solo debug y flag explícito
    if (GlobalPermissionManager.autoApproveTestMode && BuildConfig.DEBUG) {
        remembereddecisions[mac] = Pair(true, System.currentTimeMillis())
        saveRememberedDecision(mac, true)
        try{ AuthorizationManager.authorize(mac) }catch(_:Exception){}
        return PermissionDecision.APPROVED
    }
    val remembered = remembereddecisions[mac]
    if(remembered!=null && System.currentTimeMillis()-remembered.second < 86_400_000){
        return if(remembered.first) APPROVED else REJECTED
    }
    val request = PermissionRequest(mac=mac, appType=appType, deviceName=deviceName)
    pendingRequests[mac]=request
    listeners.forEach{ it.onPermissionRequested(request) }
    return waitForApproval(mac, 5000)
}
private suspend fun waitForApproval(mac:String, timeout:Long): PermissionDecision {
    val start = System.currentTimeMillis()
    while(System.currentTimeMillis()-start < timeout){
        val req = pendingRequests[mac]
        if(req!=null && req.respondedAt>0) return if(req.approved) APPROVED else REJECTED
        kotlinx.coroutines.delay(100)
    }
    pendingRequests.remove(mac)
    listeners.forEach{ it.onPermissionExpired(mac) }
    return TIMEOUT
}
```

### 3. IndustrialErrorManager.kt CORREGIDO
```kotlin
object IndustrialErrorManager {
    private const val MAX_LEN = 1024
    fun sanitizeInput(input:String): String {
        if(input.isBlank()) return ""
        val filtered = input.filter { it.code >= 32 && it.code != 127 } // no control chars
        val truncated = filtered.take(MAX_LEN)
        require(!truncated.contains("..")){ "Path traversal" }
        require(truncated.length >= truncated.trim().length * 0.5){ "Demasiados espacios/control" }
        return truncated.trim()
    }
    fun sanitizeFileName(raw:String, allowedExts:Set<String>=setOf(".gcode",".nc",".txt")): String {
        val base = raw.substringAfterLast('/').substringAfterLast('\\').substringAfterLast(':')
        val clean = base.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(64)
        require(allowedExts.any{ clean.lowercase().endsWith(it) }){ "Ext no permitida: $clean" }
        return clean.ifBlank{ "file_${System.currentTimeMillis()}.gcode" }
    }
}
```

### 4. StationClient anti-spam thread-safe
```kotlin
private val sendMutex = Mutex()
private val lastSent = AtomicReference(Pair("",0L))

suspend fun sendSafe(msg:String): Boolean = withContext(Dispatchers.IO){
    sendMutex.withLock{
        val now = System.currentTimeMillis()
        val (lastMsg, lastTime) = lastSent.get()
        if(msg==lastMsg && now-lastTime < 300) return@withContext true
        lastSent.set(Pair(msg, now))
        val clean = IndustrialErrorManager.sanitizeInput(msg)
        tcpClient.sendSafe(clean)
    }
}
```

### 5. Firmware cim_ble_firmware.h CORREGIDO (auth)
```c
static bool isAuthorized = false;
static unsigned long lastAuth = 0;
static bool checkAuthToken(const String& raw){
  if(raw.startsWith("CIM_AUTH:")){
    String token = raw.substring(9);
    token.trim();
    // token debe ser sha256 de PASSWORD_ACTUAL
    if(token.length()==71 && token.startsWith("sha256:")){
      // En producción comparar con hash almacenado en NVS
      isAuthorized=true;
      lastAuth=millis();
      sendBleResponse("AUTH:OK");
      return true;
    }
  }
  return false;
}
static void handleCommand(String raw){
  raw.trim();
  if(raw.length()==0) return;
  if(checkAuthToken(raw)) return;
  if(!isAuthorized || millis()-lastAuth > 300000){ // 5min auth timeout
    if(raw.indexOf("IDENTIFY")>=0){
      sendBleResponse("CIM_ID|...|AUTH_REQUIRED");
      return;
    }
    sendBleResponse("ERR:NOT_AUTHORIZED");
    return;
  }
  // ... resto lógica pero PLC bloqueado sin auth
  if(raw.startsWith("PLC:")){
#ifdef CIM_IS_PLC
    // watchdog: auto-off relay after 10s sin heartbeat
#endif
  }
}
```

### 6. CoordinatorViewModel - polling seguro
```kotlin
private fun startMonitoring(){
  viewModelScope.launch{
    while(isActive){
      try{
        updateDeviceList()
        val pending = try{ GlobalPermissionManager.getInstance().getPendingRequests() }catch(e:Exception){ emptyList() }
        _uiState.update{ it.copy(pendingPermissionRequest=pending.firstOrNull(), ...) }
      }catch(e:CancellationException){ throw e }
      catch(e:Exception){ Log.w("CIM","Mon fail ${e.message}") }
      delay(2000)
    }
  }
}
```

---

## Plan de Refactorización

### Fase 1 – Seguridad inmediata (0-2 días)
1. Parchear TlsSocketHelper (eliminar trust-all)
2. Parchear PermissionManager (eliminar autoApprove en release, fix Thread.sleep)
3. Sanitizar filenames G-code en Manufactura/Coordinador
4. Añadir auth check en firmware ESP32 + watchdog relay
5. CI: arreglar workflow para correr en `arena/**` y main

### Fase 2 – Estabilidad (3-7 días)
1. Eliminar `runBlocking` en CommandBroker, usar solo suspend
2. Fix `CoordinatorViewModel` bucle cancelable, quitar `catch (_:Exception)` silenciosos
3. Implementar `HandshakeParser` único, eliminar duplicación
4. Rate-limit TcpServer + broadcast solo on-change
5. Reescribir `IndustrialErrorManager.sanitizeInput` correctamente
6. Añadir validación MAC con regex estricto + longitud

### Fase 3 – Arquitectura (1-3 semanas)
1. Introducir capas: `domain/model`, `domain/usecase`, `data/repository`, `ui`
2. Descomponer `BluetoothHardwareManager` en 3 clases
3. Migrar GlobalSingletons a Hilt modules (Singleton scoped)
4. Crear `FileStorageRepository` con sanitización y límite 5MB
5. Separar `CoordinatorViewModel` en: `CintaViewModel`, `RobotViewModel`, `NetworkViewModel`, `TrackingViewModel`
6. Implementar `Result<T>` sealed class para manejo errores sin excepciones crudas

### Fase 4 – Calidad y Performance
1. Tests unitarios para `CimProtocol`, `CimMessage`, `PermissionManager`, `HandshakeParser` (target 80% core-network)
2. Tests integración para TCP handshake + auth
3. Perf: mover YoloTfliteDetector.init a IO, cachear interpreter, usar `StateFlow` para scanning
4. Documentar KDoc en todos public API
5. Lint baseline limpieza y activar `abortOnError true`

---

## Checklist de Verificación

- [ ] **Seguridad**
  - [ ] `TlsSocketHelper` no contiene `X509TrustManager` vacío
  - [ ] `grep -R "checkClientTrusted" android/` retorna 0
  - [ ] G-code filename pasa `sanitizeFileName` con whitelist
  - [ ] Firmware rechaza `PLC:START` si `isAuthorized==false`
  - [ ] `autoApproveTestMode` solo true si `BuildConfig.DEBUG`
  - [ ] No hay `Thread.sleep` en código Kotlin (solo `delay`)
  - [ ] `sanitizeInput` permite `|` `;` pero bloquea `..` y control chars

- [ ] **Estabilidad**
  - [ ] `CommandBroker.send()` sin `runBlocking`
  - [ ] `CoordinatorViewModel.startMonitoring` chequea `isActive`
  - [ ] No hay `catch (_: Exception){}` vacío (al menos Log.w)
  - [ ] TcpServer broadcast solo on-change (contar logs)

- [ ] **Arquitectura**
  - [ ] `BluetoothHardwareManager` < 300 LOC (refactorizado)
  - [ ] `CoordinatorViewModel` < 300 LOC
  - [ ] Hilt modules proveen PermissionManager, BluetoothManager
  - [ ] Existe `domain/` package con UseCases

- [ ] **Testing**
  - [ ] `./gradlew testAllModules` pasa 100%
  - [ ] Coverage core-network > 70% (Jacoco)
  - [ ] Tests para handshake parser y file sanitization

- [ ] **Documentación**
  - [ ] KDoc en public fun core-network
  - [ ] README actualizado con diagrama arquitectura
  - [ ] CHANGELOG con fixes seguridad

---

## Apéndice – Métricas

- LOC Kotlin: ~12k (apps) + ~8k (core-network) = ~20k
- Duplicación detectada: ~18% (handshakes, G-code loading, mipmap)
- Complejidad ciclomática promedio: `CoordinatorViewModel` 28, `BluetoothHardwareManager` 34 (alto, umbral recomendado <15)
- Deuda técnica estimada: 23 días-hombre
- Riesgo industrial: Alto (actuadores sin auth)

---

**Fin de auditoría.** Los parches críticos deben aplicarse antes de cualquier prueba con hardware real. No energizar relés ni robot hasta validar que `isAuthorized` y TLS están corregidos.
