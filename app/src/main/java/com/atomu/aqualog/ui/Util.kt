package com.atomu.aqualog.ui

import com.atomu.aqualog.data.Tank
import com.atomu.aqualog.data.WaterTest
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val FULL = DateTimeFormatter.ofPattern("yyyy/M/d(E)", Locale.JAPAN)
private val SHORT = DateTimeFormatter.ofPattern("M/d", Locale.JAPAN)
private val SHORT_Y = DateTimeFormatter.ofPattern("yy/M/d", Locale.JAPAN)

fun today(): Long = LocalDate.now().toEpochDay()
fun nowMinutes(): Int = java.time.LocalTime.now().let { it.hour * 60 + it.minute }
fun Long.fmtDate(): String = LocalDate.ofEpochDay(this).format(FULL)
fun Long.fmtShort(): String = LocalDate.ofEpochDay(this).format(SHORT)
fun Long.fmtShortY(): String = LocalDate.ofEpochDay(this).format(SHORT_Y)

fun Double.fmt(): String = BigDecimal.valueOf(this).stripTrailingZeros().toPlainString()
fun String.toNum(): Double? = trim().replace('，', '.').replace(',', '.').toDoubleOrNull()

fun List<Tank>.nameOf(id: Long?): String =
    if (id == null) "全水槽" else firstOrNull { it.id == id }?.name ?: "（削除された水槽）"

fun daysAgo(day: Long): String = when (val d = today() - day) {
    0L -> "今日"
    1L -> "昨日"
    else -> if (d > 0) "${d}日前" else "${-d}日後"
}

enum class Param(val label: String, val unit: String, val get: (WaterTest) -> Double?) {
    TEMP("水温", "℃", { it.temp }),
    SAL("比重", "", { it.salinity }),
    NO3("NO3", "mg/L", { it.no3 }),
    PO4("PO4", "mg/L", { it.po4 }),
    KH("KH", "dKH", { it.kh }),
    CA("Ca", "mg/L", { it.ca }),
    MG("Mg", "mg/L", { it.mg }),
}

fun WaterTest.summary(): String =
    Param.entries.mapNotNull { p -> p.get(this)?.let { "${p.label} ${it.fmt()}${p.unit}" } }
        .joinToString("　")
