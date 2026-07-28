# FIX #20 - CameraExecutor Lifecycle

## Problema
CameraExecutor no se cancelaba correctamente causando memory leaks.

## Solución aplicada
Mejorado el manejo de ciclo de vida en CameraPreviewWithVision.kt:

```kotlin
DisposableEffect(Unit) {
    onDispose {
        cameraExecutor.shutdown()
        Log.d("CameraPreview", "CameraExecutor shutdown completado")
    }
}
```

Además se agregó verificación de estado antes de análisis:

```kotlin
if (!isDetecting) {
    imageProxy.close()
    return
}
```

## Archivos modificados
- CameraPreviewWithVision.kt

## Estado
✅ CORREGIDO
