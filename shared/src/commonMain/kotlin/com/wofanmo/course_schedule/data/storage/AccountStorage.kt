package com.wofanmo.course_schedule.data.storage

import com.russhwolf.settings.Settings
import com.wofanmo.course_schedule.data.crypto.Crypto
import com.wofanmo.course_schedule.data.model.Account
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AccountStorage(private val settings: Settings) {
    private val json = Json { ignoreUnknownKeys = true }

    fun getAll(): List<Account> {
        val accountsString = settings.getStringOrNull(StorageKeys.ACCOUNTS) ?: return emptyList()
        return try {
            val accounts = json.decodeFromString<List<EncryptedAccount>>(accountsString)
            accounts.map { it.toAccount() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(account: Account) {
        val accounts = getAll().toMutableList()
        val index = accounts.indexOfFirst { it.id == account.id }

        val encryptedAccount = EncryptedAccount.fromAccount(account)

        if (index >= 0) {
            accounts[index] = encryptedAccount.toAccount()
        } else {
            accounts.add(encryptedAccount.toAccount())
        }

        val encryptedAccounts = accounts.map { EncryptedAccount.fromAccount(it) }
        val accountsString = json.encodeToString(encryptedAccounts)
        settings.putString(StorageKeys.ACCOUNTS, accountsString)
    }

    fun delete(id: String) {
        val accounts = getAll().toMutableList()
        accounts.removeAll { it.id == id }
        val encryptedAccounts = accounts.map { EncryptedAccount.fromAccount(it) }
        val accountsString = json.encodeToString(encryptedAccounts)
        settings.putString(StorageKeys.ACCOUNTS, accountsString)
    }

    // 内部类用于加密存储
    @kotlinx.serialization.Serializable
    private data class EncryptedAccount(
        val id: String,
        val platform: String,
        val username: String,
        val encryptedPassword: String,
        val url: String
    ) {
        fun toAccount(): Account {
            val decryptedPassword = try {
                Crypto.decrypt(encryptedPassword)
            } catch (e: Exception) {
                ""
            }
            return Account(
                id = id,
                platform = platform,
                username = username,
                password = decryptedPassword,
                url = url
            )
        }

        companion object {
            fun fromAccount(account: Account): EncryptedAccount {
                val encryptedPassword = try {
                    Crypto.encrypt(account.password)
                } catch (e: Exception) {
                    account.password
                }
                return EncryptedAccount(
                    id = account.id,
                    platform = account.platform,
                    username = account.username,
                    encryptedPassword = encryptedPassword,
                    url = account.url
                )
            }
        }
    }
}
