package com.example.contactmanagerdemo.finance.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.example.contactmanagerdemo.R
import com.example.contactmanagerdemo.finance.data.DailyBalancePoint
import com.example.contactmanagerdemo.finance.util.FinanceFormat
import kotlin.math.max

class BalanceTrendView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.finance_divider)
        strokeWidth = resources.displayMetrics.density
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.finance_primary)
        strokeWidth = resources.displayMetrics.density * 3f
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ColorUtils.setAlphaComponent(ContextCompat.getColor(context, R.color.finance_primary), 0x33)
        style = Paint.Style.FILL
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.finance_on_surface_variant)
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12f, resources.displayMetrics)
    }
    private val linePath = Path()
    private val fillPath = Path()
    private var data: List<DailyBalancePoint> = emptyList()

    fun setData(points: List<DailyBalancePoint>) {
        data = points
        contentDescription = if (points.isEmpty()) {
            "Нет данных для графика баланса"
        } else {
            "Динамика баланса от ${FinanceFormat.money(points.first().balanceCents)} до ${FinanceFormat.money(points.last().balanceCents)}"
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val left = paddingLeft + resources.displayMetrics.density * 8f
        val right = width - paddingRight - resources.displayMetrics.density * 8f
        val top = paddingTop + resources.displayMetrics.density * 12f
        val bottom = height - paddingBottom - resources.displayMetrics.density * 28f

        repeat(4) { index ->
            val y = top + (bottom - top) * index / 3f
            canvas.drawLine(left, y, right, y, gridPaint)
        }

        if (data.isEmpty()) {
            textPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("Добавьте операции, чтобы увидеть динамику", width / 2f, height / 2f, textPaint)
            return
        }

        val minValue = data.minOf { it.balanceCents }
        val maxValue = data.maxOf { it.balanceCents }
        val range = max(1L, maxValue - minValue)
        fun x(index: Int): Float = if (data.size == 1) (left + right) / 2f else left + (right - left) * index / (data.size - 1f)
        fun y(value: Long): Float = bottom - (bottom - top) * (value - minValue).toFloat() / range.toFloat()

        linePath.reset()
        data.forEachIndexed { index, point ->
            if (index == 0) linePath.moveTo(x(index), y(point.balanceCents)) else linePath.lineTo(x(index), y(point.balanceCents))
        }
        fillPath.set(linePath)
        fillPath.apply {
            lineTo(x(data.lastIndex), bottom)
            lineTo(x(0), bottom)
            close()
        }
        canvas.drawPath(fillPath, fillPaint)
        canvas.drawPath(linePath, linePaint)

        data.forEachIndexed { index, point ->
            canvas.drawCircle(x(index), y(point.balanceCents), resources.displayMetrics.density * 3.5f, linePaint)
        }

        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText(FinanceFormat.dateLabel(data.first().dateIso), left, height - paddingBottom.toFloat(), textPaint)
        textPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText(FinanceFormat.dateLabel(data.last().dateIso), right, height - paddingBottom.toFloat(), textPaint)
    }
}
