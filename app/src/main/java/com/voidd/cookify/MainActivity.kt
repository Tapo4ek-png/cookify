package com.voidd.cookify

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.voidd.cookify.ui.theme.CookifyTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Устанавливаем Compose как корневой элемент интерфейса
        setContent {
            CookifyTheme {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    RecipeApp() // Основной компонент приложения
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun RecipeApp() {
        // Инициализация Firebase Auth и Firestore
        val auth = FirebaseAuth.getInstance()
        val db = Firebase.firestore
        val context = LocalContext.current
        // Состояние для бокового меню
        val drawerState = rememberDrawerState(DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        // Состояние для списка рецептов и поискового запроса
        var recipes by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
        var searchQuery by remember { mutableStateOf("") }

        // Проверка авторизации пользователя
        if (auth.currentUser == null) {
            LaunchedEffect(Unit) {
                // Если пользователь не авторизован, перенаправляем на экран авторизации
                context.startActivity(Intent(context, AuthActivity::class.java))
                (context as? Activity)?.finish()
            }
            return
        }

        // Функция загрузки рецептов из Firestore
        fun loadRecipes() {
            db.collection("recipes")
                .whereEqualTo("status", "approved") // Загружаем только одобренные рецепты
                .get()
                .addOnSuccessListener { snapshot ->
                    recipes = snapshot.documents.map { doc ->
                        val data = doc.data ?: mapOf()
                        data.toMutableMap().apply {
                            put("id", doc.id) // Добавляем ID документа к данным рецепта
                        }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Ошибка загрузки рецептов", Toast.LENGTH_SHORT).show()
                }
        }

        // Эффект для первоначальной загрузки и подписки на изменения рецептов
        LaunchedEffect(Unit) {
            loadRecipes()

            // Подписываемся на изменения в коллекции рецептов
            db.collection("recipes")
                .addSnapshotListener { snapshot, _ ->
                    recipes = snapshot?.documents?.map { doc ->
                        val data = doc.data ?: mapOf()
                        data.toMutableMap().apply {
                            put("id", doc.id)
                        }
                    } ?: emptyList()
                }
        }

        // Фильтрация рецептов по поисковому запросу
        val filteredRecipes = recipes.filter { recipe ->
            searchQuery.isEmpty() ||
                    recipe["title"]?.toString()?.contains(searchQuery, ignoreCase = true) == true ||
                    recipe["ingredients"]?.toString()
                        ?.contains(searchQuery, ignoreCase = true) == true
        }

        // Основная структура интерфейса с боковым меню
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                AppDrawer(
                    onAddRecipe = {
                        context.startActivity(Intent(context, AddRecipeActivity::class.java))
                    },
                    onFavorites = {
                        context.startActivity(Intent(context, FavoritesActivity::class.java))
                    },
                    onCloseDrawer = { scope.launch { drawerState.close() } }
                )
            }
        ) {
            // Структура экрана с AppBar и контентом
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Cookify") },
                        navigationIcon = {
                            IconButton(
                                onClick = { scope.launch { drawerState.open() } }
                            ) {
                                Icon(Icons.Default.Menu, contentDescription = "Меню")
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = {
                                    context.startActivity(
                                        Intent(
                                            context,
                                            SearchActivity::class.java
                                        )
                                    )
                                }
                            ) {
                                Icon(Icons.Default.Search, contentDescription = "Поиск")
                            }
                        }
                    )
                }
            ) { padding ->
                if (filteredRecipes.isEmpty()) {
                    // Отображение сообщения, если рецептов нет
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (searchQuery.isEmpty()) "Пока нет рецептов" else "Ничего не найдено")
                    }
                } else {
                    // Отображение списка рецептов
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    ) {
                        items(filteredRecipes) { recipe ->
                            RecipeCard(
                                recipe = recipe,
                                onClick = {
                                    // Переход на экран деталей рецепта
                                    context.startActivity(
                                        Intent(context, RecipeDetailActivity::class.java).apply {
                                            putExtra(
                                                RecipeDetailActivity.EXTRA_RECIPE,
                                                HashMap(recipe)
                                            )
                                        }
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun AppDrawer(
        onAddRecipe: () -> Unit,
        onFavorites: () -> Unit,
        onCloseDrawer: () -> Unit
    ) {
        // Получение текущего пользователя
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        val context = LocalContext.current

        // Структура бокового меню
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // Шапка меню с email пользователя
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.BottomStart)
                ) {
                    Text(
                        text = currentUser?.email ?: "Гость",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Основные пункты меню
            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(
                    listOf(
                        "Добавить рецепт" to Icons.Default.Add,
                        "Избранное" to Icons.Default.Favorite
                    )
                ) { (title, icon) ->
                    NavigationDrawerItem(
                        label = { Text(title) },
                        selected = false,
                        onClick = {
                            when (title) {
                                "Добавить рецепт" -> onAddRecipe()
                                "Избранное" -> onFavorites()
                            }
                            onCloseDrawer()
                        },
                        icon = {
                            Icon(icon, contentDescription = title)
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }

            // Нижняя часть меню с кнопкой выхода
            Column(
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                HorizontalDivider()
                NavigationDrawerItem(
                    label = { Text("Выйти из аккаунта") },
                    selected = false,
                    onClick = {
                        auth.signOut()
                        onCloseDrawer()
                        (context as? Activity)?.finishAffinity()
                    },
                    icon = {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Выйти")
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}