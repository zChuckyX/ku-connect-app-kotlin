package com.example.ku_connect.data.repository

import com.example.ku_connect.data.model.User
import com.example.ku_connect.util.AppConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseFirestore.getInstance()
    val currentUserId: String? get() = auth.currentUser?.uid

    // สมัครสมาชิกใหม่ด้วย email/password แล้วบันทึกข้อมูลผู้ใช้ลง Firestore (Firebase)
    suspend fun register(username: String, email: String, password: String, profileColor: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            val authResult = auth
                .createUserWithEmailAndPassword(email, password)
                .await()
            val uid = authResult.user?.uid ?: throw Exception("ไม่สามารถสร้างบัญชีได้")

            val user = User(
                uid          = uid,
                username     = username,
                email        = email,
                profileColor = profileColor
            )

            db.collection(AppConfig.COLLECTION_USERS).document(uid).set(user).await()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // เข้าสู่ระบบด้วย email/password และดึงข้อมูลผู้ใช้จาก Firestore (Firebase)
    suspend fun login(email: String, password: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            val snapshot = db.collection(AppConfig.COLLECTION_USERS)
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .await()

            if (snapshot.isEmpty) return@withContext Result.failure(Exception("ไม่พบบัญชีผู้ใช้นี้"))

            val user = snapshot.documents[0].toObject(User::class.java)
                ?: return@withContext Result.failure(Exception("ข้อมูลผิดพลาด"))

            auth.signInWithEmailAndPassword(email, password).await()

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ดึงข้อมูลผู้ใช้ปัจจุบันจาก Firestore (Firebase) ตาม USER_ID ที่ล็อกอินอยู่
    suspend fun getCurrentUser(): User? = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext null

        db.collection(AppConfig.COLLECTION_USERS)
            .document(uid)
            .get()
            .await()
            .toObject(User::class.java)
    }

    // อัปเดต username และ profileColor ของผู้ใช้ปัจจุบัน
    suspend fun updateUsername(username: String, profileColor: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val uid = currentUserId ?: throw Exception("ไม่ได้เข้าสู่ระบบ")

            db.collection(AppConfig.COLLECTION_USERS)
                .document(uid)
                .update("username", username, "profileColor", profileColor)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ออกจากระบบผู้ใช้ปัจจุบัน
    fun logout() = auth.signOut()
}