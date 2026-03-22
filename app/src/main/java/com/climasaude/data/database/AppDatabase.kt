package com.climasaude.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.climasaude.data.database.entities.*
import com.climasaude.data.database.dao.*
import com.climasaude.utils.Converters

@Database(
    entities = [
        User::class,
        WeatherData::class,
        HealthAlert::class,
        Medication::class,
        MedicationLog::class,
        EmergencyContact::class,
        Symptom::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun weatherDataDao(): WeatherDataDao
    abstract fun healthAlertDao(): HealthAlertDao
    abstract fun medicationDao(): MedicationDao
    abstract fun medicationLogDao(): MedicationLogDao
    abstract fun emergencyContactDao(): EmergencyContactDao
    abstract fun symptomDao(): SymptomDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            // Garantir thread-safety e evitar recriação desnecessária. Modificado por: Daniel
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "clima_saude_database"
                )
                    // fallbackToDestructiveMigration avisa que dados serão perdidos em updates de versão. 
                    // Em produção, deve-se usar .addMigrations(). Modificado por: Daniel
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
