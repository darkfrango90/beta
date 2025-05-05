package com.darkfrango.despesas

import android.app.ProgressDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.darkfrango.despesas.model.Despesa
import com.darkfrango.despesas.service.FirebaseService
import com.google.firebase.auth.FirebaseAuth
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class LancamentoDespesaActivity : AppCompatActivity() {

    private lateinit var edtValor: EditText
    private lateinit var imgFoto: ImageView
    private var uriFoto: Uri? = null
    private var currentPhotoPath: String = ""
    private val firebaseService = FirebaseService()
    private val REQUEST_GALLERY = 1000
    private val REQUEST_CAMERA = 1002
    private val REQUEST_PERMISSION = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lancamento_despesa)

        edtValor = findViewById(R.id.edtValor)
        imgFoto = findViewById(R.id.imageViewFoto)
        val btnFoto = findViewById<Button>(R.id.btnFoto)
        val btnEnviar = findViewById<Button>(R.id.btnEnviar)

        btnFoto.setOnClickListener {
            abrirOpcoesFoto()
        }

        btnEnviar.setOnClickListener {
            enviarDespesa()
        }
    }

    private fun abrirOpcoesFoto() {
        val options = arrayOf("Galeria", "Câmera")
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Selecionar imagem")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> selecionarFotoGaleria()
                1 -> tirarFotoComCamera()
            }
        }
        builder.show()
    }

    private fun selecionarFotoGaleria() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_IMAGES
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (checkSelfPermission(permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(permission), REQUEST_PERMISSION)
            return
        }

        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_GALLERY)
    }

    private fun tirarFotoComCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            val photoFile: File? = try {
                criarArquivoImagem()
            } catch (ex: IOException) {
                ex.printStackTrace()
                null
            }
            if (photoFile != null) {
                val photoURI = FileProvider.getUriForFile(
                    this,
                    "${packageName}.fileprovider",
                    photoFile
                )
                uriFoto = photoURI
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                startActivityForResult(intent, REQUEST_CAMERA)
            }
        }
    }

    @Throws(IOException::class)
    private fun criarArquivoImagem(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir!!).apply {
            currentPhotoPath = absolutePath
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                REQUEST_GALLERY -> {
                    uriFoto = data?.data
                    val inputStream = contentResolver.openInputStream(uriFoto!!)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    imgFoto.setImageBitmap(bitmap)
                }
                REQUEST_CAMERA -> {
                    val bitmap = BitmapFactory.decodeFile(currentPhotoPath)
                    imgFoto.setImageBitmap(bitmap)
                    uriFoto = Uri.fromFile(File(currentPhotoPath))
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION && grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            selecionarFotoGaleria()
        } else {
            Toast.makeText(this, "Permissão necessária para acessar imagens", Toast.LENGTH_SHORT).show()
        }
    }

    private fun enviarDespesa() {
        val valorTexto = edtValor.text.toString().trim()
        if (valorTexto.isEmpty()) {
            Toast.makeText(this, "Informe o valor da despesa", Toast.LENGTH_SHORT).show()
            return
        }

        val valor = valorTexto.toDoubleOrNull()
        if (valor == null) {
            Toast.makeText(this, "Valor inválido", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUser = FirebaseAuth.getInstance().currentUser
        val despesa = Despesa(
            valor = valor,
            dataHora = Date(),
            userId = currentUser?.uid,
            email = currentUser?.email,
            nome = currentUser?.displayName
        )

        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Enviando despesa...")
        progressDialog.setCancelable(false)
        progressDialog.show()

        firebaseService.salvarDespesa(
            context = this,
            despesa = despesa,
            imagemUri = uriFoto,
            onSuccess = {
                progressDialog.dismiss()
                Toast.makeText(this, "Despesa enviada com sucesso!", Toast.LENGTH_SHORT).show()
                edtValor.text.clear()
                imgFoto.setImageResource(0)
                uriFoto = null
            },
            onFailure = { erro ->
                progressDialog.dismiss()
                Toast.makeText(this, "Erro: $erro", Toast.LENGTH_LONG).show()
            }
        )
    }
}
