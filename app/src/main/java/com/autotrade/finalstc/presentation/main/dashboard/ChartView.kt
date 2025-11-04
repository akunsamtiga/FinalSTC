package com.autotrade.finalstc.presentation.main.dashboard

import android.graphics.Color
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material3.*
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.model.GradientColor
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun TradingLineChart(selectedAsset: Asset?, colors: DashboardColors) {
    var entries by remember { mutableStateOf(generateInitialData(100)) }
    var timeStep by remember { mutableStateOf(entries.size) }

    // Simulasi data berjalan
    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            val lastValue = entries.lastOrNull()?.y ?: 100f
            val noise = Random.nextFloat() * 4f - 2f
            val newValue = lastValue + noise
            val updatedList = ArrayList(entries).apply {
                add(Entry(timeStep.toFloat(), newValue))
                if (size > 100) removeAt(0)
            }
            entries = updatedList
            timeStep++
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(0.8.dp, colors.chartLine2.copy(alpha = 0.3f)),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = colors.chartLine2.copy(alpha = 0.1f),
                spotColor = colors.chartLine2.copy(alpha = 0.2f)
            )
            .background(
                brush = Brush.verticalGradient(
                    listOf(colors.chartGradientStart, colors.chartGradientEnd)
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        colors.accentPrimary.copy(alpha = 0.05f),
                        androidx.compose.ui.graphics.Color.Transparent,
                        colors.chartLine2.copy(alpha = 0.05f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(1000f, 1000f)
                ),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Menampilkan nama aset di atas chart
            Text(
                text = selectedAsset?.let { "${it.name} (${it.ric})" } ?: "No Asset Selected",
                style = MaterialTheme.typography.titleSmall,
                color = colors.textPrimary,
                modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = 12.dp, bottom = 8.dp)
            )

            AndroidView(
                factory = { context ->
                    LineChart(context).apply {
                        setBackgroundColor(Color.TRANSPARENT)
                        description.isEnabled = false
                        legend.isEnabled = false

                        xAxis.apply {
                            setDrawGridLines(false)
                            setDrawAxisLine(false)
                            position = XAxis.XAxisPosition.BOTTOM
                            textColor = colors.chartText.toArgb()
                            setDrawLabels(false)
                        }

                        axisLeft.apply {
                            textColor = colors.chartText.toArgb()
                            setDrawGridLines(true)
                            gridColor = colors.chartGrid.toArgb()
                            enableGridDashedLine(10f, 10f, 0f)
                        }

                        axisRight.isEnabled = false
                        setTouchEnabled(false)
                        isDragEnabled = false
                        setScaleEnabled(false)
                        setPinchZoom(false)
                        setVisibleXRangeMaximum(50f)
                        moveViewToX(entries.last().x)
                        animateX(800)
                    }
                },
                update = { chart ->
                    val gradientColors = listOf(
                        GradientColor(colors.chartLine2.toArgb(), colors.chartGradientEnd.toArgb())
                    )
                    val dataSet = LineDataSet(entries, "").apply {
                        setGradientColors(gradientColors)
                        color = colors.chartLine2.toArgb()
                        lineWidth = 1.5f
                        setDrawCircles(false)
                        setDrawValues(false)
                        mode = LineDataSet.Mode.CUBIC_BEZIER
                        cubicIntensity = 0.2f
                        setDrawFilled(true)
                        fillDrawable = chart.context.getGradientDrawable(colors)
                    }
                    chart.data = LineData(dataSet)
                    chart.notifyDataSetChanged()
                    chart.moveViewToX(entries.last().x)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            )
        }
    }
}

private fun generateInitialData(count: Int): ArrayList<Entry> {
    val list = ArrayList<Entry>()
    var value = 100f
    repeat(count) { i ->
        val noise = Random.nextFloat() * 4f - 2f
        value += noise
        list.add(Entry(i.toFloat(), value))
    }
    return list
}

// Gunakan warna dari DashboardColors untuk fill area chart
private fun android.content.Context.getGradientDrawable(colors: DashboardColors) =
    android.graphics.drawable.GradientDrawable(
        android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
        intArrayOf(colors.chartLine2.copy(alpha = 0.5f).toArgb(), Color.TRANSPARENT)
    )