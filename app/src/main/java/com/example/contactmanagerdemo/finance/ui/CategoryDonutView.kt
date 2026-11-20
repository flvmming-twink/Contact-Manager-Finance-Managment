package com.example.contactmanagerdemo.finance.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.contactmanagerdemo.R
import com.example.contactmanagerdemo.finance.data.CategorySpend
import com.example.contactmanagerdemo.finance.util.FinanceFormat
import kotlin.math.min

class CategoryDonutView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = ContextCompat.getColor(context, R.color.finance_divider)
    }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.finance_on_surface)
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.finance_on_surface_variant)
        textAlign = Paint.Align.CENTER
    }
    private val bounds = RectF()

    private var data: List<CategorySpend> = emptyList()

    fun setData(items: List<CategorySpend>) {
        data = items
        val total = items.sumOf { it.amountCents }
        contentDescription = if (total == 0L) {
            "Нет расходов за выбранный месяц"
        } else {
            "Расходы по категориям: " + items.joinToString { "${it.categoryName} ${FinanceFormat.money(it.amountCents)}" }
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = min(width, height).toFloat()
        val stroke = size * 0.13f
        val radius = size / 2f - stroke
        val cx = width / 2f
        val cy = height / 2f
        bounds.set(cx - radius, cy - radius, cx + radius, cy + radius)
        arcPaint.strokeWidth = stroke
        arcPaint.strokeCap = Paint.Cap.BUTT
        trackPaint.strokeWidth = stroke
        canvas.drawArc(bounds, -90f, 360f, false, trackPaint)

        val total = data.sumOf { it.amountCents }
        var start = -90f
        if (total > 0L) {
            data.forEach { item ->
                val sweep = item.amountCents.toFloat() / total.toFloat() * 360f
                arcPaint.color = runCatching { Color.parseColor(item.color) }.getOrDefault(Color.GRAY)
                canvas.drawArc(bounds, start, sweep, false, arcPaint)
                start += sweep
            }
        }

        valuePaint.textSize = size * 0.105f
        labelPaint.textSize = size * 0.06f
        val value = if (total == 0L) "0 ₽" else FinanceFormat.money(total)
        canvas.drawText(value, cx, cy - 2f, valuePaint)
        canvas.drawText("Расходы", cx, cy + size * 0.11f, labelPaint)
    }
}
