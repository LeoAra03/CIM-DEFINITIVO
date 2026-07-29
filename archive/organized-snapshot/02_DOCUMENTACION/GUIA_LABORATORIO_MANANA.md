# GUÍA DE LABORATORIO - CIM v6.0

## Preparación (5 minutos)

1. Instalar las 5 APKs usando `Instalar-APKs.ps1`
2. Conectar todos los teléfonos a la misma red Wi-Fi

## Demo Rápida (15 minutos)

### Escenario 1: Modo Simulado (Recomendado)
1. En el **Coordinador**:
   - Abrir app-coordinador
   - Ir a pestaña EXEC
   - Activar "AUTO MODE"
   - Pulsar "SIMULAR CICLO COMPLETO" (si aparece) o usar la consola de automatización

2. En cada estación:
   - Abrir la app correspondiente
   - Ir a pestaña SINCRO
   - Activar "Modo Autónomo"
   - Usar botones de simulación de sensores

### Escenario 2: Con Red (Avanzado)
1. Iniciar HUB en Coordinador (pestaña NODOS)
2. Anotar la IP mostrada
3. En cada estación: SINCRO → ingresar IP → VINCULAR
4. Autorizar desde el HUB
5. Ejecutar comandos desde cualquier estación

## Verificación de Funcionamiento

- [ ] Logs muestran mensajes en tiempo real
- [ ] Comandos se envían sin errores
- [ ] Modo autónomo funciona sin red
- [ ] Autorización funciona con red
- [ ] Simuladores de sensores responden

## Troubleshooting

**"No autorizado"**: Activar Modo Autónomo o esperar VALIDADO

**Sin conexión Bluetooth**: El sistema funciona sin él (modo simulado)

**Handshake falla**: Verificar que la contraseña sea la misma en todas las apps

