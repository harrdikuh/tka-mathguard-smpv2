package com.example.tkamathguard

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Penyimpanan hasil ujian ke Firestore.
 * Hasil dikirim setelah ujian selesai; pembacaan rekap guru akan diaktifkan pada tahap berikutnya.
 */
object CloudRepository {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    fun saveResult(
        name: String,
        school: String,
        studentClass: String,
        score: Int,
        correct: Int,
        wrong: Int,
        violations: Int,
        onComplete: (Boolean, String) -> Unit
    ) {
        fun writeResult(uid: String) {
            val data = hashMapOf<String, Any>(
                "uid" to uid,
                "name" to name.trim(),
                "school" to school.trim(),
                "studentClass" to studentClass.trim(),
                "score" to score,
                "correct" to correct,
                "wrong" to wrong,
                "violations" to violations,
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("examResults")
                .add(data)
                .addOnSuccessListener { onComplete(true, "Berhasil tersimpan di cloud.") }
                .addOnFailureListener { error ->
                    onComplete(false, "Gagal menyimpan: ${error.localizedMessage ?: "periksa internet dan aturan Firebase"}")
                }
        }

        val currentUser = auth.currentUser
        if (currentUser != null) {
            writeResult(currentUser.uid)
        } else {
            auth.signInAnonymously()
                .addOnSuccessListener { result ->
                    val uid = result.user?.uid
                    if (uid == null) onComplete(false, "Login perangkat gagal.") else writeResult(uid)
                }
                .addOnFailureListener { error ->
                    onComplete(false, "Login Firebase gagal: ${error.localizedMessage ?: "aktifkan Anonymous sign-in"}")
                }
        }
    }
}
