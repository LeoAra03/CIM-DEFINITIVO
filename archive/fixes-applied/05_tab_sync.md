# FIX #21 - Sincronización de Pestañas

## Problema
selectedTabIndex no se sincronizaba con el ViewModel.

## Solución aplicada
Se mejoró la sincronización en CoordinatorMasterScreen:

```kotlin
var selectedTabIndex by remember { mutableStateOf(state.currentTabIndex) }

LaunchedEffect(state.currentTabIndex) {
    selectedTabIndex = state.currentTabIndex
}

NavigationBarItem(
    selected = selectedTabIndex == index,
    onClick = {
        selectedTabIndex = index
        vm.selectTab(index)
    }
)
```

## Archivos modificados
- MainActivity.kt (CoordinatorMasterScreen)

## Estado
✅ CORREGIDO
