package com.example.securevault.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.securevault.data.entity.AuthEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Authentication Credentials (Schema 1).
 */
@Dao
interface AuthDao {

    @Query("SELECT * FROM app_auth WHERE id = 1 LIMIT 1")
    fun getAuthFlow(): Flow<AuthEntity?>

    @Query("SELECT * FROM app_auth WHERE id = 1 LIMIT 1")
    suspend fun getAuth(): AuthEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setAuth(auth: AuthEntity)

    @Query("DELETE FROM app_auth")
    suspend fun clearAuth()
}
