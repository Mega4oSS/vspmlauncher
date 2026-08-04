package ru.artem.alaverdyan.vspmlauncher.update

import kotlinx.coroutines.delay

private const val MAX_RETRIES = 3
private const val RETRY_BASE_DELAY_MS = 1000L

suspend fun <T> withRetry(
    description: String,
    block: suspend () -> T
): T {
    var lastError: Exception? = null
    repeat(MAX_RETRIES) { attempt ->
        try {
            return block()
        } catch (e: Exception) {
            lastError = e
            if (attempt < MAX_RETRIES - 1) {
                delay(RETRY_BASE_DELAY_MS * (attempt + 1))
            }
        }
    }
    throw IllegalStateException("Не удалось выполнить \"$description\" за $MAX_RETRIES попыток", lastError)
}