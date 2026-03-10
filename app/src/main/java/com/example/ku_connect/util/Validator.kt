package com.example.ku_connect.util

object Validator {
    fun isValidKuEmail(email: String): Boolean {
        if (email.isBlank()) return false
        val trimmed = email.trim().lowercase()
        return trimmed.endsWith(AppConfig.ALLOWED_EMAIL_DOMAIN) &&
                android.util.Patterns.EMAIL_ADDRESS.matcher(trimmed).matches()
    }

    fun isValidPassword(password: String): PasswordResult {
        if (password.length < AppConfig.MIN_PASSWORD_LENGTH)
            return PasswordResult.TooShort
        if (!password.any { it.isUpperCase() })
            return PasswordResult.NoUppercase
        if (!password.any { it.isLowerCase() })
            return PasswordResult.NoLowercase
        if (!password.any { it.isDigit() })
            return PasswordResult.NoDigit
        if (!password.any { !it.isLetterOrDigit() })
            return PasswordResult.NoSpecialChar
        return PasswordResult.Valid
    }

    fun isValidUsername(username: String): Boolean {
        if (username.isBlank()) return false
        val trimmed = username.trim()
        return trimmed.length in 3..30 && trimmed.matches(Regex("^[a-zA-Z0-9_ก-๙ ]+$"))
    }

    sealed class PasswordResult {
        object Valid : PasswordResult()
        object TooShort : PasswordResult()
        object NoUppercase : PasswordResult()
        object NoLowercase : PasswordResult()
        object NoDigit : PasswordResult()
        object NoSpecialChar : PasswordResult()

        fun message(): String = when (this) {
            Valid -> ""
            TooShort -> "รหัสผ่านต้องมีอย่างน้อย 8 ตัวอักษร"
            NoUppercase -> "ต้องมีตัวพิมพ์ใหญ่อย่างน้อย 1 ตัว"
            NoLowercase -> "ต้องมีตัวพิมพ์เล็กอย่างน้อย 1 ตัว"
            NoDigit -> "ต้องมีตัวเลขอย่างน้อย 1 ตัว"
            NoSpecialChar -> "ต้องมีอักขระพิเศษอย่างน้อย 1 ตัว (เช่น @, #, !)"
        }
    }
}