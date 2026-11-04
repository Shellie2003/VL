package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.cutpilot.data.DefaultPresets
import com.example.cutpilot.model.ModifierKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Vôlavo", appName)
    }

    @Test
    fun `verify default presets contain Premiere Pro and DaVinci Resolve`() {
        val profiles = DefaultPresets.allProfiles
        assertTrue(profiles.isNotEmpty())
        val premiere = profiles.find { it.id == "premiere_pro" }
        assertNotNull(premiere)
        assertTrue(premiere!!.shortcuts.any { it.label.contains("Cutter") })

        val canva = profiles.find { it.id == "canva_studio" }
        assertNotNull(canva)
        assertTrue(canva!!.shortcuts.any { it.label.contains("Scinder") })
    }
}
