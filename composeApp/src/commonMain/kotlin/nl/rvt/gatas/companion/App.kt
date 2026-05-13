package nl.rvt.gatas.companion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.icerock.moko.permissions.PermissionsController
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App(permissionsController: PermissionsController) {
    MaterialTheme {
        RootContent(
            modifier = Modifier.fillMaxSize()
                .background(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
            permissionsController = permissionsController
        )
    }
}
