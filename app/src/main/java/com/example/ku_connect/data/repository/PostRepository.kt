package com.example.ku_connect.data.repository

import com.example.ku_connect.data.model.Comment
import com.example.ku_connect.data.model.Post
import com.example.ku_connect.util.AppConfig
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PostRepository {
    private val db = FirebaseFirestore.getInstance()

    // ดึงโพสต์ยอดนิยม เรียงตามจำนวนไลค์และคอมเมนต์จากมากไปน้อย
    suspend fun getPopularPosts(limit: Long = 20): Result<List<Post>> = withContext(Dispatchers.IO) {
        try {
            val snapshot = db.collection(AppConfig.COLLECTION_POSTS)
                .orderBy("likeCount", Query.Direction.DESCENDING)
                .orderBy("commentCount", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .await()

            Result.success(snapshot.toObjects(Post::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ดึงโพสต์ทั้งหมดเรียงตามเวลาที่สร้างล่าสุด และ จำกัดจำนวนที่ดึง
    suspend fun getAllPosts(limit: Long = 50): Result<List<Post>> = withContext(Dispatchers.IO) {
        try {
            val snapshot = db.collection(AppConfig.COLLECTION_POSTS)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .await()

            Result.success(snapshot.toObjects(Post::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ค้นหาโพสต์ตามชื่อหัวข้อ (title)
    suspend fun searchPosts(query: String): Result<List<Post>> = withContext(Dispatchers.IO) {
        try {
            val snapshot = db.collection(AppConfig.COLLECTION_POSTS)
                .orderBy("title")
                .startAt(query)
                .endAt(query + "\uf8ff")
                .get()
                .await()

            Result.success(snapshot.toObjects(Post::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // สร้างโพสต์ใหม่และเพิ่มจำนวนโพสต์ของผู้เขียน
    suspend fun createPost(post: Post): Result<String> = withContext(Dispatchers.IO) {
        try {
            val ref = db.collection(AppConfig.COLLECTION_POSTS).document()
            ref.set(post.copy(id = ref.id)).await()

            val ref2 = db.collection(AppConfig.COLLECTION_USERS).document(post.authorId)
            ref2.update(
                "postCount", FieldValue.increment(1)
            )

            Result.success(ref.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // กดไลค์หรือยกเลิกไลค์โพสต์ พร้อมอัปเดตจำนวนไลค์
    suspend fun toggleLike(postId: String, userId: String, liked: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val ref = db.collection(AppConfig.COLLECTION_POSTS).document(postId)

            if (liked) {
                ref.update(
                    "likedBy", FieldValue.arrayUnion(userId),
                    "likeCount", FieldValue.increment(1)
                ).await()
            } else {
                ref.update(
                    "likedBy", FieldValue.arrayRemove(userId),
                    "likeCount", FieldValue.increment(-1)
                ).await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ดึงคอมเมนต์ทั้งหมดของโพสต์ เรียงตามเวลาที่โพสต์
    suspend fun getComments(postId: String): Result<List<Comment>> = withContext(Dispatchers.IO) {
        try {
            val snapshot = db.collection(AppConfig.COLLECTION_POSTS)
                .document(postId)
                .collection(AppConfig.COLLECTION_COMMENTS)
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .get()
                .await()

            Result.success(snapshot.toObjects(Comment::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // เพิ่มคอมเมนต์ใหม่ให้โพสต์ และเพิ่มจำนวนคอมเมนต์
    suspend fun addComment(postId: String, comment: Comment): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val ref = db.collection(AppConfig.COLLECTION_POSTS)
                .document(postId)
                .collection(AppConfig.COLLECTION_COMMENTS)
                .document()

            ref.set(comment.copy(id = ref.id)).await()

            db.collection(AppConfig.COLLECTION_POSTS)
                .document(postId)
                .update("commentCount", FieldValue.increment(1))
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ดึงโพสต์ทั้งหมดของผู้ใช้ตาม authorId (USER_ID)
    suspend fun getMyPosts(userId: String): Result<List<Post>> = withContext(Dispatchers.IO) {
        try {
            val snapshot = db.collection(AppConfig.COLLECTION_POSTS)
                .whereEqualTo("authorId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            Result.success(snapshot.toObjects(Post::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // แก้ไขข้อมูลโพสต์ (title และ content)
    suspend fun updatePost(postId: String, title: String, content: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            db.collection(AppConfig.COLLECTION_POSTS)
                .document(postId)
                .update(mapOf("title" to title, "content" to content))
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ลบโพสต์และคอมเมนต์ทั้งหมดของโพสต์นั้น พร้อมลดจำนวนโพสต์ของผู้ใช้
    suspend fun deletePost(postId: String, userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val comments = db.collection(AppConfig.COLLECTION_POSTS)
                .document(postId)
                .collection(AppConfig.COLLECTION_COMMENTS)
                .get().await()
            val batch = db.batch()
            comments.documents.forEach { batch.delete(it.reference) }
            batch.delete(db.collection(AppConfig.COLLECTION_POSTS).document(postId))
            batch.commit().await()

            val ref2 = db.collection(AppConfig.COLLECTION_USERS).document(userId)
            ref2.update(
                "postCount", FieldValue.increment(-1)
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}