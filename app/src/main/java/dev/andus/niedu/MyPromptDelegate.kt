package dev.andus.niedu

import android.text.Editable
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSession.PromptDelegate
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import android.text.TextWatcher
import androidx.core.graphics.toColorInt

class MyPromptDelegate(private val activity: AppCompatActivity) : PromptDelegate {

    companion object {
        private val HEX_COLOR_REGEX = Regex("^#[0-9a-fA-F]{6}$")
        private val UTC: TimeZone = TimeZone.getTimeZone("UTC")
    }

    private fun <T : PromptDelegate.BasePrompt> GeckoResult<PromptDelegate.PromptResponse>.dismissOn(
        builder: MaterialAlertDialogBuilder,
        prompt: T
    ) {
        builder.setNegativeButton("Anuluj") { dialog, _ ->
            dialog.dismiss()
            complete(prompt.dismiss())
        }
        builder.setOnCancelListener {
            complete(prompt.dismiss())
        }
    }

    override fun onChoicePrompt(
        session: GeckoSession,
        prompt: PromptDelegate.ChoicePrompt
    ): GeckoResult<PromptDelegate.PromptResponse> {
        val result = GeckoResult<PromptDelegate.PromptResponse>()
        val choices = prompt.choices
        val items = choices.map { it.label }.toTypedArray()

        val builder = MaterialAlertDialogBuilder(activity)

        if (prompt.type == PromptDelegate.ChoicePrompt.Type.MULTIPLE) {
            val checkedItems = choices.map { it.selected }.toBooleanArray()
            builder.setTitle(prompt.title?.takeIf { it.isNotEmpty() } ?: "Wybierz opcje:")
            builder.setMultiChoiceItems(items, checkedItems) { _, which, isChecked ->
                checkedItems[which] = isChecked
            }
            builder.setPositiveButton("OK") { _, _ ->
                val selectedIds = choices.filterIndexed { i, _ -> checkedItems[i] }.map { it.id }
                result.complete(prompt.confirm(selectedIds.toTypedArray()))
            }
        } else {
            val initialIndex = choices.indexOfFirst { it.selected }.coerceAtLeast(0)
            var selectedIndex = initialIndex
            builder.setTitle(prompt.title?.takeIf { it.isNotEmpty() } ?: "Wybierz opcje:")
            builder.setSingleChoiceItems(items, initialIndex) { _, which ->
                selectedIndex = which
            }
            builder.setPositiveButton("OK") { _, _ ->
                result.complete(prompt.confirm(choices[selectedIndex].id))
            }
        }

        result.dismissOn(builder, prompt)
        builder.show()
        return result
    }

    override fun onDateTimePrompt(
        session: GeckoSession,
        prompt: PromptDelegate.DateTimePrompt
    ): GeckoResult<PromptDelegate.PromptResponse> {
        val result = GeckoResult<PromptDelegate.PromptResponse>()

        if (prompt.type != PromptDelegate.DateTimePrompt.Type.DATE) {
            result.complete(prompt.dismiss())
            return result
        }

        val pickerBuilder = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Wybierz datę")

        prompt.defaultValue?.let { defaultVal ->
            runCatching {
                val (y, m, d) = defaultVal.split("-").map { it.toInt() }
                Calendar.getInstance(UTC).apply { set(y, m - 1, d) }.timeInMillis
            }.onSuccess { pickerBuilder.setSelection(it) }
        }

        val datePicker = pickerBuilder.build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            val cal = Calendar.getInstance(UTC).apply { timeInMillis = selection }
            val dateString = String.format(
                Locale.US, "%04d-%02d-%02d",
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH)
            )
            result.complete(prompt.confirm(dateString))
        }

        datePicker.addOnNegativeButtonClickListener { result.complete(prompt.dismiss()) }
        datePicker.addOnCancelListener { result.complete(prompt.dismiss()) }
        datePicker.show(activity.supportFragmentManager, "MATERIAL_DATE_PICKER")

        return result
    }

    override fun onColorPrompt(
        session: GeckoSession,
        prompt: PromptDelegate.ColorPrompt
    ): GeckoResult<PromptDelegate.PromptResponse> {
        val result = GeckoResult<PromptDelegate.PromptResponse>()
        val defaultVal = prompt.defaultValue ?: "#000000"

        val colorPreview = View(activity).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 100
            ).apply { bottomMargin = 32 }
            runCatching { setBackgroundColor(defaultVal.toColorInt()) }
        }

        val inputLayout = TextInputLayout(activity)

        val input = TextInputEditText(activity).apply {
            setText(defaultVal)
            maxLines = 1
            inputType = android.text.InputType.TYPE_CLASS_TEXT

            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val text = s?.toString() ?: return
                    if (text.matches(HEX_COLOR_REGEX)) {
                        inputLayout.error = null
                        runCatching { colorPreview.setBackgroundColor(text.toColorInt()) }
                    } else {
                        inputLayout.error = "Nieprawidłowy format — wpisz np. #FF0000"
                    }
                }
            })
        }

        inputLayout.apply {
            addView(input)
            hint = "Kolor HEX (np. #FF0000)"
        }

        val container = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(64, 32, 64, 0)
            addView(colorPreview)
            addView(inputLayout)
        }

        val builder = MaterialAlertDialogBuilder(activity)
            .setTitle("Wybierz kolor")
            .setView(container)
            .setPositiveButton("OK", null)
            .setNegativeButton("Anuluj") { _, _ ->
                result.complete(prompt.dismiss())
            }
            .setOnCancelListener {
                result.complete(prompt.dismiss())
            }

        val dialog = builder.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val selectedColor = input.text?.toString() ?: ""
            if (selectedColor.matches(HEX_COLOR_REGEX)) {
                result.complete(prompt.confirm(selectedColor))
                dialog.dismiss()
            } else {
                inputLayout.error = "Nieprawidłowy format — wpisz np. #FF0000"
            }
        }

        return result
    }
}
