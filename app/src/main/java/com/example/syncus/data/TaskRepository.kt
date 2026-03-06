package com.example.syncus.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class TaskRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private fun tasksRef(uid: String) =
        db.collection("users").document(uid).collection("tasks")

    suspend fun getTasks(uid: String): List<Task> {
        val snap = tasksRef(uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .await()

        return snap.documents.map { doc ->
            Task(
                id = doc.id,
                title = doc.getString("title").orEmpty(),
                done = doc.getBoolean("done") ?: false,
                createdAt = doc.getTimestamp("createdAt")
            )
        }
    }

    suspend fun addTask(uid: String, title: String) {
        val data = hashMapOf(
            "title" to title.trim(),
            "done" to false,
            "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )
        tasksRef(uid).add(data).await()
    }

    suspend fun setDone(uid: String, taskId: String, done: Boolean) {
        tasksRef(uid).document(taskId)
            .update("done", done)
            .await()
    }

    suspend fun deleteTask(uid: String, taskId: String) {
        tasksRef(uid).document(taskId).delete().await()
    }
}