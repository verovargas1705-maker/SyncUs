package com.example.syncus.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class AuthUiState(
    val displayName: String = "",
    val email: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val error: String? = null
)

class AuthViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun onNameChange(value: String) {
        _state.value = _state.value.copy(displayName = value, error = null)
    }

    fun onEmailChange(value: String) {
        _state.value = _state.value.copy(email = value, error = null)
    }

    fun onPasswordChange(value: String) {
        _state.value = _state.value.copy(password = value, error = null)
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun register(onSuccess: () -> Unit) {
        val name = _state.value.displayName.trim()
        val email = _state.value.email.trim()
        val pass = _state.value.password

        if (name.isBlank()) {
            _state.value = _state.value.copy(error = "Escribe tu nombre")
            return
        }
        if (email.isBlank()) {
            _state.value = _state.value.copy(error = "Escribe tu email")
            return
        }
        if (pass.length < 6) {
            _state.value = _state.value.copy(
                error = "La contraseña debe tener al menos 6 caracteres"
            )
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)

            runCatching {
                auth.createUserWithEmailAndPassword(email, pass).await()

                val u = auth.currentUser ?: error("No se pudo obtener usuario")

                val req = UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()

                u.updateProfile(req).await()
                u.reload().await()

                db.collection("users").document(u.uid)
                    .set(
                        mapOf(
                            "displayName" to name,
                            "email" to email,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                    .await()
            }.onSuccess {
                _state.value = _state.value.copy(loading = false)
                onSuccess()
            }.onFailure { e ->
                _state.value = _state.value.copy(
                    loading = false,
                    error = e.message ?: "Error creando cuenta"
                )
            }
        }
    }

    fun login(onSuccess: () -> Unit) {
        val email = _state.value.email.trim()
        val pass = _state.value.password

        if (email.isBlank()) {
            _state.value = _state.value.copy(error = "Escribe tu email")
            return
        }
        if (pass.isBlank()) {
            _state.value = _state.value.copy(error = "Escribe tu contraseña")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)

            runCatching {
                auth.signInWithEmailAndPassword(email, pass).await()
            }.onSuccess {
                _state.value = _state.value.copy(loading = false)
                onSuccess()
            }.onFailure { e ->
                _state.value = _state.value.copy(
                    loading = false,
                    error = e.message ?: "Error iniciando sesión"
                )
            }
        }
    }
}