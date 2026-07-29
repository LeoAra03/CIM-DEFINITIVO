# Expectativa vs. resultado — corte pre-hardware

| Expectativa | Resultado verificable actual | Brecha / siguiente evidencia |
|---|---|---|
| Plataforma CIM integrada | Estructura Android, red y firmware activos documentados | Ejecutar integración con dispositivos reales. |
| Entrega reproducible | Validadores Python, controles de sintaxis y documentación | CI final y logs del commit de banco. |
| Seguridad de operación | Diseño y procedimiento conservadores | E-stop, relé, límites e interlocks físicos deben probarse. |
| Comunicación robusta | Contratos de identidad/framing y simulación | Medir BLE/reconexión bajo condiciones reales. |
| Inspección de calidad | Herramientas para modelo/visión disponibles | Convertir/validar TFLite y piezas/ArUco reales. |
| Ciclo CIM completo | Máquina de estados simulada | Evidencia de ciclo físico controlado. |

La diferencia entre expectativa y resultado no se oculta: define el orden de pruebas y evita conclusiones anticipadas.
