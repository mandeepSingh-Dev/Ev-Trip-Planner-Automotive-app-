/**
 * Package definition for the OEM EV Trip Planner Automotive application.
 */
package com.oem.evplanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.oem.evplanner.ui.theme.EVTripPlannerAutomotiveAppTheme

/**
 * Main application activity for mobile/companion fallback UI views, configured
 * with edge-to-edge display and Jetpack Compose support.
 */
class MainActivity : ComponentActivity() {
    /**
     * Called when the activity is starting, setting up the Jetpack Compose content view.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Enable edge-to-edge display layout rendering
        enableEdgeToEdge()
        // Set root Jetpack Compose content view hierarchy
        setContent {
            // Apply the custom Automotive app theme styling
            EVTripPlannerAutomotiveAppTheme {
                // Root scaffold layout filling entire available display bounds
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Placeholder inner content container for mobile view if required
                }
            }
        }
    }
}
