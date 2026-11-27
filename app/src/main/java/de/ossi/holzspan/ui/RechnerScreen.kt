@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)

package de.ossi.holzspan.ui

import android.content.SharedPreferences
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.ossi.holzspan.Bearbeitung
import de.ossi.holzspan.DURCHMESSER
import de.ossi.holzspan.Eingabe
import de.ossi.holzspan.FraeserTyp
import de.ossi.holzspan.HOLZARTEN
import de.ossi.holzspan.Maschine
import de.ossi.holzspan.SCHNEIDEN
import de.ossi.holzspan.Steifigkeit
import de.ossi.holzspan.berechne
import java.util.Locale
import kotlin.math.sin

@Composable
fun RechnerScreen(prefs: SharedPreferences) {
    var typ by rememberSaveable { mutableStateOf(FraeserTyp.UPCUT) }
    var durchmesser by rememberSaveable { mutableStateOf(6.0) }
    var schneiden by rememberSaveable { mutableStateOf(2) }
    var holzIndex by rememberSaveable { mutableStateOf(0) }
    var bearbeitung by rememberSaveable { mutableStateOf(Bearbeitung.KONTUR) }

    var steifigkeit by remember {
        mutableStateOf(
            Steifigkeit.entries.getOrNull(prefs.getInt("steifigkeit", 0)) ?: Steifigkeit.LEICHT
        )
    }
    var nMin by remember { mutableStateOf(prefs.getInt("nMin", 10000).toString()) }
    var nMax by remember { mutableStateOf(prefs.getInt("nMax", 24000).toString()) }
    var fMax by remember { mutableStateOf(prefs.getInt("fMax", 3000).toString()) }

    val maschine = Maschine(
        drehzahlMin = nMin.toIntOrNull()?.coerceAtLeast(1000) ?: 10000,
        drehzahlMax = nMax.toIntOrNull()?.coerceAtLeast(1000) ?: 24000,
        vorschubMax = fMax.toIntOrNull()?.coerceAtLeast(100) ?: 3000,
        steifigkeit = steifigkeit,
    )
    val holz = HOLZARTEN[holzIndex]
    val ergebnis = berechne(Eingabe(typ, durchmesser, schneiden, holz, bearbeitung, maschine))

    Box(
        Modifier
            .fillMaxSize()
            .background(Birke)
    ) {
        HolzMaserung()
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Kopf()

            Karte("Fraeser") {
                Auswahl(FraeserTyp.entries, typ, { it.titel }) {
                    typ = it
                    if (it == FraeserTyp.KUGEL) bearbeitung = Bearbeitung.RELIEF
                    else if (bearbeitung == Bearbeitung.RELIEF) bearbeitung = Bearbeitung.KONTUR
                }
                Unterzeile("Durchmesser")
                Auswahl(DURCHMESSER, durchmesser, { mm(it) }) { durchmesser = it }
                Unterzeile("Schneiden")
                Auswahl(SCHNEIDEN, schneiden, { "$it" }) { schneiden = it }
            }

            Karte("Holz") {
                Auswahl(HOLZARTEN.indices.toList(), holzIndex, { HOLZARTEN[it].name }) { holzIndex = it }
                Text(
                    "${holz.gruppe.titel} · ${holz.hinweis}",
                    color = Nussbaum,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }

            Karte("Bearbeitung") {
                Auswahl(Bearbeitung.entries, bearbeitung, { it.titel }) { bearbeitung = it }
            }

            ErgebnisKarte(ergebnis)

            Karte("Meine Maschine") {
                Auswahl(Steifigkeit.entries, steifigkeit, { it.titel }) {
                    steifigkeit = it
                    prefs.edit().putInt("steifigkeit", it.ordinal).apply()
                }
                Text(steifigkeit.beschreibung, color = Nussbaum, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ZahlFeld("Drehzahl min", nMin, Modifier.weight(1f)) {
                        nMin = it; it.toIntOrNull()?.let { v -> prefs.edit().putInt("nMin", v).apply() }
                    }
                    ZahlFeld("Drehzahl max", nMax, Modifier.weight(1f)) {
                        nMax = it; it.toIntOrNull()?.let { v -> prefs.edit().putInt("nMax", v).apply() }
                    }
                }
                ZahlFeld("Max. Vorschub mm/min (GRBL \$110)", fMax, Modifier.fillMaxWidth()) {
                    fMax = it; it.toIntOrNull()?.let { v -> prefs.edit().putInt("fMax", v).apply() }
                }
            }

            Text(
                "Startwerte aus Hersteller-Tabellen. Immer erst an einem Probestueck testen " +
                    "und nach Geraeusch und Spaenen anpassen.",
                color = Nussbaum.copy(alpha = 0.7f),
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun Kopf() {
    Column(Modifier.padding(top = 8.dp, bottom = 4.dp, start = 4.dp)) {
        Text(
            "Holzspan",
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 36.sp,
            letterSpacing = 1.sp,
            color = Nussbaum,
        )
        Text("Fraeswerte fuer Holz", color = Kupfer, fontSize = 15.sp, letterSpacing = 0.5.sp)
    }
}

/** Zarte Holzmaserung im Hintergrund. */
@Composable
private fun HolzMaserung() {
    Canvas(Modifier.fillMaxSize()) {
        val linien = 26
        for (i in 0 until linien) {
            val yBasis = size.height * i / linien
            val path = Path()
            var x = 0f
            path.moveTo(0f, yBasis)
            while (x <= size.width) {
                val y = yBasis + 10f * sin(x / 140f + i * 0.9f) + 4f * sin(x / 37f + i)
                path.lineTo(x, y)
                x += 12f
            }
            drawPath(path, Maserung.copy(alpha = 0.55f), style = Stroke(width = 1.5f))
        }
        drawCircle(Maserung.copy(alpha = 0.4f), radius = 38f, center = Offset(size.width * 0.82f, size.height * 0.3f), style = Stroke(2f))
        drawCircle(Maserung.copy(alpha = 0.3f), radius = 22f, center = Offset(size.width * 0.82f, size.height * 0.3f), style = Stroke(2f))
    }
}

@Composable
private fun Karte(titel: String, inhalt: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Ahorn,
        border = BorderStroke(1.dp, Maserung),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                titel.uppercase(),
                color = Kupfer,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                letterSpacing = 1.5.sp,
            )
            inhalt()
        }
    }
}

@Composable
private fun Unterzeile(text: String) {
    Text(text, color = Nussbaum, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
}

@Composable
private fun <T> Auswahl(eintraege: List<T>, gewaehlt: T, name: (T) -> String, onWahl: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        eintraege.forEach { e ->
            FilterChip(
                selected = e == gewaehlt,
                onClick = { onWahl(e) },
                label = { Text(name(e)) },
                shape = RoundedCornerShape(12.dp),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Birke,
                    labelColor = Dunkelholz,
                    selectedContainerColor = Nussbaum,
                    selectedLabelColor = Ahorn,
                ),
            )
        }
    }
}

@Composable
private fun ZahlFeld(label: String, wert: String, modifier: Modifier, onAendern: (String) -> Unit) {
    OutlinedTextField(
        value = wert,
        onValueChange = { neu -> onAendern(neu.filter { it.isDigit() }.take(6)) },
        label = { Text(label, fontSize = 12.sp) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Nussbaum,
            unfocusedBorderColor = Maserung,
            focusedLabelColor = Nussbaum,
            cursorColor = Nussbaum,
        ),
        modifier = modifier,
    )
}

@Composable
private fun ErgebnisKarte(e: de.ossi.holzspan.Ergebnis) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Nussbaum,
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "DEINE WERTE",
                color = Kupfer,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                letterSpacing = 1.5.sp,
            )
            Row {
                Wert("Drehzahl", ganz(e.drehzahl), "U/min", Modifier.weight(1f))
                Wert("Vorschub", ganz(e.vorschub), "mm/min", Modifier.weight(1f))
            }
            Row {
                Wert("Eintauchen", ganz(e.eintauchen), "mm/min", Modifier.weight(1f))
                Wert(
                    "Zustelltiefe",
                    e.zustelltiefe?.let { komma(it, 1) } ?: "Modell",
                    if (e.zustelltiefe != null) "mm" else "",
                    Modifier.weight(1f),
                )
            }
            Row {
                Wert("Seitlich", komma(e.seitlich, 2), "mm", Modifier.weight(1f))
                if (e.rillenhoehe != null) {
                    Wert("Rillenhoehe", komma(e.rillenhoehe, 3), "mm", Modifier.weight(1f))
                } else {
                    Wert("Spandicke", komma(e.spandicke, 3), "mm", Modifier.weight(1f))
                }
            }

            if (e.warnungen.isNotEmpty() || e.tipps.isNotEmpty()) {
                HorizontalDivider(color = Ahorn.copy(alpha = 0.25f))
            }
            e.warnungen.forEach { w ->
                Surface(shape = RoundedCornerShape(12.dp), color = Warnrot) {
                    Text("⚠  $w", color = Ahorn, fontSize = 13.sp, modifier = Modifier.padding(10.dp))
                }
            }
            e.tipps.forEach { t ->
                Text("•  $t", color = Ahorn.copy(alpha = 0.9f), fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun Wert(titel: String, zahl: String, einheit: String, modifier: Modifier) {
    Column(modifier) {
        Text(titel, color = Ahorn.copy(alpha = 0.75f), fontSize = 12.sp)
        Row {
            Text(zahl, color = Ahorn, fontSize = 26.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
            if (einheit.isNotEmpty()) {
                Text(
                    " $einheit",
                    color = Kupfer,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 11.dp),
                )
            }
        }
    }
}

private fun ganz(n: Int): String = String.format(Locale.GERMANY, "%,d", n)
private fun komma(d: Double, stellen: Int): String = String.format(Locale.GERMANY, "%.${stellen}f", d)
private fun mm(d: Double): String =
    if (d % 1.0 == 0.0) "${d.toInt()} mm" else "${d.toString().replace('.', ',')} mm"
