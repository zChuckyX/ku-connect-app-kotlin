package com.example.ku_connect.data.repository

import com.example.ku_connect.data.model.MarketItem
import com.example.ku_connect.util.AppConfig
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class MarketRepository {
    private val db = FirebaseFirestore.getInstance()

    // ดึงรายการร้านค้าทั้งหมดจาก Firestore (Firebase) เรียงตามเวลาที่สร้างล่าสุด และจำกัดจำนวนที่ดึง
    suspend fun getAllItems(limit: Long = 50): Result<List<MarketItem>> = withContext(Dispatchers.IO) {
        try {
            val snapshot = db.collection(AppConfig.COLLECTION_MARKET)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .await()

            Result.success(snapshot.toObjects(MarketItem::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ค้นหาสินค้าตามชื่อร้านค้า (shopName) จาก Firestore (Firebase)
    suspend fun searchItems(query: String): Result<List<MarketItem>> = withContext(Dispatchers.IO) {
        try {
            val snapshot = db.collection(AppConfig.COLLECTION_MARKET)
                .orderBy("shopName")
                .startAt(query)
                .endAt(query + "\uf8ff")
                .get()
                .await()

            Result.success(snapshot.toObjects(MarketItem::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // เพิ่มร้านค้าใหม่ลงใน Firestore (Firebase) และเพิ่มจำนวนร้านค้าที่ผู้ขายโพสต์ (marketCount)
    suspend fun addItem(item: MarketItem): Result<String> = withContext(Dispatchers.IO) {
        try {
            val ref = db.collection(AppConfig.COLLECTION_MARKET)
                .document()
            ref.set(item.copy(id = ref.id))
                .await()

            val ref2 = db.collection(AppConfig.COLLECTION_USERS).document(item.sellerId)
            ref2.update(
                "marketCount", FieldValue.increment(1)
            )

            Result.success(ref.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}