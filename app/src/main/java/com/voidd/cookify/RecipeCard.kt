package com.voidd.cookify

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Компонент карточки рецепта для отображения основной информации.
 *
 * @param recipe Map, содержащая данные рецепта (обязательные ключи: "title" и "ingredients")
 * @param onClick Обработчик клика по карточке
 * @param modifier Modifier для настройки внешнего вида карточки
 * @param maxTitleLines Максимальное количество строк для заголовка (по умолчанию 1)
 * @param maxIngredientLines Максимальное количество строк для списка ингредиентов (по умолчанию 3)
 */
@Composable
fun RecipeCard(
    recipe: Map<String, Any>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    maxTitleLines: Int = 1,
    maxIngredientLines: Int = 3
) {
    // Основная карточка с тенью и обработчиком клика
    Card(
        modifier = modifier
            .padding(8.dp) // Внешние отступы
            .fillMaxWidth() // Занимает всю доступную ширину
            .clickable(onClick = onClick), // Обработчик клика
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp) // Тень карточки
    ) {
        // Вертикальное расположение элементов внутри карточки
        Column(
            modifier = Modifier.padding(16.dp) // Внутренние отступы
        ) {
            // Заголовок рецепта
            Text(
                text = recipe["title"]?.toString() ?: "Без названия", // Используем значение из Map или заглушку
                style = MaterialTheme.typography.titleLarge, // Стиль из Material Design
                maxLines = maxTitleLines, // Ограничение количества строк
                overflow = TextOverflow.Ellipsis // Многоточие при переполнении
            )

            // Вертикальный отступ между заголовком и ингредиентами
            Spacer(modifier = Modifier.height(8.dp))

            // Список ингредиентов рецепта
            Text(
                text = recipe["ingredients"]?.toString() ?: "Нет ингредиентов", // Используем значение из Map или заглушку
                style = MaterialTheme.typography.bodyMedium, // Стиль из Material Design
                maxLines = maxIngredientLines, // Ограничение количества строк
                overflow = TextOverflow.Ellipsis // Многоточие при переполнении
            )
        }
    }
}