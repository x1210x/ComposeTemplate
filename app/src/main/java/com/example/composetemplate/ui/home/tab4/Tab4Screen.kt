package com.example.composetemplate.ui.home.tab4

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun Tab4Screen(
    viewModel: Tab4ViewModel = viewModel(),
    showSnackbar: (String) -> Unit
) {
    LaunchedEffect(true) {
        viewModel.init()
    }

    val uiState = viewModel.uiState
    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.Blue.copy(0.3f))) {
        Button(
            modifier = Modifier.align(Alignment.Center),
            onClick = { showSnackbar("탭4 클릭") }
        ) {
            Text(uiState.text)
        }
    }
}