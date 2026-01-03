package com.mmb.compose.compose_wrapper

object ComposeWrapLogic {

    val choices = listOf(
        "Row",
        "Column",
        "Box",
        "LazyRow",
        "LazyColumn",
        "Card",
        "Surface"
    )

    fun buildWrappedCode(original: String, selected: String): String =
        when (selected) {
            "Row" -> "Row {\n    $original\n}"
            "Column" -> "Column {\n    $original\n}"
            "Box" -> "Box {\n    $original\n}"
            "Card" -> "Card {\n    $original\n}"
            "Surface" -> "Surface {\n    $original\n}"
            "LazyRow" -> "LazyRow {\n    item { $original }\n}"
            "LazyColumn" -> "LazyColumn {\n    item { $original }\n}"
            else -> original
        }

    fun importFor(selected: String): String? =
        when (selected) {
            "Row", "Column", "Box" ->
                "androidx.compose.foundation.layout.$selected"

            "LazyRow", "LazyColumn" ->
                "androidx.compose.foundation.lazy.$selected"

            "Card", "Surface" ->
                "androidx.compose.material3.$selected"

            else -> null
        }
}
