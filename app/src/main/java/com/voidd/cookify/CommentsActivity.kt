package com.voidd.cookify

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.voidd.cookify.ui.theme.CookifyTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Activity для отображения и управления комментариями к рецепту.
 * Получает recipeId из Intent для загрузки соответствующих комментариев.
 */
class CommentsActivity : ComponentActivity() {
    companion object {
        const val EXTRA_RECIPE_ID = "extra_recipe_id" // Ключ для передачи ID рецепта
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Получаем ID рецепта из Intent
        val recipeId = intent.getStringExtra(EXTRA_RECIPE_ID) ?: ""
        setContent {
            CookifyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Основной экран комментариев
                    CommentsScreen(
                        recipeId = recipeId,
                        onBack = { finish() } // Обработчик кнопки "Назад"
                    )
                }
            }
        }
    }
}

/**
 * Основной экран комментариев с верхней панелью и областью содержимого.
 * @param recipeId ID рецепта, для которого показываются комментарии
 * @param onBack Обработчик нажатия кнопки "Назад"
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsScreen(
    recipeId: String,
    onBack: () -> Unit
) {
    // Используем Scaffold для базовой структуры экрана с TopAppBar
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Комментарии") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        // Основное содержимое экрана
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(bottom = 16.dp)
                .fillMaxSize()
        ) {
            // Секция с комментариями
            CommentsSection(
                recipeId = recipeId,
                modifier = Modifier.weight(1f) // Занимает все доступное пространство
            )
        }
    }
}

/**
 * Секция с комментариями, включая их загрузку, отображение и форму для нового комментария.
 * @param recipeId ID рецепта
 * @param modifier Модификатор для настройки layout
 */
@Composable
fun CommentsSection(recipeId: String, modifier: Modifier) {
    // Инициализация Firestore и Firebase Auth
    val db = Firebase.firestore
    val auth = FirebaseAuth.getInstance()
    val currentUserId = auth.currentUser?.uid // ID текущего пользователя

    // Состояния для управления комментариями
    var comments by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var newComment by remember { mutableStateOf("") } // Текст нового комментария
    var isLoading by remember { mutableStateOf(true) } // Флаг загрузки
    var errorMessage by remember { mutableStateOf<String?>(null) } // Сообщение об ошибке

    /**
     * Функция для удаления комментария.
     * @param commentId ID комментария для удаления
     */
    val deleteComment = { commentId: String ->
        db.collection("recipes").document(recipeId)
            .collection("comments")
            .document(commentId)
            .delete()
            .addOnSuccessListener {
                Log.d("COMMENT", "Comment deleted successfully")
            }
            .addOnFailureListener { e ->
                errorMessage = "Не удалось удалить комментарий"
                Log.e("COMMENT", "Error deleting comment", e)
            }
    }

    // Эффект для загрузки комментариев при изменении recipeId
    LaunchedEffect(recipeId) {
        try {
            // Слушатель для получения комментариев в реальном времени
            db.collection("recipes").document(recipeId)
                .collection("comments")
                .orderBy("timestamp", Query.Direction.DESCENDING) // Сортировка по дате (новые сначала)
                .addSnapshotListener { snapshot, error ->
                    isLoading = false

                    if (error != null) {
                        errorMessage = "Ошибка загрузки комментариев"
                        Log.e("COMMENTS", "Error loading comments", error)
                        return@addSnapshotListener
                    }

                    // Преобразование документов в список карт с данными
                    comments = snapshot?.documents?.mapNotNull { doc ->
                        doc.data?.toMutableMap()?.apply {
                            put("id", doc.id) // Добавляем ID документа в данные
                            Log.d("COMMENT_LOADED", "Comment loaded: ${doc.id}")
                        }
                    } ?: emptyList()

                    Log.d("COMMENTS", "Loaded ${comments.size} comments")
                }
        } catch (e: Exception) {
            errorMessage = "Ошибка при загрузке"
            Log.e("COMMENTS", "Exception", e)
        }
    }

    // Основной layout секции комментариев
    Column(
        modifier = modifier
            .padding(horizontal = 16.dp)
    ) {
        // Состояния отображения
        when {
            isLoading -> {
                // Индикатор загрузки
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            errorMessage != null -> {
                // Сообщение об ошибке
                Text(
                    errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(8.dp)
                )
            }

            comments.isEmpty() -> {
                // Сообщение при отсутствии комментариев
                Text(
                    "Пока нет комментариев",
                    modifier = Modifier.padding(8.dp)
                )
            }

            else -> {
                // Список комментариев с LazyColumn для эффективного отображения
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(comments) { comment ->
                        // Карточка отдельного комментария
                        CommentCard(
                            comment = comment,
                            onReplyClick = { userEmail ->
                                newComment = "$userEmail, " // Добавление имени пользователя в ответ
                            },
                            onDeleteClick = { commentId ->
                                deleteComment(commentId) // Обработчик удаления
                            },
                            canDelete = comment["userId"]?.toString() == currentUserId // Проверка прав на удаление
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        // Поле ввода нового комментария
        CommentInput(
            value = newComment,
            onValueChange = { newComment = it },
            onSubmit = {
                // Обработчик отправки комментария
                if (newComment.isNotBlank() && auth.currentUser != null) {
                    val user = auth.currentUser!!
                    val commentData = hashMapOf(
                        "text" to newComment,
                        "userId" to user.uid,
                        "userEmail" to (user.email ?: "Аноним"),
                        "timestamp" to System.currentTimeMillis()
                    )

                    // Добавление комментария в Firestore
                    db.collection("recipes").document(recipeId)
                        .collection("comments")
                        .add(commentData)
                        .addOnSuccessListener {
                            newComment = "" // Очистка поля после успешной отправки
                            Log.d("COMMENT", "Comment added successfully")
                        }
                        .addOnFailureListener { e ->
                            errorMessage = "Не удалось отправить комментарий"
                            Log.e("COMMENT", "Error adding comment", e)
                        }
                }
            },
            enabled = auth.currentUser != null // Поле ввода доступно только авторизованным пользователям
        )
    }
}

/**
 * Карточка для отображения отдельного комментария.
 * @param comment Данные комментария (map с полями: text, userId, userEmail, timestamp, id)
 * @param onReplyClick Обработчик нажатия кнопки "Ответить"
 * @param onDeleteClick Обработчик нажатия кнопки "Удалить"
 * @param canDelete Флаг, разрешающий удаление (true только для автора комментария)
 */
@Composable
fun CommentCard(
    comment: Map<String, Any>,
    onReplyClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit,
    canDelete: Boolean
) {
    var showMenu by remember { mutableStateOf(false) } // Состояние для отображения меню действий
    // Форматирование даты комментария
    val dateFormatter = remember { SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault()) }
    val date = remember(comment["timestamp"]) {
        dateFormatter.format(Date((comment["timestamp"] as? Long) ?: 0))
    }

    // Карточка с тенью
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Заголовок карточки с информацией об авторе и дате
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = comment["userEmail"]?.toString() ?: "Аноним",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = date,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                // Кнопка меню действий
                Box(
                    contentAlignment = Alignment.CenterEnd
                ) {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Действия с комментарием",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Выпадающее меню с действиями
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        // Пункт "Ответить" - доступен всем
                        DropdownMenuItem(
                            text = { Text("Ответить") },
                            onClick = {
                                showMenu = false
                                onReplyClick(comment["userEmail"]?.toString() ?: "Аноним")
                            }
                        )

                        // Пункт "Удалить" - только автору
                        if (canDelete) {
                            DropdownMenuItem(
                                text = { Text("Удалить", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMenu = false
                                    onDeleteClick(comment["id"]?.toString() ?: "")
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Текст комментария
            Text(
                text = comment["text"]?.toString() ?: "",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

/**
 * Поле ввода для нового комментария.
 * @param value Текущее значение поля
 * @param onValueChange Обработчик изменения текста
 * @param onSubmit Обработчик отправки комментария
 * @param enabled Флаг доступности поля ввода
 */
@Composable
fun CommentInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    enabled: Boolean
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        placeholder = { Text("Написать комментарий...") },
        trailingIcon = {
            // Кнопка отправки
            IconButton(
                onClick = onSubmit,
                enabled = value.isNotBlank() && enabled
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Отправить")
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
        keyboardActions = KeyboardActions(onSend = { onSubmit() }),
        singleLine = false,
        maxLines = 3,
        enabled = enabled
    )
}