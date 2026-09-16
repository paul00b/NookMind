package fr.paulbr.nookmind.core.ui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
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

    private val _cues = MutableSharedFlow<Toast>(extraBufferCapacity = 8)
    /** One emission per toast that should be felt. Automatic background failures opt out. */
    val cues: SharedFlow<Toast> = _cues

    fun success(res: StringResource, vararg args: Any) = show(ToastMessage.Resource(res, args.toList()), ToastKind.SUCCESS)
    fun error(res: StringResource, vararg args: Any) = show(ToastMessage.Resource(res, args.toList()), ToastKind.ERROR)
    fun success(text: String) = show(ToastMessage.Text(text), ToastKind.SUCCESS)
    fun error(text: String) = show(ToastMessage.Text(text), ToastKind.ERROR)

    /** Like [error], but for automatic background failures the user never asked for — no haptic cue. */
    fun errorSilently(res: StringResource, vararg args: Any) =
        show(ToastMessage.Resource(res, args.toList()), ToastKind.ERROR, cue = false)

    fun show(message: ToastMessage, kind: ToastKind, durationMs: Long = 3_000, cue: Boolean = true) {
        val toast = Toast(nextId++, message, kind)
        _toasts.update { it + toast }
        if (cue) _cues.tryEmit(toast)
        scope.launch {
            delay(durationMs)
            dismiss(toast.id)
        }
    }

    fun dismiss(id: Long) = _toasts.update { list -> list.filter { it.id != id } }
}
