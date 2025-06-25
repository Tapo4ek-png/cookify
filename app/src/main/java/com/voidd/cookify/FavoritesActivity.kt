package com.voidd.cookify

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.voidd.cookify.ui.theme.CookifyTheme
import kotlinx.coroutines.tasks.await

// Основная Activity для отображения избранных рецептов
class FavoritesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CookifyTheme {
                // Создание экрана избранного с обработчиками нажатий
                FavoritesScreen(
                    onBack = { finish() }, // Обработчик кнопки "Назад"
                    onRecipeClick = { recipe ->
                        // Переход к деталям рецепта при нажатии
                        startActivity(
                            Intent(this, RecipeDetailActivity::class.java).apply {
                                putExtra(RecipeDetailActivity.EXTRA_RECIPE, HashMap(recipe))
                            }
                        )
                    }
                )
            }
        }
    }
}

// Композируемая функция для экрана избранного
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onBack: () -> Unit, // Колбэк для кнопки "Назад"
    onRecipeClick: (Map<String, Any>) -> Unit // Колбэк для нажатия на рецепт
) {
    // Инициализация Firestore и Firebase Auth
    val db = Firebase.firestore
    val auth = FirebaseAuth.getInstance()

    // Состояния для хранения избранных рецептов и статуса загрузки
    var favorites by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Получение ID текущего пользователя с кэшированием
    val userId = remember(auth.currentUser?.uid) {
        auth.currentUser?.uid ?: run {
            isLoading = false
            null
        }
    }

    // Эффект для первоначальной загрузки данных
    LaunchedEffect(userId) {
        if (userId == null) return@LaunchedEffect

        try {
            // Загрузка избранных рецептов из Firestore
            val snapshot = db.collection("users").document(userId)
                .collection("favorites")
                .get()
                .await()

            // Преобразование документов в Map с добавлением обязательных полей
            favorites = snapshot.documents.mapNotNull { doc ->
                doc.data?.toMutableMap()?.apply {
                    put("id", doc.id)
                    put("recipeId", doc.id)
                    if (!containsKey("status")) {
                        put("status", "approved")
                    }
                }
            }
        } catch (_: Exception) {
            // Обработка ошибок загрузки
        } finally {
            isLoading = false
        }
    }

    // Эффект для подписки на изменения в избранном
    DisposableEffect(userId) {
        if (userId == null) return@DisposableEffect onDispose {}

        // Создание слушателя для обновлений в реальном времени
        val listener = db.collection("users").document(userId)
            .collection("favorites")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                // Обновление списка при изменениях
                favorites = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.toMutableMap()?.apply {
                        put("id", doc.id)
                        put("recipeId", doc.id)
                        if (!containsKey("status")) {
                            put("status", "approved")
                        }
                    }
                } ?: emptyList()
            }

        // Отписка при уничтожении компонента
        onDispose {
            listener.remove()
        }
    }

    // Структура экрана с AppBar и контентом
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Избранное") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        // Отображение состояния загрузки/контента/пустого состояния
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator() // Индикатор загрузки
                }
            }
            favorites.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Нет избранных рецептов") // Сообщение об отсутствии рецептов
                }
            }
            else -> {
                // Список избранных рецептов
                LazyColumn(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                ) {
                    items(favorites, key = { it["id"].toString() }) { recipe ->
                        CompactRecipeCard(
                            recipe = recipe,
                            onClick = { onRecipeClick(recipe) },
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}

// Компонент карточки рецепта в компактном формате
@Composable
fun CompactRecipeCard(
    recipe: Map<String, Any>, // Данные рецепта
    onClick: () -> Unit, // Обработчик нажатия
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Название рецепта
            Text(
                text = recipe["title"]?.toString() ?: "Без названия",
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Ингредиенты рецепта
            Text(
                text = recipe["ingredients"]?.toString() ?: "Нет ингредиентов",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}