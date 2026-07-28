package com.example.plc
import android.util.Log
import org.junit.Before
import org.junit.After

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Before
import org.junit.After
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.After

import org.junit.Test
import org.junit.Before
import org.junit.After
import org.junit.runner.RunWith
import org.junit.Before
import org.junit.After

import org.junit.Assert.*
import org.junit.Before
import org.junit.After

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.example.plc", appContext.packageName)
    }
}