package fr.paulbr.nookmind.feature.series

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import fr.paulbr.nookmind.app.AppContainer
import fr.paulbr.nookmind.core.designsystem.components.NookSheet
import fr.paulbr.nookmind.core.model.Series

// Placeholder — replaced by the real sheet in the series step.
@Composable
fun AddSeriesSheet(container: AppContainer, prefill: Series?, onClose: () -> Unit) {
    NookSheet(onClose = onClose) { Text(prefill?.title ?: "") }
}
