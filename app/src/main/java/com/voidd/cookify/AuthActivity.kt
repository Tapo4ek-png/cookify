package com.voidd.cookify

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.voidd.cookify.ui.theme.CookifyTheme

// Основная Activity для аутентификации пользователя
class AuthActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Устанавливаем Compose контент для Activity
        setContent {
            CookifyTheme {
                AuthScreenContent()
            }
        }
    }
}

// Композируемая функция, представляющая экран аутентификации
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreenContent() {
    // Получаем экземпляр Firebase Auth
    val auth = Firebase.auth
    // Получаем контекст для Toast и Intent
    val context = LocalContext.current
    // Состояния для хранения email и password
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Основной контейнер с вертикальным расположением элементов
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Заголовок приложения
        Text(
            text = "Cookify",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Поле ввода email
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Поле ввода пароля (с маскировкой символов)
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Кнопка входа
        Button(
            onClick = {
                // Проверка на пустые поля
                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(context, "Enter email and password", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                // Попытка входа через Firebase Auth
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // При успешном входе переход в MainActivity
                            context.startActivity(Intent(context, MainActivity::class.java))
                            // Закрытие текущей Activity
                            (context as? ComponentActivity)?.finish()
                        } else {
                            // Показ ошибки при неудачном входе
                            Toast.makeText(
                                context,
                                "Error: ${task.exception?.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Login")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Кнопка регистрации
        Button(
            onClick = {
                // Проверка на пустые поля
                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(context, "Enter email and password", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                // Попытка регистрации через Firebase Auth
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // При успешной регистрации переход в MainActivity
                            context.startActivity(Intent(context, MainActivity::class.java))
                            // Закрытие текущей Activity
                            (context as? ComponentActivity)?.finish()
                        } else {
                            // Показ ошибки при неудачной регистрации
                            Toast.makeText(
                                context,
                                "Error: ${task.exception?.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Register")
        }
    }
}