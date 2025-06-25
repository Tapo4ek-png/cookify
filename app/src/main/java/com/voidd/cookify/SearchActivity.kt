package com.voidd.cookify

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.voidd.cookify.ui.theme.CookifyTheme

// Основная Activity для поиска рецептов
class SearchActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CookifyTheme {
                // Отображаем экран поиска с обработчиком возврата
                SearchScreen(onBack = { finish() })
            }
        }
    }
}

// Основной Composable для экрана поиска
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(onBack: () -> Unit) {
    // Инициализация Firestore
    val db = Firebase.firestore
    val context = LocalContext.current

    // Состояния для поискового запроса и списка рецептов
    var searchQuery by remember { mutableStateOf("") }
    var recipes by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var showFilterDialog by remember { mutableStateOf(false) }

    // Состояния для фильтров поиска
    var searchInTitle by remember { mutableStateOf(true) }
    var searchInIngredients by remember { mutableStateOf(true) }
    var searchInInstructions by remember { mutableStateOf(true) }

    // Эффект для загрузки рецептов из Firestore
    LaunchedEffect(Unit) {
        db.collection("recipes")
            .whereEqualTo("status", "approved") // Фильтрация только одобренных рецептов
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("SearchActivity", "Error loading recipes", error)
                    return@addSnapshotListener
                }
                // Преобразование документов в список карт с добавлением ID
                recipes = snapshot?.documents?.mapNotNull {
                    it.data?.toMutableMap()?.apply {
                        put("id", it.id)
                    }
                } ?: emptyList()
            }
    }

    // Фильтрация рецептов по поисковому запросу и выбранным фильтрам
    val filteredRecipes = recipes.filter { recipe ->
        if (searchQuery.isEmpty()) true // Если запрос пустой, показываем все рецепты
        else {
            var matches = false
            // Проверка совпадений в зависимости от выбранных фильтров
            if (searchInTitle) {
                matches = matches || recipe["title"]?.toString()?.contains(searchQuery, ignoreCase = true) == true
            }
            if (searchInIngredients) {
                matches = matches || recipe["ingredients"]?.toString()?.contains(searchQuery, ignoreCase = true) == true
            }
            if (searchInInstructions) {
                matches = matches || recipe["instructions"]?.toString()?.contains(searchQuery, ignoreCase = true) == true
            }
            matches
        }
    }

    // Основной макет экрана с Scaffold
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    // Поле поиска с декоративным Surface
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.large),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 3.dp
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Основное текстовое поле для ввода поискового запроса
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 56.dp),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                singleLine = true,
                                decorationBox = { innerTextField ->
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        // Плейсхолдер, если поле пустое
                                        if (searchQuery.isEmpty()) {
                                            Text(
                                                "Поиск...",
                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                ),
                                                modifier = Modifier.fillMaxWidth(),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                        }
                    }
                },
                navigationIcon = {
                    // Кнопка "Назад"
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    // Кнопка для открытия диалога фильтров
                    IconButton(
                        onClick = { showFilterDialog = true }
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Фильтры поиска",
                            modifier = Modifier.size(24.dp),
                            tint = if (searchInTitle && searchInIngredients && searchInInstructions) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.primary // Подсветка, если не все фильтры активны
                            }
                        )
                    }

                    // Кнопка очистки поиска (отображается только когда есть текст)
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Очистить",
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        // Отображение состояния в зависимости от результатов поиска
        if (filteredRecipes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (searchQuery.isEmpty()) "Введите запрос для поиска" else "Ничего не найдено",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxWidth()
            ) {
                // Заголовок с количеством результатов
                Text(
                    text = "Результаты поиска:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
                // Список найденных рецептов
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredRecipes) { recipe ->
                        RecipeCard(
                            recipe = recipe,
                            onClick = {
                                // Переход к деталям рецепта при нажатии
                                context.startActivity(
                                    Intent(context, RecipeDetailActivity::class.java).apply {
                                        putExtra(RecipeDetailActivity.EXTRA_RECIPE, HashMap(recipe))
                                    }
                                )
                            }
                        )
                    }
                }
            }
        }

        // Диалог фильтров поиска
        if (showFilterDialog) {
            AlertDialog(
                onDismissRequest = { showFilterDialog = false },
                title = { Text("Фильтры поиска") },
                text = {
                    Column {
                        Text("Искать в:", style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(8.dp))

                        // Чекбокс для поиска в заголовке
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = searchInTitle,
                                onCheckedChange = { searchInTitle = it }
                            )
                            Text(
                                "Заголовок рецепта",
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }

                        // Чекбокс для поиска в ингредиентах
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = searchInIngredients,
                                onCheckedChange = { searchInIngredients = it }
                            )
                            Text(
                                "Ингредиенты",
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }

                        // Чекбокс для поиска в инструкциях
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = searchInInstructions,
                                onCheckedChange = { searchInInstructions = it }
                            )
                            Text(
                                "Инструкции приготовления",
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = { showFilterDialog = false }
                    ) {
                        Text("ГОТОВО")
                    }
                }
            )
        }
    }
}