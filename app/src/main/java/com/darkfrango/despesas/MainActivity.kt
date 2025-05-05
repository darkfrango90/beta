package com.darkfrango.despesas

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var edtEmail: EditText
    private lateinit var edtSenha: EditText
    private lateinit var btnLogin: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        edtEmail = findViewById(R.id.editTextEmail)
        edtSenha = findViewById(R.id.editTextSenha)
        btnLogin = findViewById(R.id.buttonLogin)

        // Recupera o e-mail salvo (se houver)
        val sharedPreferences = getSharedPreferences("prefs", MODE_PRIVATE)
        val emailSalvo = sharedPreferences.getString("email_salvo", "")
        edtEmail.setText(emailSalvo)

        btnLogin.setOnClickListener {
            val email = edtEmail.text.toString().trim()
            val senha = edtSenha.text.toString().trim()

            if (email.isNotEmpty() && senha.isNotEmpty()) {
                FirebaseAuth.getInstance().signInWithEmailAndPassword(email, senha)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // Salva o e-mail para uso futuro
                            sharedPreferences.edit().putString("email_salvo", email).apply()

                            startActivity(Intent(this, LancamentoDespesaActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(
                                this,
                                "Erro no login: ${task.exception?.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Preencha email e senha", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
