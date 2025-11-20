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

    // Método para iniciar sesión - nombre corregido a signIn
    suspend fun signIn(email: String, password: String): Result<Unit> {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Método para obtener el usuario actual - nombre corregido a getCurrentUser
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
                        rol = "user"
                    )
                    db.collection("users").document(firebaseUser.uid).set(newUser).await()
                    newUser
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    // Método para verificar si hay usuario logueado
    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    // Método para cerrar sesión - nombre corregido a signOut
    fun signOut() {
        auth.signOut()
    }

    // Métodos adicionales para gestión de usuarios (opcionales para el admin)

    // Método para obtener todos los usuarios (solo para admins)
    suspend fun getAllUsers(): List<User> {
        return try {
            val snapshot = db.collection("users").get().await()
            snapshot.documents.map { document ->
                document.toObject(User::class.java) ?: User()
            }.filter { it.id.isNotEmpty() } // Filtrar usuarios válidos
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Método para crear usuario (solo para admins)
    suspend fun createUser(email: String, password: String, name: String, rol: String): Result<User> {
        return try {
            // Crear usuario en Firebase Auth
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val userId = authResult.user?.uid ?: throw Exception("Error al crear usuario")

            // Crear documento en Firestore
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

    // Método para actualizar usuario
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

    // Método para eliminar usuario
    suspend fun deleteUser(userId: String): Result<Unit> {
        return try {
            // IMPORTANTE: En producción, deberías eliminar también de Firebase Auth
            // y manejar la seguridad con Firebase Rules
            db.collection("users").document(userId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}