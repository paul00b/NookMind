package fr.paulbr.nookmind.core.ui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource

enum class ToastKind { SUCCESS, ERROR, INFO }

/** A toast body: either a localized resource (resolved at render time) or raw text (server errors). */
sealed interface ToastMessage {
    data class Resource(val res: StringResource, val args: List<Any> = emptyList()) : ToastMessage
    data class Text(val text: String) : ToastMessage
}

data class Toast(val id: Long, val message: ToastMessage, val kind: ToastKind)

/** Port of react-hot-toast: bottom-centred stack above the navigation, 3 s per toast. */
class ToastController(private val scope: CoroutineScope) {
    private val _toasts = MutableStateFlow<List<Toast>>(emptyList())
    val toasts: StateFlow<List<Toast>> = _toasts
    private var nextId = 0L

    fun success(res: StringResource, vararg args: Any) = show(ToastMessage.Resource(res, args.toList()), ToastKind.SUCCESS)
    fun error(res: StringResource, vararg args: Any) = show(ToastMessage.Resource(res, args.toList()), ToastKind.ERROR)
    fun success(text: String) = show(ToastMessage.Text(text), ToastKind.SUCCESS)
    fun error(text: String) = show(ToastMessage.Text(text), ToastKind.ERROR)

    fun show(message: ToastMessage, kind: ToastKind, durationMs: Long = 3_000) {
        val toast = Toast(nextId++, message, kind)
        _toasts.update { it + toast }
        scope.launch {
            delay(durationMs)
            dismiss(toast.id)
        }
    }

    fun dismiss(id: Long) = _toasts.update { list -> list.filter { it.id != id } }
}
