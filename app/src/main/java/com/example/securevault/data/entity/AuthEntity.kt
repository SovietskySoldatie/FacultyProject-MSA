package com.example.securevault.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Database Schema 1: Application Authentication Credentials.
 * Stores the master login password for the single-user local vault.
 */
@Entity(tableName = "app_auth")
data class AuthEntity(
    @PrimaryKey
    val id: Int = 1,

    @ColumnInfo(name = "password")
    val password: String
)
