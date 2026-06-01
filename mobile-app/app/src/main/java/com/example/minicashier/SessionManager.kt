package com.example.minicashier

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.dataStore by preferencesDataStore("session")

class SessionManager(
    private val context: Context
) {

    companion object {
        val USER_ID = stringPreferencesKey("user_id")
        val NAME = stringPreferencesKey("name")
        val USERNAME = stringPreferencesKey("username")
        val ROLE = stringPreferencesKey("role")
    }

    suspend fun saveLogin(
        userId: Int,
        name: String,
        username: String,
        role: String
    ) {
        context.dataStore.edit {
            it[USER_ID] = userId.toString()
            it[NAME] = name
            it[USERNAME] = username
            it[ROLE] = role
        }
    }

    suspend fun getUser(): UserData? {
        val pref = context.dataStore.data.first()

        val userId = pref[USER_ID]
        val name = pref[NAME]
        val username = pref[USERNAME]
        val role = pref[ROLE]

        return if (
            username != null &&
            role != null
        ) {
            UserData(
                id = userId?.toIntOrNull() ?: 0,
                name = name ?: "-",
                username = username,
                role = role
            )
        } else {
            null
        }
    }

    suspend fun logout() {
        context.dataStore.edit {
            it.clear()
        }
    }
}