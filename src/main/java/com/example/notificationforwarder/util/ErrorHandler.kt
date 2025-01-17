package com.example.notificationforwarder.util

import android.util.Log
import com.example.notificationforwarder.service.email.EmailException
import java.io.IOException

sealed class AppError(val message: String, val cause: Throwable? = null) {
    class NetworkError(message: String, cause: Throwable? = null) : AppError(message, cause)
    class EmailError(message: String, cause: Throwable? = null) : AppError(message, cause)
    class DatabaseError(message: String, cause: Throwable? = null) : AppError(message, cause)
    class ConfigurationError(message: String) : AppError(message)
}

object ErrorHandler {
    private const val TAG = "NotificationForwarder"

    fun handleError(error: Throwable): AppError {
        Log.e(TAG, "Error occurred", error)
        return when (error) {
            is EmailException -> AppError.EmailError("Erreur d'envoi d'email: ${error.message}", error)
            is IOException -> AppError.NetworkError("Erreur réseau: ${error.message}", error)
            else -> AppError.DatabaseError("Erreur inattendue: ${error.message}", error)
        }
    }
} 