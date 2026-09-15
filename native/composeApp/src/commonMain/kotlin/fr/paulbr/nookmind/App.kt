package fr.paulbr.nookmind

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import fr.paulbr.nookmind.resources.Res
import fr.paulbr.nookmind.resources.common_appName
import org.jetbrains.compose.resources.stringResource

@Composable
fun App() {
    MaterialTheme {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(Res.string.common_appName))
        }
    }
}
