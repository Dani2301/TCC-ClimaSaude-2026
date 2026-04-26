package com.climasaude.data.repository

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.climasaude.BuildConfig
import com.climasaude.data.database.dao.UserDao
import com.climasaude.data.database.entities.User
import com.climasaude.data.preferences.AppPreferences
import com.climasaude.domain.models.*
import com.climasaude.utils.Resource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val userDao: UserDao,
    private val appPreferences: AppPreferences,
    @param:ApplicationContext private val context: Context
) {

    private val googleSignInClient: GoogleSignInClient by lazy {
        val gsoBuilder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
        
        if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotEmpty()) {
            gsoBuilder.requestIdToken(BuildConfig.GOOGLE_WEB_CLIENT_ID)
        }
        
        GoogleSignIn.getClient(context, gsoBuilder.build())
    }

    val currentUser: FirebaseUser?
        get() = firebaseAuth.currentUser

    suspend fun loginWithEmail(email: String, password: String): Resource<UserProfile> {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = result.user

            if (user != null) {
                val userProfile = withTimeoutOrNull(5000) { getUserProfile(user.uid) }
                    ?: createUserProfile(user.uid, user.email ?: email, "Usuário")
                
                saveUserToLocal(userProfile)
                
                appPreferences.setUserId(user.uid)
                appPreferences.setUserLoggedIn(true)
                Resource.Success(userProfile)
            } else {
                Resource.Error("Falha na autenticação: Usuário nulo")
            }
        } catch (e: Exception) {
            Resource.Error(mapAuthException(e))
        }
    }

    private fun mapAuthException(e: Exception): String {
        Log.e("AuthRepository", "Auth error: ${e.javaClass.simpleName} - ${e.message}")
        return when (e) {
            is com.google.firebase.auth.FirebaseAuthInvalidUserException -> "Usuário não encontrado ou desativado"
            is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> "Senha incorreta ou credenciais inválidas"
            is com.google.firebase.auth.FirebaseAuthUserCollisionException -> "Este email já está em uso"
            is com.google.firebase.FirebaseNetworkException -> "Erro de conexão com a internet"
            is com.google.android.gms.common.api.ApiException -> "Erro de API Google (${e.statusCode}): Verifique as chaves SHA-1"
            else -> e.message ?: "Ocorreu um erro inesperado na autenticação"
        }
    }

    suspend fun registerWithEmail(
        email: String,
        password: String,
        name: String
    ): Resource<UserProfile> {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user

            if (user != null) {
                val userProfile = createUserProfile(user.uid, email, name)
                
                saveUserToFirestore(userProfile)
                saveUserToLocal(userProfile)

                appPreferences.setUserId(user.uid)
                appPreferences.setUserLoggedIn(true)

                Resource.Success(userProfile)
            } else {
                Resource.Error("Falha ao criar conta no servidor")
            }
        } catch (e: Exception) {
            Resource.Error(mapAuthException(e))
        }
    }

    suspend fun loginWithGoogle(idToken: String): Resource<UserProfile> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = firebaseAuth.signInWithCredential(credential).await()
            val user = result.user

            if (user != null) {
                var userProfile = withTimeoutOrNull(5000) { getUserProfile(user.uid) }

                if (userProfile == null) {
                    userProfile = createUserProfile(
                        user.uid,
                        user.email ?: "",
                        user.displayName ?: "Usuário",
                        user.photoUrl?.toString()
                    )
                    saveUserToFirestore(userProfile)
                }

                saveUserToLocal(userProfile)
                appPreferences.setUserId(user.uid)
                appPreferences.setUserLoggedIn(true)

                Resource.Success(userProfile)
            } else {
                Resource.Error("Falha na autenticação com Google")
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Google sign in error", e)
            Resource.Error(mapAuthException(e))
        }
    }

    suspend fun getCurrentUserProfile(): UserProfile? {
        val uid = firebaseAuth.currentUser?.uid ?: return null
        return withTimeoutOrNull(5000) { getUserProfile(uid) }
    }

    suspend fun resetPassword(email: String): Resource<String> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            Resource.Success("Email de recuperação enviado com sucesso")
        } catch (e: Exception) {
            Resource.Error(mapAuthException(e))
        }
    }

    suspend fun logout(): Resource<String> {
        return try {
            firebaseAuth.signOut()
            if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotEmpty()) {
                googleSignInClient.signOut().await()
            }

            appPreferences.setUserId("")
            appPreferences.setUserLoggedIn(false)
            appPreferences.clearBiometricEnabled()

            Resource.Success("Sessão encerrada")
        } catch (e: Exception) {
            Resource.Error("Erro ao encerrar sessão: ${e.message}")
        }
    }

    suspend fun deleteAccount(): Resource<String> {
        return try {
            val userId = currentUser?.uid
            if (userId != null) {
                withTimeoutOrNull(5000) {
                    firestore.collection("users").document(userId).delete().await()
                }
                val localUser = userDao.getUserById(userId)
                if (localUser != null) {
                    userDao.deleteUser(localUser)
                }
                currentUser?.delete()?.await()
                appPreferences.clearAll()
                Resource.Success("Conta excluída permanentemente")
            } else {
                Resource.Error("Usuário não autenticado")
            }
        } catch (e: Exception) {
            Resource.Error("Falha ao excluir conta: ${e.message}")
        }
    }

    private suspend fun getUserProfile(userId: String): UserProfile? {
        return try {
            val doc = firestore.collection("users").document(userId).get().await()
            if (doc.exists()) {
                doc.toObject(UserProfile::class.java)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun createUserProfile(
        userId: String,
        email: String,
        name: String,
        photoUrl: String? = null
    ): UserProfile {
        return UserProfile(
            id = userId,
            email = email,
            name = name,
            photoUrl = photoUrl,
            preferences = UserPreferences(
                notifications = NotificationPreferences(),
                privacy = PrivacySettings(),
                location = LocationSettings()
            )
        )
    }

    private suspend fun saveUserToFirestore(userProfile: UserProfile) {
        try {
            // Adicionado timeout para evitar travamentos em redes instáveis ou primeiro login
            withTimeoutOrNull(8000) {
                firestore.collection("users")
                    .document(userProfile.id)
                    .set(userProfile)
                    .await()
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Erro ao salvar perfil no Firestore", e)
        }
    }

    private suspend fun saveUserToLocal(userProfile: UserProfile) {
        val user = User(
            id = userProfile.id,
            email = userProfile.email,
            name = userProfile.name,
            photoUrl = userProfile.photoUrl,
            birthDate = userProfile.birthDate,
            gender = userProfile.gender,
            weight = userProfile.weight,
            height = userProfile.height,
            medicalConditions = userProfile.medicalConditions.map { it.name },
            allergies = userProfile.allergies.map { it.name },
            theme = userProfile.preferences?.theme ?: "auto",
            language = userProfile.preferences?.language ?: "pt-BR"
        )
        userDao.insertUser(user)
    }

    private fun convertToUserProfile(user: User): UserProfile {
        return UserProfile(
            id = user.id,
            email = user.email,
            name = user.name,
            photoUrl = user.photoUrl,
            birthDate = user.birthDate,
            age = user.birthDate?.let { calculateAge(it) },
            gender = user.gender,
            weight = user.weight,
            height = user.height,
            bmi = if (user.weight != null && user.height != null) {
                calculateBMI(user.weight, user.height)
            } else null,
            medicalConditions = user.medicalConditions.map { condition ->
                MedicalCondition(
                    id = UUID.randomUUID().toString(),
                    name = condition,
                    severity = "moderate",
                    diagnosedDate = null,
                    isWeatherSensitive = listOf("artrite", "enxaqueca", "asma", "rinite")
                        .any { condition.contains(it, ignoreCase = true) }
                )
            },
            allergies = user.allergies.map { allergy ->
                Allergy(
                    id = UUID.randomUUID().toString(),
                    name = allergy,
                    severity = "moderate",
                    season = null
                )
            },
            preferences = UserPreferences(
                theme = user.theme,
                language = user.language,
                notifications = NotificationPreferences(
                    weatherAlerts = user.notificationPreferences["weatherAlerts"] ?: true,
                    medicationReminders = user.notificationPreferences["medicationReminders"] ?: true,
                    healthTips = user.notificationPreferences["healthTips"] ?: true,
                    emergencyAlerts = user.notificationPreferences["emergencyAlerts"] ?: true,
                    dailyReports = user.notificationPreferences["dailyReports"] ?: false
                ),
                privacy = PrivacySettings(
                    shareLocation = user.privacySettings["shareLocation"] ?: true,
                    shareHealthData = user.privacySettings["shareHealthData"] ?: false,
                    analyticsEnabled = user.privacySettings["analyticsEnabled"] ?: true,
                    crashReportsEnabled = user.privacySettings["crashReportsEnabled"] ?: true
                ),
                location = LocationSettings()
            ),
            isComplete = calculateIsProfileComplete(user)
        )
    }

    private fun convertToUser(userProfile: UserProfile): User {
        val prefs = userProfile.preferences ?: UserPreferences()
        return User(
            id = userProfile.id,
            email = userProfile.email,
            name = userProfile.name,
            photoUrl = userProfile.photoUrl,
            birthDate = userProfile.birthDate,
            gender = userProfile.gender,
            weight = userProfile.weight,
            height = userProfile.height,
            medicalConditions = userProfile.medicalConditions.map { it.name },
            allergies = userProfile.allergies.map { it.name },
            theme = prefs.theme,
            language = prefs.language,
            notificationPreferences = mapOf(
                "weatherAlerts" to prefs.notifications.weatherAlerts,
                "medicationReminders" to prefs.notifications.medicationReminders,
                "healthTips" to prefs.notifications.healthTips,
                "emergencyAlerts" to prefs.notifications.emergencyAlerts,
                "dailyReports" to prefs.notifications.dailyReports
            ),
            privacySettings = mapOf(
                "shareLocation" to prefs.privacy.shareLocation,
                "shareHealthData" to prefs.privacy.shareHealthData,
                "analyticsEnabled" to prefs.privacy.analyticsEnabled,
                "crashReportsEnabled" to prefs.privacy.crashReportsEnabled
            )
        )
    }
    
    private fun calculateAge(birthDate: Date): Int {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        calendar.time = birthDate
        val birthYear = calendar.get(Calendar.YEAR)
        return currentYear - birthYear
    }
    
    private fun calculateBMI(weight: Float, height: Float): Float {
        val heightInMeters = height / 100
        return weight / (heightInMeters * heightInMeters)
    }
    
    private fun calculateIsProfileComplete(user: User): Boolean {
        return user.name.isNotBlank() &&
                user.email.isNotBlank() &&
                user.birthDate != null &&
                user.gender != null &&
                user.medicalConditions.isNotEmpty()
    }
}
