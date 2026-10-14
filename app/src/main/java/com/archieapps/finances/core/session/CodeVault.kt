package com.archieapps.finances.core.session

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import java.security.KeyStore
import java.time.Clock
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private val Context.vaultDataStore: DataStore<Preferences> by preferencesDataStore("finances-vault")

enum class VaultMode { Keystore, MemoryOnly }

data class CodeMemo(val code: String, val validFor: String)

class CodeVault(context: Context) {
    private val store = context.vaultDataStore

    @Volatile
    private var memo: CodeMemo? = null

    @Volatile
    var mode: VaultMode = VaultMode.Keystore
        private set

    private val _budget = MutableStateFlow(StrikeBudget())
    val budget: StateFlow<StrikeBudget> = _budget.asStateFlow()

    fun currentCode(clock: Clock = Clock.system(DayCode.SERVER_ZONE)): String? {
        val current = memo ?: return null

        if (!DayCode.isValidFor(current.validFor, clock)) {
            memo = null

            return null
        }

        return current.code
    }

    val unlocked: Boolean
        get() = currentCode() != null

    suspend fun storedValidForToday(clock: Clock = Clock.system(DayCode.SERVER_ZONE)): Boolean {
        val prefs = store.data.first()

        return prefs[CIPHER] != null &&
            prefs[IV] != null &&
            DayCode.isValidFor(prefs[VALID_FOR], clock)
    }

    suspend fun decryptCipher(): Cipher? {
        val prefs = store.data.first()
        val iv = prefs[IV]?.let { decode(it) } ?: return null

        return try {
            Cipher.getInstance(TRANSFORMATION).apply {
                init(Cipher.DECRYPT_MODE, key() ?: return null, GCMParameterSpec(TAG_BITS, iv))
            }
        } catch (invalidated: KeyPermanentlyInvalidatedException) {
            dropKey()
            clear()
            null
        } catch (failure: Exception) {
            clear()
            null
        }
    }

    fun encryptCipher(): Cipher? = try {
        Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, key() ?: return null)
        }
    } catch (invalidated: KeyPermanentlyInvalidatedException) {
        dropKey()
        null
    } catch (failure: Exception) {
        null
    }

    suspend fun openWith(cipher: Cipher): Boolean {
        val prefs = store.data.first()
        val payload = prefs[CIPHER]?.let { decode(it) } ?: return false
        val validFor = prefs[VALID_FOR] ?: return false

        return try {
            val plain = String(cipher.doFinal(payload))
            memo = CodeMemo(plain, validFor)
            true
        } catch (failure: Exception) {
            clear()
            false
        }
    }

    suspend fun save(code: String, cipher: Cipher, clock: Clock = Clock.system(DayCode.SERVER_ZONE)) {
        val validFor = DayCode.serverToday(clock).toString()

        try {
            val payload = cipher.doFinal(code.toByteArray())
            store.edit {
                it[CIPHER] = encode(payload)
                it[IV] = encode(cipher.iv)
                it[VALID_FOR] = validFor
            }
            memo = CodeMemo(code, validFor)
        } catch (failure: Exception) {
            keepInMemory(code, clock)
        }
    }

    fun keepInMemory(code: String, clock: Clock = Clock.system(DayCode.SERVER_ZONE)) {
        mode = VaultMode.MemoryOnly
        memo = CodeMemo(code, DayCode.serverToday(clock).toString())
    }

    suspend fun clear() {
        memo = null
        store.edit { it.clear() }
    }

    fun forgetMemo() {
        memo = null
    }

    suspend fun forget() {
        dropKey()
        clear()
    }

    suspend fun onRejected(attemptsLeft: Int?) {
        _budget.update { it.afterRejection(attemptsLeft) }
        clear()
    }

    fun onValidated() {
        _budget.update { it.afterSuccess() }
    }

    fun setServerBlocked(value: Int) {
        _budget.update { it.withServerBlocked(value) }
    }

    fun markMemoryOnly() {
        mode = VaultMode.MemoryOnly
    }

    private fun key(): SecretKey? = try {
        val keystore = KeyStore.getInstance(PROVIDER).apply { load(null) }
        val existing = keystore.getKey(ALIAS, null) as? SecretKey

        existing ?: generate()
    } catch (failure: Exception) {
        null
    }

    private fun generate(): SecretKey? = try {
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
        val spec = KeyGenParameterSpec.Builder(
            ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true)
            .setUnlockedDeviceRequired(true)
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    setUserAuthenticationParameters(
                        0,
                        KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL,
                    )
                } else {
                    @Suppress("DEPRECATION")
                    setUserAuthenticationValidityDurationSeconds(-1)
                }
            }
            .build()

        generator.init(spec)
        generator.generateKey()
    } catch (failure: Exception) {
        mode = VaultMode.MemoryOnly
        null
    }

    private fun dropKey() {
        runCatching {
            KeyStore.getInstance(PROVIDER).apply { load(null) }.deleteEntry(ALIAS)
        }
    }

    private fun encode(value: ByteArray): String = Base64.getEncoder().encodeToString(value)

    private fun decode(value: String): ByteArray = Base64.getDecoder().decode(value)

    companion object {
        private const val PROVIDER = "AndroidKeyStore"
        private const val ALIAS = "finances.code.v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val TAG_BITS = 128

        private val CIPHER = stringPreferencesKey("code_cipher")
        private val IV = stringPreferencesKey("code_iv")
        private val VALID_FOR = stringPreferencesKey("code_valid_for")
    }
}
