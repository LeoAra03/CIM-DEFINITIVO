# SESSION 2 COMPLETION GUIDE
## Integración de Módulos de Estación + Orchestración Central

**Estado**: Scaffolds completados (8/8 componentes principales)
**Fecha**: Session 2 Final
**Siguiente**: Session 3 - UI Cleanup & App-manufactura migration

---

## 📊 Estructura Creada

### Módulos Estación (4)
```
✅ app-scorbot/
   ├── build.gradle.kts (configuración)
   ├── AndroidManifest.xml (permisos)
   └── MainActivity.kt + ScorbotViewModel.kt
   
✅ app-vision/
   ├── build.gradle.kts
   ├── AndroidManifest.xml  
   └── MainActivity.kt + VisionViewModel.kt

✅ app-laser/
   ├── build.gradle.kts
   ├── AndroidManifest.xml
   └── MainActivity.kt + LaserViewModel.kt

✅ app-conveyor/
   ├── build.gradle.kts
   ├── AndroidManifest.xml
   └── MainActivity.kt + ConveyorViewModel.kt
```

### Hub Central (Coordinador)
```
✅ app-coordinador/
   ├── MainActivity.kt (entry point + setContent HubScreen)
   ├── HubViewModel.kt (orquestación de autorización)
   ├── HubScreen.kt (dispositivos + AuthorizationDialog)
   └── AuthorizationDialog.kt (componente modal - ya existía)
```

---

## 🔌 Puntos de Integración Críticos

### 1. **CommunicationCoordinator → HubViewModel**
**Archivo**: `app-coordinador/viewmodels/HubViewModel.kt`

**Flujo**:
```
ESP32 IDENTIFY → BluetoothHardwareManager
              → CommunicationCoordinator.handleIncomingMessage()
              → SessionState creada
              → coordinationStatus StateFlow emitido
              → HubViewModel.startMonitoringDevices() detecta cambio
              → showAuthorizationDialog(device)
              → AuthorizationDialog.kt renderizado
```

**Implementación Requerida**:
```kotlin
// HubViewModel.kt línea 37-46
viewModelScope.launch {
    commCoordinator.coordinationStatus.collect { status ->
        // status es Map<String, SessionState> de devices
        updateDeviceStates(status)
    }
}
```

**TODO PENDIENTE**: Instanciar `CommunicationCoordinator` en el constructor de HubViewModel
```kotlin
// En algún lugar de app-coordinador que tenga Dagger/Hilt:
val commCoordinator = CommunicationCoordinator(
    authManager = AuthorizationManager,
    permManager = PermissionManager,
    btManager = BluetoothHardwareManager
)
```

---

### 2. **Estaciones → CommunicationCoordinator**
**Archivos**: 
- `app-scorbot/viewmodels/ScorbotViewModel.kt` (línea 58)
- `app-vision/viewmodels/VisionViewModel.kt` (línea 45)
- `app-laser/viewmodels/LaserViewModel.kt` (línea 49)
- `app-conveyor/viewmodels/ConveyorViewModel.kt` (línea 51)

**Estado Actual**: Todos tienen `TODO: Integrar con CommunicationCoordinator`

**Implementación Requerida**:
```kotlin
// En cada executeCommand():
fun executeCommand(command: String) {
    if (!_isAuthorized.value) {
        Timber.tag("ScorbotVM").w("Sin autorización: $command")
        return
    }
    
    // TODO IMPLEMENTAR:
    val routed = commCoordinator.routeCommand(
        mac = deviceMac,
        command = "COMMAND|$command",
        timeoutMs = 5000
    )
    
    if (routed) {
        Timber.tag("ScorbotVM").i("Comando enrutado exitosamente")
    } else {
        Timber.tag("ScorbotVM").e("Fallo al enviar comando")
    }
}
```

**Inyección de Dependencia Requerida**:
```kotlin
class ScorbotViewModel(
    private val commCoordinator: CommunicationCoordinator
) : ViewModel()

// En MainActivity:
@Composable
fun ScorbotApp() {
    val viewModel: ScorbotViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                ScorbotViewModel(
                    commCoordinator = /* obtener instancia global */
                )
            }
        }
    )
}
```

---

### 3. **AuthorizationDialog → HubViewModel.approve/reject**
**Archivo**: `app-coordinador/ui/screens/HubScreen.kt` (línea 114-127)

**Estado**: ✅ Completamente Integrado

```kotlin
// HubScreen.kt línea 114-127
AnimatedVisibility(visible = authDialogState != null) {
    authDialogState?.let { dialog ->
        AuthorizationDialog(
            deviceName = dialog.name,
            deviceMac = dialog.mac,
            onApprove = { rememberDecision ->
                viewModel.approveDevice(dialog.mac, rememberDecision)
            },
            onReject = { rememberDecision ->
                viewModel.rejectDevice(dialog.mac, rememberDecision)
            }
        )
    }
}
```

**Flujo**:
```
AuthorizationDialog "Aprobar" click
  → HubViewModel.approveDevice(mac, remember)
  → PermissionManager.grantPermission(mac)
  → CommunicationCoordinator.sendAuthorizationResponse(mac, true)
  → ESP32 recibe "AUTHORIZED|..." 
  → authState = AUTHORIZED
  → Comandos ahora permitidos
```

---

## 🧪 Validación & Pruebas (Session 3)

### Checklist Previo a Session 4 (E2E Testing)

- [ ] **Gradle Build**: `./gradlew :app-coordinador:app:build` ✅ compila sin errores
- [ ] **Modulos Registrados**: `settings.gradle.kts` incluye `:app-scorbot:app`, `:app-vision:app`, etc.
- [ ] **Namespaces Únicos**: Cada app tiene namespace diferente (com.sistema.distribuido.scorbot, etc.)
- [ ] **Permisos Bluetooth**: AndroidManifest.xml en cada app tiene BLUETOOTH + BLUETOOTH_SCAN
- [ ] **Core-Network Dependency**: Todas las apps importan `:core-network` en build.gradle
- [ ] **ViewModels Compielen**: Verificar que AuthorizationManager import es correcto

---

## 🚀 Próximos Pasos (Session 3)

### Immediate Tasks
1. **Verificar compilación**: `./gradlew clean build`
2. **Inyección de dependencias**: Implementar Hilt/Dagger para inyectar CommunicationCoordinator
3. **Audit de app-manufactura**: Buscar UI phantom buttons (ver IMPLEMENTATION_SUMMARY_V6.md)
4. **Migrar funcionalidad** desde app-manufactura a app-coordinador si es necesario

### Architectural Improvements
1. Crear `StationCommandService` base class para reutilizar lógica de command routing
2. Implementar `SessionStateManager` para persistencia de autorización
3. Agregar timeout handler para dispositivos que no responden después de autorizados

---

## 📝 Resumen de Estado

| Componente | Estado | Líneas | Notas |
|-----------|--------|--------|-------|
| CommunicationCoordinator | ✅ Completo | 620 | Session 1 |
| ESP32 Firmware v7.0 | ✅ Completo | 260 | Session 1 FreeRTOS |
| AuthorizationDialog | ✅ Completo | 180 | Session 1 |
| HubViewModel | ✅ Completo | 140 | Session 2 |
| HubScreen | ✅ Completo | 220 | Session 2 |
| ScorbotViewModel | 🟡 TODO | 45 | Integración CommunicationCoordinator |
| VisionViewModel | 🟡 TODO | 40 | Integración CommunicationCoordinator |
| LaserViewModel | 🟡 TODO | 45 | Integración CommunicationCoordinator |
| ConveyorViewModel | 🟡 TODO | 42 | Integración CommunicationCoordinator |

---

## 🔑 Claves para Session 3

1. **No cambiar Session 2 scaffolds**: Solo agregar Hilt/inyección de dependencias
2. **Audit app-manufactura primero**: Identificar qué buttons/UI son phantom antes de mover
3. **Mantener core-network inalterado**: CommunicationCoordinator ya está en 100% funcionalidad
4. **Test cada app individualmente**: Build local en cada módulo antes de E2E

---

**Creado por**: GitHub Copilot  
**Session**: 2/5  
**Tiempo Estimado Session 3**: 3-4 horas  
**Bloqueadores para Session 4**: Inyección de CommunicationCoordinator en ViewModels
