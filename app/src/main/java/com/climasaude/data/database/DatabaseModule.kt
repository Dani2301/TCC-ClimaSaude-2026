package com.climasaude.data.database

import android.content.Context
import com.climasaude.data.database.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    fun provideUserDao(database: AppDatabase): UserDao {
        return database.userDao()
    }

    @Provides
    fun provideWeatherDataDao(database: AppDatabase): WeatherDataDao {
        return database.weatherDataDao()
    }

    @Provides
    fun provideHealthAlertDao(database: AppDatabase): HealthAlertDao {
        return database.healthAlertDao()
    }

    @Provides
    fun provideMedicationDao(database: AppDatabase): MedicationDao {
        return database.medicationDao()
    }

    @Provides
    fun provideMedicationLogDao(database: AppDatabase): MedicationLogDao {
        return database.medicationLogDao()
    }

    @Provides
    fun provideEmergencyContactDao(database: AppDatabase): EmergencyContactDao {
        return database.emergencyContactDao()
    }

    @Provides
    fun provideSymptomDao(database: AppDatabase): SymptomDao {
        return database.symptomDao()
    }
}