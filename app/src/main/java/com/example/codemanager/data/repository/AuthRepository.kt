package com.example.codemanager.data.repository

import com.example.codemanager.data.model.User
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.lang.Exception

class AuthRepository {

    // CORRECCIÓN: Usamos getInstance() en lugar de Firebase.auth para evitar el error de KTX
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

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

    // --- MODIFICADO: Crear usuario sin cerrar sesión del Admin ---
    suspend fun createUser(email: String, password: String, name: String, rol: String): Result<User> {
        return try {
            // 1. Obtenemos la configuración de la App actual usando getInstance()
            val mainApp = FirebaseApp.getInstance()
            val options = mainApp.options

            // 2. Definimos un nombre para la app secundaria
            val secondaryAppName = "SecondaryAuthApp"

            // 3. Inicializamos (o recuperamos) la App Secundaria
            val secondaryApp = try {
                FirebaseApp.getInstance(secondaryAppName)
            } catch (e: IllegalStateException) {
                FirebaseApp.initializeApp(mainApp.applicationContext, options, secondaryAppName)
            }

            // 4. Obtenemos la instancia de Auth específica de esa App Secundaria
            val secondaryAuth = FirebaseAuth.getInstance(secondaryApp)

            // 5. Creamos el usuario en la instancia secundaria
            val authResult = secondaryAuth.createUserWithEmailAndPassword(email, password).await()
            val userId = authResult.user?.uid ?: throw Exception("Error al crear usuario en Auth")

            val newUser = User(
                id = userId,
                email = email,
                name = name,
                rol = rol
            )

            // 6. Guardamos en Firestore usando 'db' (la instancia PRINCIPAL del Admin)
            db.collection("users").document(userId).set(newUser).await()

            // 7. Cerramos sesión en la instancia secundaria para limpiar
            secondaryAuth.signOut()

            Result.success(newUser)
        } catch (e: Exception) {
            e.printStackTrace()
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

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}