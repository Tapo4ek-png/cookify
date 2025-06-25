@file:Suppress("DEPRECATION")

package com.voidd.cookify

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.voidd.cookify.ui.theme.CookifyTheme
import kotlinx.coroutines.tasks.await

@Suppress("DEPRECATION")
class RecipeDetailActivity : ComponentActivity() {
    companion object {
        // Константы для передачи данных между активностями
        const val EXTRA_RECIPE = "extra_recipe"
        private const val DEEP_LINK_BASE = "https://cookify-84195.web.app/recipe/"

        // Генерация deep link для рецепта
        fun getDeepLink(recipeId: String): String {
            return "$DEEP_LINK_BASE$recipeId"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Получаем ID рецепта из deep link или из intent
        val recipeId = if (intent?.action == Intent.ACTION_VIEW) {
            // Если открыто через deep link
            intent.data?.lastPathSegment ?: ""
        } else {
            // Если открыто из приложения
            (intent.getSerializableExtra(EXTRA_RECIPE) as? Map<*, *>)?.get("id")?.toString() ?: ""
        }

        // Устанавливаем Compose контент
        setContent {
            CookifyTheme {
                RecipeDetailScreen(
                    recipeId = recipeId,
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    recipeId: String,
    onBack: () -> Unit
) {
    // Контекст и Firebase зависимости
    val context = LocalContext.current
    val db = Firebase.firestore
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid

    // Состояния экрана
    var recipe by remember { mutableStateOf<Map<String, Any>?>(null) } // Данные рецепта
    var isLoading by remember { mutableStateOf(true) } // Флаг загрузки
    var isFavorite by remember { mutableStateOf(false) } // Флаг избранного
    var favoritesCount by remember { mutableIntStateOf(0) } // Счетчик избранных
    var loadError by remember { mutableStateOf<String?>(null) } // Ошибка загрузки

    // Эффект для загрузки данных рецепта при изменении recipeId
    LaunchedEffect(recipeId) {
        if (recipeId.isEmpty()) {
            loadError = "Не указан ID рецепта"
            isLoading = false
            return@LaunchedEffect
        }

        try {
            // 1. Загружаем основной документ рецепта из Firestore
            val recipeDoc = db.collection("recipes").document(recipeId).get().await()
            if (!recipeDoc.exists()) {
                loadError = "Рецепт не найден"
                isLoading = false
                return@LaunchedEffect
            }

            recipe = recipeDoc.data ?: emptyMap()

            // 2. Загружаем количество пользователей, добавивших рецепт в избранное
            val favoritesSnapshot = db.collection("recipes").document(recipeId)
                .collection("favorites").get().await()
            favoritesCount = favoritesSnapshot.size()

            // 3. Проверяем, добавил ли текущий пользователь рецепт в избранное
            if (userId != null) {
                val userFavoriteDoc = db.collection("users").document(userId)
                    .collection("favorites").document(recipeId).get().await()
                isFavorite = userFavoriteDoc.exists()
            }

            isLoading = false
        } catch (e: Exception) {
            loadError = "Ошибка загрузки: ${e.localizedMessage}"
            isLoading = false
        }
    }

    /**
     * Функция для добавления/удаления рецепта из избранного
     */
    fun toggleFavorite() {
        if (recipeId.isEmpty() || userId == null) return

        if (isFavorite) {
            // Удаляем из избранного:
            // 1. Из коллекции пользователя
            db.collection("users").document(userId)
                .collection("favorites").document(recipeId).delete()

            // 2. Из коллекции рецепта
            db.collection("recipes").document(recipeId)
                .collection("favorites").document(userId).delete()
                .addOnSuccessListener { isFavorite = false }
        } else {
            // Добавляем в избранное:
            val favoriteData = hashMapOf(
                "timestamp" to System.currentTimeMillis()
            )

            // 1. В коллекцию пользователя
            db.collection("users").document(userId)
                .collection("favorites").document(recipeId).set(recipe!! + favoriteData)

            // 2. В коллекцию рецепта
            db.collection("recipes").document(recipeId)
                .collection("favorites").document(userId).set(favoriteData)
                .addOnSuccessListener { isFavorite = true }
        }
    }

    /**
     * Функция для поделиться рецептом
     */
    fun shareRecipe() {
        val deepLink = RecipeDetailActivity.getDeepLink(recipeId)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, recipe?.get("title")?.toString() ?: "Рецепт из Cookify")
            putExtra(
                Intent.EXTRA_TEXT,
                "Посмотрите этот рецепт в Cookify:\n\n" +
                        "${recipe?.get("title")}\n\n" +
                        "Ссылка: $deepLink\n\n" +
                        "Или скачайте приложение: https://github.com/Tapo4ek-png/cookify/releases/tag/release]"
            )
        }
        ContextCompat.startActivity(
            context,
            Intent.createChooser(shareIntent, "Поделиться рецептом"),
            null
        )
    }

    // UI экрана
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(recipe?.get("title")?.toString() ?: "Рецепт") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    // Кнопка "Поделиться"
                    IconButton(onClick = { shareRecipe() }) {
                        Icon(Icons.Default.Share, contentDescription = "Поделиться")
                    }

                    // Кнопка "Избранное" (только для авторизованных пользователей)
                    if (userId != null) {
                        IconButton(onClick = { toggleFavorite() }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = if (isFavorite) "Удалить из избранного" else "Добавить в избранное",
                                    tint = if (isFavorite) Color.Red else MaterialTheme.colorScheme.onSurface
                                )
                                if (favoritesCount > 0) {
                                    Text(
                                        text = favoritesCount.toString(),
                                        fontSize = 10.sp,
                                        color = if (isFavorite) Color.Red else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        // Состояния экрана
        when {
            // Загрузка
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            // Ошибка
            loadError != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(loadError!!, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onBack) {
                            Text("Назад")
                        }
                    }
                }
            }
            // Данные не загружены
            recipe == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Данные рецепта не загружены", color = MaterialTheme.colorScheme.error)
                }
            }
            // Успешная загрузка - отображаем рецепт
            else -> {
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        // Секция "Ингредиенты"
                        Text(
                            text = "Ингредиенты:",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = recipe?.get("ingredients")?.toString() ?: "Нет данных",
                            fontSize = 16.sp,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )

                        // Секция "Инструкция"
                        Text(
                            text = "Инструкция:",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = recipe?.get("instructions")?.toString() ?: "Нет данных",
                            fontSize = 16.sp,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )

                        // Кнопка для перехода к комментариям
                        Button(
                            onClick = {
                                val intent = Intent(context, CommentsActivity::class.java).apply {
                                    putExtra(CommentsActivity.EXTRA_RECIPE_ID, recipeId)
                                }
                                context.startActivity(intent)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp)
                        ) {
                            Text("Показать комментарии")
                        }
                    }
                }
            }
        }
    }
}