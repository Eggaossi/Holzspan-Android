package de.ossi.holzspan.ui

import android.content.SharedPreferences
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.ossi.holzspan.Bearbeitung
import de.ossi.holzspan.DURCHMESSER
import de.ossi.holzspan.Eingabe
import de.ossi.holzspan.Ergebnis
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
    var maschineOffen by rememberSaveable { mutableStateOf(false) }

    var maschine by remember { mutableStateOf(ladeMaschine(prefs)) }
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
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Kopf(maschine) { maschineOffen = true }

            ErgebnisKarte(ergebnis)

            Karte {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Auswahl("Fräser", FraeserTyp.entries, typ, { it.titel }, Modifier.weight(1.4f)) {
                        typ = it
                        if (it == FraeserTyp.KUGEL) bearbeitung = Bearbeitung.RELIEF
                        else if (bearbeitung == Bearbeitung.RELIEF) bearbeitung = Bearbeitung.KONTUR
                    }
                    Auswahl("Ø", DURCHMESSER, durchmesser, { mm(it) }, Modifier.weight(1.2f)) { durchmesser = it }
                    Auswahl("Schneiden", SCHNEIDEN, schneiden, { "$it" }, Modifier.weight(1f)) { schneiden = it }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Auswahl(
                        "Holz", HOLZARTEN.indices.toList(), holzIndex,
                        { HOLZARTEN[it].name }, Modifier.weight(1f),
                    ) { holzIndex = it }
                    Auswahl("Bearbeitung", Bearbeitung.entries, bearbeitung, { it.titel }, Modifier.weight(1f)) {
                        bearbeitung = it
                    }
                }
            }

            Hinweise(ergebnis)

            Spacer(Modifier.weight(1f))
            Text(
                "Startwerte aus Hersteller-Tabellen – immer erst an einem Probestück testen.",
                color = Nussbaum.copy(alpha = 0.6f),
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }

    if (maschineOffen) {
        MaschineDialog(maschine, onSchliessen = { maschineOffen = false }) {
            maschine = it
            speichereMaschine(prefs, it)
            maschineOffen = false
        }
    }
}

private fun ladeMaschine(p: SharedPreferences) = Maschine(
    drehzahlMin = p.getInt("nMin", 10000),
    drehzahlMax = p.getInt("nMax", 24000),
    vorschubMax = p.getInt("fMax", 3000),
    steifigkeit = Steifigkeit.entries.getOrNull(p.getInt("steifigkeit", 0)) ?: Steifigkeit.LEICHT,
)

private fun speichereMaschine(p: SharedPreferences, m: Maschine) {
    p.edit()
        .putInt("nMin", m.drehzahlMin)
        .putInt("nMax", m.drehzahlMax)
        .putInt("fMax", m.vorschubMax)
        .putInt("steifigkeit", m.steifigkeit.ordinal)
        .apply()
}

@Composable
private fun Kopf(m: Maschine, onMaschine: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp)) {
        Column(Modifier.weight(1f)) {
            Text(
                "Holzspan",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                color = Nussbaum,
            )
            Text("Fräswerte für Holz", color = Kupfer, fontSize = 12.sp)
        }
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Ahorn,
            border = BorderStroke(1.dp, Maserung),
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onMaschine),
        ) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), horizontalAlignment = Alignment.End) {
                Text("Maschine", color = Nussbaum, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "${m.steifigkeit.titel} · max ${ganz(m.vorschubMax)}",
                    color = Kupfer,
                    fontSize = 11.sp,
                )
            }
        }
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
        val mitte = Offset(size.width * 0.82f, size.height * 0.8f)
        drawCircle(Maserung.copy(alpha = 0.4f), radius = 38f, center = mitte, style = Stroke(2f))
        drawCircle(Maserung.copy(alpha = 0.3f), radius = 22f, center = mitte, style = Stroke(2f))
    }
}

@Composable
private fun Karte(inhalt: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Ahorn,
        border = BorderStroke(1.dp, Maserung),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            inhalt()
        }
    }
}

/** Ausklapp-Feld: Beschriftung oben, Wert darunter, Antippen oeffnet die Liste. */
@Composable
private fun <T> Auswahl(
    titel: String,
    eintraege: List<T>,
    gewaehlt: T,
    name: (T) -> String,
    modifier: Modifier,
    onWahl: (T) -> Unit,
) {
    var offen by remember { mutableStateOf(false) }
    Box(modifier) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Birke,
            border = BorderStroke(1.dp, Maserung),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { offen = true },
        ) {
            Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                Text(titel, color = Kupfer, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        name(gewaehlt),
                        color = Dunkelholz,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text("▾", color = Nussbaum, fontSize = 14.sp)
                }
            }
        }
        DropdownMenu(
            expanded = offen,
            onDismissRequest = { offen = false },
            modifier = Modifier
                .background(Ahorn)
                .heightIn(max = 420.dp),
        ) {
            eintraege.forEach { e ->
                val aktiv = e == gewaehlt
                DropdownMenuItem(
                    text = {
                        Text(
                            name(e),
                            color = if (aktiv) Nussbaum else Dunkelholz,
                            fontWeight = if (aktiv) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                    onClick = { onWahl(e); offen = false },
                )
            }
        }
    }
}

@Composable
private fun ErgebnisKarte(e: Ergebnis) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Nussbaum,
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row {
                Wert("Drehzahl", ganz(e.drehzahl), "U/min", Modifier.weight(1f))
                Wert("Vorschub", ganz(e.vorschub), "mm/min", Modifier.weight(1f))
                Wert("Eintauchen", ganz(e.eintauchen), "mm/min", Modifier.weight(1f))
            }
            Row {
                Wert(
                    "Tiefe",
                    e.zustelltiefe?.let { komma(it, 1) } ?: "Modell",
                    if (e.zustelltiefe != null) "mm" else "",
                    Modifier.weight(1f),
                )
                Wert("Seitlich", komma(e.seitlich, 2), "mm", Modifier.weight(1f))
                if (e.rillenhoehe != null) {
                    Wert("Rille", komma(e.rillenhoehe, 3), "mm", Modifier.weight(1f))
                } else {
                    Wert("Span", komma(e.spandicke, 3), "mm", Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun Wert(titel: String, zahl: String, einheit: String, modifier: Modifier) {
    Column(modifier) {
        Text(titel, color = Ahorn.copy(alpha = 0.7f), fontSize = 11.sp)
        Text(
            zahl,
            color = Ahorn,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            maxLines = 1,
        )
        if (einheit.isNotEmpty()) Text(einheit, color = Kupfer, fontSize = 11.sp)
    }
}

/** Warnungen zuerst, insgesamt 2 Zeilen; Antippen zeigt alles. */
@Composable
private fun Hinweise(e: Ergebnis) {
    var alle by rememberSaveable { mutableStateOf(false) }
    val liste = e.warnungen.map { true to it } + e.tipps.map { false to it }
    if (liste.isEmpty()) return
    val sichtbar = if (alle) liste else liste.take(2)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { alle = !alle }
            .animateContentSize()
            .padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        sichtbar.forEach { (warnung, text) ->
            if (warnung) {
                Surface(shape = RoundedCornerShape(10.dp), color = Warnrot) {
                    Text(
                        text,
                        color = Ahorn,
                        fontSize = 12.sp,
                        maxLines = if (alle) 4 else 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            } else {
                Text(
                    "•  $text",
                    color = Dunkelholz,
                    fontSize = 12.sp,
                    maxLines = if (alle) 4 else 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (liste.size > 2) {
            Text(
                if (alle) "weniger" else "+ ${liste.size - 2} weitere Hinweise",
                color = Kupfer,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun MaschineDialog(m: Maschine, onSchliessen: () -> Unit, onSpeichern: (Maschine) -> Unit) {
    var steif by remember { mutableStateOf(m.steifigkeit) }
    var nMin by remember { mutableStateOf(m.drehzahlMin.toString()) }
    var nMax by remember { mutableStateOf(m.drehzahlMax.toString()) }
    var fMax by remember { mutableStateOf(m.vorschubMax.toString()) }

    AlertDialog(
        onDismissRequest = onSchliessen,
        containerColor = Ahorn,
        title = {
            Text("Meine Maschine", color = Nussbaum, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Auswahl("Steifigkeit", Steifigkeit.entries, steif, { it.titel }, Modifier.fillMaxWidth()) {
                    steif = it
                }
                Text(steif.beschreibung, color = Nussbaum, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ZahlFeld("Drehzahl min", nMin, Modifier.weight(1f)) { nMin = it }
                    ZahlFeld("Drehzahl max", nMax, Modifier.weight(1f)) { nMax = it }
                }
                ZahlFeld("Max. Vorschub mm/min (GRBL \$110)", fMax, Modifier.fillMaxWidth()) { fMax = it }
                Spacer(Modifier.height(2.dp))
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSpeichern(
                    Maschine(
                        drehzahlMin = nMin.toIntOrNull()?.coerceAtLeast(1000) ?: m.drehzahlMin,
                        drehzahlMax = nMax.toIntOrNull()?.coerceAtLeast(1000) ?: m.drehzahlMax,
                        vorschubMax = fMax.toIntOrNull()?.coerceAtLeast(100) ?: m.vorschubMax,
                        steifigkeit = steif,
                    )
                )
            }) { Text("Speichern", color = Nussbaum, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onSchliessen) { Text("Abbrechen", color = Kupfer) }
        },
    )
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

private fun ganz(n: Int): String = String.format(Locale.GERMANY, "%,d", n)
private fun komma(d: Double, stellen: Int): String = String.format(Locale.GERMANY, "%.${stellen}f", d)
private fun mm(d: Double): String =
    if (d % 1.0 == 0.0) "${d.toInt()} mm" else "${d.toString().replace('.', ',')} mm"
