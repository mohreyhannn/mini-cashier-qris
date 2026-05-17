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

        val USERNAME = stringPreferencesKey("username")
        val ROLE = stringPreferencesKey("role")
    }

    suspend fun saveLogin(
        username: String,
        role: String
    ) {

        context.dataStore.edit {

            it[USERNAME] = username
            it[ROLE] = role
        }
    }

    suspend fun getUser(): UserData? {

        val pref = context.dataStore.data.first()

        val username = pref[USERNAME]
        val role = pref[ROLE]

        return if (
            username != null &&
            role != null
        ) {

            UserData(
                id = 0,
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