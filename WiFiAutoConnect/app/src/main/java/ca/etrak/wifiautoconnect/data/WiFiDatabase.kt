package ca.etrak.wifiautoconnect.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [WiFiNetwork::class, ConnectionLog::class],
    version = 1,
    exportSchema = false
)
abstract class WiFiDatabase : RoomDatabase() {

    abstract fun wifiDao(): WiFiDao

    companion object {
        @Volatile
        private var INSTANCE: WiFiDatabase? = null

        fun getDatabase(context: Context): WiFiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WiFiDatabase::class.java,
                    "wifi_autoconnect_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
