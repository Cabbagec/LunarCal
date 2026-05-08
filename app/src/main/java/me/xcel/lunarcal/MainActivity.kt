package me.xcel.lunarcal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import me.xcel.lunarcal.ui.LunarCalApp
import me.xcel.lunarcal.ui.LunarCalendarViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: LunarCalendarViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LunarCalApp(viewModel = viewModel)
        }
    }
}
