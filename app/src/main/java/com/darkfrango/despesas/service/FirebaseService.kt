package com.darkfrango.despesas.service

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import com.darkfrango.despesas.model.Despesa
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import java.util.Date
import java.util.UUID

class FirebaseService {

    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    fun salvarDespesa(
        context: Context,
        despesa: Despesa,
        imagemUri: Uri?,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userId = despesa.userId ?: currentUser?.uid ?: "desconhecido"
        val email = despesa.email ?: currentUser?.email ?: "sem-email"
        val nome = despesa.nome ?: currentUser?.displayName ?: "sem-nome"
        val dataHora = despesa.dataHora ?: Date()

        val salvarNoFirestore = { fotoUrl: String ->
            val dados = hashMapOf(
                "valor" to despesa.valor,
                "dataHora" to dataHora,
                "userId" to userId,
                "email" to email,
                "nome" to nome,
                "fotoUrl" to fotoUrl
            )

            firestore.collection("despesas")
                .add(dados)
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { e ->
                    onFailure(e.message ?: "Erro ao salvar no Firestore")
                }
        }

        if (imagemUri != null) {
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, imagemUri)
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 60, outputStream)
                val imageData = outputStream.toByteArray()

                val ref = storage.reference.child("fotos/$userId/${UUID.randomUUID()}.jpg")
                val uploadTask = ref.putBytes(imageData)

                uploadTask
                    .continueWithTask { task ->
                        if (!task.isSuccessful) {
                            task.exception?.let { throw it }
                        }
                        ref.downloadUrl
                    }
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            salvarNoFirestore(task.result.toString())
                        } else {
                            onFailure("Erro ao obter URL da imagem.")
                        }
                    }
            } catch (e: Exception) {
                onFailure("Erro ao processar imagem: ${e.message}")
            }
        } else {
            salvarNoFirestore("")
        }
    }
}
