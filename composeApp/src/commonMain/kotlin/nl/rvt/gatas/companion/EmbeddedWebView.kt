package nl.rvt.gatas.companion

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun EmbeddedWebView(
    url: String,
    modifier: Modifier = Modifier,
)
