package com.example.codemanager.data.repository

import com.example.codemanager.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.lang.Exception

class AuthRepository {

    private val auth: FirebaseAuth = Firebase.auth
    private val db: FirebaseFirestore = Firebase.firestore

    // Iniciar sesión
    suspend fun signIn(email: String, password: String): Result<Unit> {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Obtener usuario actual
    suspend fun getCurrentUser(): User? {
        return auth.currentUser?.let { firebaseUser ->
            try {
                val document = db.collection("users").document(firebaseUser.uid).get().await()
                if (document.exists()) {
                    document.toObject(User::class.java)
                } else {
                    // Crear usuario por defecto si no existe en Firestore
                    val newUser = User(
                        id = firebaseUser.uid,
                        email = firebaseUser.email ?: "",
                        name = firebaseUser.displayName ?: firebaseUser.email?.split("@")?.first() ?: "",
                        rol = "Usuario"
                    )
                    db.collection("users").document(firebaseUser.uid).set(newUser).await()
                    newUser
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    fun signOut() {
        auth.signOut()
    }

    suspend fun getAllUsers(): List<User> {
        return try {
            val snapshot = db.collection("users").get().await()
            snapshot.documents.map { document ->
                document.toObject(User::class.java) ?: User()
            }.filter { it.id.isNotEmpty() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun createUser(email: String, password: String, name: String, rol: String): Result<User> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val userId = authResult.user?.uid ?: throw Exception("Error al crear usuario")

            val newUser = User(
                id = userId,
                email = email,
                name = name,
                rol = rol
            )

            db.collection("users").document(userId).set(newUser).await()
            Result.success(newUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUser(userId: String, name: String, rol: String): Result<Unit> {
        return try {
            val updates = mapOf(
                "name" to name,
                "rol" to rol
            )
            db.collection("users").document(userId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteUser(userId: String): Result<Unit> {
        return try {
            val currentUserId = auth.currentUser?.uid

            // Verificación de seguridad: No permitir borrarse a uno mismo
            if (currentUserId == userId) {
                return Result.failure(Exception("No puedes eliminar tu propia cuenta mientras estás logueado."))
            }

            db.collection("users").document(userId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- NUEVO: Enviar correo de restablecimiento ---
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}