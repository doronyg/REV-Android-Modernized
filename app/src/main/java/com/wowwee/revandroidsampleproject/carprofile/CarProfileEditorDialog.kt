package com.wowwee.revandroidsampleproject.carprofile

import android.content.Context
import android.text.InputType
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import com.wowwee.revandroidsampleproject.R
import java.util.Locale

fun interface CarProfileSaveListener {
    fun onSave(displayName: String, colorHex: String)
}

object CarProfileEditorDialog {
    private data class NamedColor(val name: String, val colorHex: String)

    private val presetColors = listOf(
        NamedColor("Black", "#111111"),
        NamedColor("White", "#F5F5F5")
    )

    @JvmStatic
    fun show(
        context: Context,
        title: String,
        initialName: String,
        initialColorHex: String,
        fallbackDisplayName: String,
        onSave: CarProfileSaveListener
    ) {
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (16 * context.resources.displayMetrics.density).toInt()
            setPadding(pad, pad / 2, pad, 0)
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        val nameInput = EditText(context).apply {
            hint = context.getString(R.string.car_profile_name_hint)
            setText(initialName)
            maxLines = 1
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
        }

        val colorInput = EditText(context).apply {
            hint = context.getString(R.string.car_profile_color_hint)
            setText(normalizeColor(initialColorHex))
            maxLines = 1
            inputType = InputType.TYPE_CLASS_TEXT
        }

        root.addView(nameInput)
        root.addView(colorInput)

        AlertDialog.Builder(context)
            .setTitle(title)
            .setView(root)
            .setNeutralButton(R.string.car_profile_pick_color) { _, _ ->
                showColorPicker(context) { selected ->
                    colorInput.setText(selected)
                    show(context, title, nameInput.text.toString(), selected, fallbackDisplayName, onSave)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.confirm) { _, _ ->
                val displayName = nameInput.text?.toString()?.trim().orEmpty()
                val sanitizedName = if (displayName.isEmpty()) {
                    fallbackDisplayName
                } else {
                    displayName
                }
                onSave.onSave(sanitizedName, normalizeColor(colorInput.text?.toString()))
            }
            .show()
    }

    private fun showColorPicker(context: Context, onColorSelected: (String) -> Unit) {
        val labels = presetColors.map { "${it.name} (${it.colorHex})" }.toTypedArray()
        AlertDialog.Builder(context)
            .setTitle(R.string.car_profile_pick_color)
            .setItems(labels) { _, which ->
                onColorSelected(presetColors[which].colorHex)
            }
            .show()
    }

    private fun normalizeColor(raw: String?): String {
        val candidate = raw?.trim()?.uppercase(Locale.US).orEmpty()
        return if (Regex("^#[0-9A-F]{6}$").matches(candidate)) {
            candidate
        } else {
            "#FFFFFF"
        }
    }
}



