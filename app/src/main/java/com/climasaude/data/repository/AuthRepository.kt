package com.climasaude.data.repository

import android.content.Context
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
        // QA Alert: Login de teste removido em builds de produção para maior segurança
        if (BuildConfig.DEBUG && email == "admin@climasaude.com.br" && password == "admin123") {
            val mockUser = createUserProfile("admin_id", email, "Usuário Administrador")
            appPreferences.setUserId("admin_id")
            appPreferences.setUserLoggedIn(true)
            return Resource.Success(mockUser)
        }

        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = result.user

            if (user != null) {
                val userProfile = getUserProfile(user.uid)
                if (userProfile != null) {
                    appPreferences.setUserId(user.uid)
                    appPreferences.setUserLoggedIn(true)
                    Resource.Success(userProfile)
                } else {
                    Resource.Error("Perfil do usuário não encontrado no banco de dados")
                }
            } else {
                Resource.Error("Falha na autenticação: Usuário nulo")
            }
        } catch (e: Exception) {
            Resource.Error(mapAuthException(e))
        }
    }

    private fun mapAuthException(e: Exception): String {
        return when (e) {
            is com.google.firebase.auth.FirebaseAuthInvalidUserException -> "Usuário não encontrado ou desativado"
            is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> "Senha incorreta ou formato de email inválido"
            is com.google.firebase.auth.FirebaseAuthUserCollisionException -> "Este email já está em uso"
            is com.google.firebase.FirebaseNetworkException -> "Erro de conexão com a internet"
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
                Resource.Error("Falha ao criar conta")
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
                var userProfile = getUserProfile(user.uid)

                if (userProfile == null) {
                    userProfile = createUserProfile(
                        user.uid,
                        user.email ?: "",
                        user.displayName ?: "",
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
            Resource.Error(mapAuthException(e))
        }
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
                firestore.collection("users").document(userId).delete().await()

                val localUser = userDao.getUserById(userId)
                if (localUser != null) {
                    userDao.deleteUser(localUser)
                }

                currentUser?.delete()?.await()

                appPreferences.clearAll()

                Resource.Success("Conta excluída permanentemente")
            } else {
                Resource.Error("Usuário não autenticado para exclusão")
            }
        } catch (e: Exception) {
            Resource.Error("Falha ao excluir conta: ${e.message}")
        }
    }

    private suspend fun getUserProfile(userId: String): UserProfile? {
        if (userId == "admin_id") {
            return createUserProfile("admin_id", "admin@climasaude.com.br", "Usuário Administrador")
        }
        return try {
            val doc = firestore.collection("users").document(userId).get().await()
            if (doc.exists()) {
                doc.toObject(UserProfile::class.java)
            } else {
                val localUser = userDao.getUserById(userId)
                localUser?.let { convertToUserProfile(it) }
            }
        } catch (_: Exception) {
            val localUser = userDao.getUserById(userId)
            localUser?.let { convertToUserProfile(it) }
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
        if (userProfile.id == "admin_id") return
        try {
            firestore.collection("users")
                .document(userProfile.id)
                .set(userProfile)
                .await()
        } catch (_: Exception) {
        }
    }

    private suspend fun saveUserToLocal(userProfile: UserProfile) {
        if (userProfile.id == "admin_id") return
        val user = convertToUser(userProfile)
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
            theme = userProfile.preferences.theme,
            language = userProfile.preferences.language,
            notificationPreferences = mapOf(
                "weatherAlerts" to userProfile.preferences.notifications.weatherAlerts,
                "medicationReminders" to userProfile.preferences.notifications.medicationReminders,
                "healthTips" to userProfile.preferences.notifications.healthTips,
                "emergencyAlerts" to userProfile.preferences.notifications.emergencyAlerts,
                "dailyReports" to userProfile.preferences.notifications.dailyReports
            ),
            privacySettings = mapOf(
                "shareLocation" to userProfile.preferences.privacy.shareLocation,
                "shareHealthData" to userProfile.preferences.privacy.shareHealthData,
                "analyticsEnabled" to userProfile.preferences.privacy.analyticsEnabled,
                "crashReportsEnabled" to userProfile.preferences.privacy.crashReportsEnabled
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
