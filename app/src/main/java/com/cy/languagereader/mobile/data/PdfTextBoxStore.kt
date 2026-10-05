package com.cy.languagereader.mobile.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class PdfTextBoxNote(
    val id: String,
    val pageIndex: Int,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val text: String,
)

class PdfTextBoxStore(context: Context) {
    private val prefs =
        context.getSharedPreferences("pdf_text_boxes", Context.MODE_PRIVATE)

    fun list(bookId: String): List<PdfTextBoxNote> =
        decode(prefs.getString(key(bookId), "[]").orEmpty())

    fun add(
        bookId: String,
        pageIndex: Int,
        x: Float,
        y: Float,
        text: String,
    ): PdfTextBoxNote {
        val item = PdfTextBoxNote(
            id = UUID.randomUUID().toString(),
            pageIndex = pageIndex,
            x = x.coerceIn(0.02f, 0.78f),
            y = y.coerceIn(0.02f, 0.88f),
            width = 0.34f,
            height = 0.10f,
            text = text.trim(),
        )

        val next = list(bookId).toMutableList().apply { add(item) }
        save(bookId, next)
        return item
    }

    fun update(
        bookId: String,
        item: PdfTextBoxNote,
    ) {
        val next = list(bookId)
            .map { existing ->
                if (existing.id == item.id) item else existing
            }
        save(bookId, next)
    }

    fun remove(
        bookId: String,
        id: String,
    ) {
        save(
            bookId,
            list(bookId).filterNot { it.id == id },
        )
    }

    fun clear(bookId: String) {
        prefs.edit().remove(key(bookId)).apply()
    }

    private fun save(
        bookId: String,
        items: List<PdfTextBoxNote>,
    ) {
        val array = JSONArray()

        items.forEach { item ->
            array.put(
                JSONObject()
                    .put("id", item.id)
                    .put("page", item.pageIndex)
                    .put("x", item.x.toDouble())
                    .put("y", item.y.toDouble())
                    .put("width", item.width.toDouble())
                    .put("height", item.height.toDouble())
                    .put("text", item.text)
            )
        }

        prefs.edit()
            .putString(key(bookId), array.toString())
            .apply()
    }

    private fun decode(raw: String): List<PdfTextBoxNote> =
        runCatching {
            val array = JSONArray(raw)

            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)

                    add(
                        PdfTextBoxNote(
                            id = item.optString("id"),
                            pageIndex = item.optInt("page", 0),
                            x = item.optDouble("x", 0.08).toFloat(),
                            y = item.optDouble("y", 0.08).toFloat(),
                            width = item.optDouble("width", 0.34).toFloat(),
                            height = item.optDouble("height", 0.10).toFloat(),
                            text = item.optString("text"),
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())

    private fun key(bookId: String): String =
        "boxes_$bookId"
}
