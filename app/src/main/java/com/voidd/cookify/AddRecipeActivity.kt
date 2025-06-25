package com.voidd.cookify

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.voidd.cookify.ui.theme.CookifyTheme

// Основная активность для добавления нового рецепта
class AddRecipeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Установка Compose контента
        setContent {
            CookifyTheme {
                AddRecipeScreen()
            }
        }
    }
}

// Экран добавления рецепта с использованием Compose
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecipeScreen() {
    // Состояния для полей формы
    var title by remember { mutableStateOf("") }          // Название рецепта
    var ingredients by remember { mutableStateOf("") }    // Ингредиенты
    var instructions by remember { mutableStateOf("") }    // Инструкции по приготовлению

    // Инициализация Firebase сервисов
    val db = FirebaseFirestore.getInstance()              // Доступ к Firestore
    val auth = FirebaseAuth.getInstance()                 // Доступ к Firebase Auth
    val context = LocalContext.current                    // Контекст для Toast сообщений

    // Функция для добавления рецепта в Firestore
    fun addRecipe() {
        // Проверка заполнения обязательных полей
        if (title.isNotEmpty() && ingredients.isNotEmpty()) {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                Toast.makeText(context, "Ошибка: пользователь не авторизован", Toast.LENGTH_SHORT)
                    .show()
                return
            }

            // Создание объекта рецепта для сохранения
            val recipeData = mapOf(
                "title" to title,
                "ingredients" to ingredients,
                "instructions" to instructions,
                "timestamp" to System.currentTimeMillis(), // Временная метка создания
                "authorId" to userId,                    // ID автора
                "status" to "pending",                   // Статус "на модерации"
                "moderatorComment" to ""                  // Комментарий модератора (пока пустой)
            )

            // Добавление рецепта в коллекцию "recipes_pending"
            db.collection("recipes_pending").add(recipeData)
                .addOnSuccessListener {
                    Toast.makeText(context, "Рецепт отправлен на модерацию!", Toast.LENGTH_SHORT)
                        .show()
                    (context as ComponentActivity).finish() // Закрытие экрана после успешного сохранения
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(context, "Заполните обязательные поля", Toast.LENGTH_SHORT).show()
        }
    }

    // Структура экрана с использованием Scaffold
    Scaffold(
        topBar = {
            // Верхняя панель с заголовком и кнопкой "Назад"
            TopAppBar(
                title = { Text("Новый рецепт") },
                navigationIcon = {
                    IconButton(onClick = {
                        (context as ComponentActivity).finish()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            // Кнопка сохранения (FAB)
            FloatingActionButton(
                onClick = { addRecipe() }
            ) {
                Icon(Icons.Default.Check, contentDescription = "Сохранить")
            }
        }
    ) { padding ->
        // Основное содержимое экрана - форма ввода
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            // Поле для ввода названия рецепта
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Название рецепта") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Поле для ввода ингредиентов
            Text("Ингредиенты:", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small
            ) {
                BasicTextField(
                    value = ingredients,
                    onValueChange = { ingredients = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)

                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Поле для ввода инструкций
            Text("Инструкция:", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small
            ) {
                BasicTextField(
                    value = instructions,
                    onValueChange = { instructions = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
        }
    }
}