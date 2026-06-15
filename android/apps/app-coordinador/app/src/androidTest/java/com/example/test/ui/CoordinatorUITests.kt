package com.example.test.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI Tests para CoordinadorMasterScreen usando Espresso + Compose
 */
@RunWith(AndroidJUnit4::class)
class CoordinatorUITests {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testTabNavigationExists() {
        // Verificar que los tabs son renderizados
        composeTestRule.onNodeWithText("Sistema").assertExists()
        composeTestRule.onNodeWithText("Robot").assertExists()
        composeTestRule.onNodeWithText("ArUco").assertExists()
        composeTestRule.onNodeWithText("Tracking").assertExists()
        composeTestRule.onNodeWithText("Red").assertExists()
        composeTestRule.onNodeWithText("Almacén").assertExists()
    }

    @Test
    fun testSystemTabButtonsExist() {
        // Navegar y verificar botones en SystemTab
        composeTestRule.onNodeWithText("Control de Cinta Transportadora")
            .assertExists()
    }

    @Test
    fun testNetworkTabInitialState() {
        // Navegar a NetworkTab
        composeTestRule.onNodeWithText("Red").performClick()

        // Verificar que el status del servidor sea visible
        composeTestRule.onNodeWithText("Servidor TCP")
            .assertExists()
    }

    @Test
    fun testStorageGridVisible() {
        // Navegar a StorageTab
        composeTestRule.onNodeWithText("Almacén").performClick()

        // Verificar que el grid es visible
        composeTestRule.onNodeWithText("Posiciones de Almacenamiento")
            .assertExists()
    }
}

