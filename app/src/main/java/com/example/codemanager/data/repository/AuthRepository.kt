package com.example.codemanager.data.repository

import com.example.codemanager.data.model.User
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.lang.Exception

class AuthRepository {

    // Instancias
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    // --- LECTURA DE ROL (NUEVO) ---
    // Esta función es vital para que el MainActivity sepa si es Admin o Usuario rápidamente
    suspend fun getUserRole(): String {
        return try {
            val userId = auth.currentUser?.uid ?: return "Usuario"
            val document = db.collection("users").document(userId).get().await()
            // Retorna el campo "rol" o "Usuario" si no existe
            document.getString("rol") ?: "Usuario"
        } catch (e: Exception) {
            "Usuario"
        }
    }

    // Iniciar sesión
    suspend fun signIn(email: String, password: String): Result<Unit> {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Obtener usuario actual (Objeto completo)
    suspend fun getCurrentUser(): User? {
        return auth.currentUser?.let { firebaseUser ->
            try {
                val document = db.collection("users").document(firebaseUser.uid).get().await()
                if (document.exists()) {
                    document.toObject(User::class.java)
                } else {
                    // Crear usuario por defecto si no existe
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

    // Crear usuario secundario (sin cerrar sesión del Admin)
    suspend fun createUser(email: String, password: String, name: String, rol: String): Result<User> {
        return try {
            val mainApp = FirebaseApp.getInstance()
            val options = mainApp.options
            val secondaryAppName = "SecondaryAuthApp"

            val secondaryApp = try {
                FirebaseApp.getInstance(secondaryAppName)
            } catch (e: IllegalStateException) {
                FirebaseApp.initializeApp(mainApp.applicationContext, options, secondaryAppName)
            }

            val secondaryAuth = FirebaseAuth.getInstance(secondaryApp)
            val authResult = secondaryAuth.createUserWithEmailAndPassword(email, password).await()
            val userId = authResult.user?.uid ?: throw Exception("Error al crear usuario en Auth")

            val newUser = User(
                id = userId,
                email = email,
                name = name,
                rol = rol
            )

            db.collection("users").document(userId).set(newUser).await()
            secondaryAuth.signOut()

            Result.success(newUser)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun updateUser(userId: String, name: String, rol: String): Result<Unit> {
        return try {
            val updates = mapOf("name" to name, "rol" to rol)
            db.collection("users").document(userId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteUser(userId: String): Result<Unit> {
        return try {
            val currentUserId = auth.currentUser?.uid
            if (currentUserId == userId) {
                return Result.failure(Exception("No puedes eliminar tu propia cuenta."))
            }
            db.collection("users").document(userId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}