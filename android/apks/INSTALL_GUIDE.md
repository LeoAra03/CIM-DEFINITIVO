# Instalación rápida de APKs CIM v6.0

Primero genera las APKs:

```bash
cd config
./gradlew buildAllApks validateApks writeApkChecksums
```

Instala en un dispositivo Android conectado por ADB:

```bash
adb install -r config/output-apks/app-coordinador.apk
adb install -r config/output-apks/app-plc.apk
adb install -r config/output-apks/app-manufactura.apk
adb install -r config/output-apks/app-calidad.apk
adb install -r config/output-apks/app-almacen.apk
adb install -r config/output-apks/wear-coordinador.apk
```

En Windows también puedes usar:

```powershell
.\tools\powershell\Instalar-APKs.ps1
```

Si no usas ADB, copia las APKs al teléfono y ábrelas manualmente (activa fuentes desconocidas si corresponde).
