# Instrucciones para la IA (Plantilla de Diseño)

Cuando el usuario solicite crear una nueva pantalla o aplicación, utiliza los siguientes archivos como referencia obligatoria para mantener la consistencia visual:

1. **DesignSystem.kt**: Contiene los colores base y componentes (Cards, Buttons, Scaffold).
2. **EjemploUso.kt**: Muestra la estructura de código recomendada.

### Reglas Estéticas:
- **Colores**: Fondo oscuro (#121212), Tarjetas (#1E1E1E), Acentos en Azul (#42A5F5).
- **Tipografía**: Títulos en Mayúsculas, peso ExtraBold.
- **Formas**: Esquinas redondeadas de 12.dp y bordes sutiles con transparencia.
- **Estados**: Usar `IndustrialStatusRow` con indicadores circulares (Verde para OK, Gris para Off).
