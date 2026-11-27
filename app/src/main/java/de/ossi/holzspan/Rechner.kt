package de.ossi.holzspan

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class Maschine(
    val drehzahlMin: Int = 10000,
    val drehzahlMax: Int = 24000,
    val vorschubMax: Int = 3000, // GRBL $110 / $111 in mm/min
    val steifigkeit: Steifigkeit = Steifigkeit.LEICHT,
)

data class Eingabe(
    val typ: FraeserTyp,
    val durchmesser: Double,
    val schneiden: Int,
    val holz: Holzart,
    val bearbeitung: Bearbeitung,
    val maschine: Maschine,
)

data class Ergebnis(
    val drehzahl: Int,
    val vorschub: Int,
    val eintauchen: Int,
    val zustelltiefe: Double?,   // null = kommt aus dem Relief-Modell
    val seitlich: Double,        // seitliche Zustellung in mm
    val spandicke: Double,       // tatsaechliche Spandicke mm/Schneide
    val rillenhoehe: Double?,    // nur Kugelfraeser
    val warnungen: List<String>,
    val tipps: List<String>,
)

private const val ZIEL_DREHZAHL = 18000

fun berechne(e: Eingabe): Ergebnis {
    val d = e.durchmesser
    val m = e.maschine
    val warnungen = mutableListOf<String>()
    val tipps = mutableListOf<String>()
    val hart = e.holz.gruppe == Gruppe.HART

    // 1) Ziel-Spandicke: Hersteller-Wert x Holzart x Maschinen-Steifigkeit
    var spandicke = herstellerSpandicke(e.holz.gruppe, d) * e.holz.faktor * m.steifigkeit.faktor

    // 2) Zustelltiefe (ap) und seitliche Zustellung (ae)
    val tiefenFaktor = when (m.steifigkeit) {
        Steifigkeit.LEICHT -> 0.4
        Steifigkeit.MITTEL -> 0.6
        Steifigkeit.STEIF -> 0.8
    } * (if (hart) 0.8 else 1.0)

    var ap: Double?
    val ae: Double
    when (e.bearbeitung) {
        Bearbeitung.NUT -> { ap = d * tiefenFaktor; ae = d }
        Bearbeitung.TASCHE -> { ap = d * tiefenFaktor * 1.2; ae = d * 0.45 }
        Bearbeitung.KONTUR -> { ap = d * tiefenFaktor * 2.5; ae = d * 0.3 }
        Bearbeitung.RELIEF -> { ap = null; ae = d * 0.10 }
    }
    ap = ap?.let { (it * 10).roundToInt() / 10.0 }

    // Hersteller: bei 2x D Tiefe -25 %, bei 3x D -50 % Spandicke
    if (ap != null) {
        val verhaeltnis = ap / d
        if (verhaeltnis >= 3.0) spandicke *= 0.5
        else if (verhaeltnis >= 2.0) spandicke *= 0.75
    }

    // 3) Spanausduennung: bei kleiner seitlicher Zustellung wird der Span
    //    duenner, also darf der Vorschub hoeher sein (begrenzt auf 1,6x)
    var ausduennung = 1.0
    if (e.bearbeitung != Bearbeitung.RELIEF && ae < d / 2) {
        val x = 1.0 - 2.0 * ae / d
        ausduennung = min(1.6, 1.0 / sqrt(1.0 - x * x))
    }
    if (e.typ == FraeserTyp.KUGEL) spandicke *= 0.8

    // 4) Drehzahl und Vorschub, auf die Maschine begrenzt
    var drehzahl = ZIEL_DREHZAHL.coerceIn(m.drehzahlMin, m.drehzahlMax).toDouble()
    var vorschub = drehzahl * e.schneiden * spandicke * ausduennung
    if (vorschub > m.vorschubMax) {
        // Vorschub zu hoch fuer die Maschine: Drehzahl absenken, damit der Span
        // dick genug bleibt (duenne Spaene = Reibung = Brandspuren)
        vorschub = m.vorschubMax.toDouble()
        drehzahl = max(m.drehzahlMin.toDouble(), vorschub / (e.schneiden * spandicke * ausduennung))
        tipps += "Vorschub auf Maschinen-Maximum begrenzt, Drehzahl dafür gesenkt."
    }
    val echteSpandicke = vorschub / (drehzahl * e.schneiden * ausduennung)
    if (echteSpandicke < spandicke * 0.6) {
        warnungen += "Brandgefahr: Span zu dünn, weil die Maschine nicht schneller fahren kann. " +
            "Weniger Schneiden oder kleineren Fräser nehmen."
    }
    if (echteSpandicke < 0.02) {
        warnungen += "Sehr dünner Span (unter 0,02 mm): Fräser reibt eher als er schneidet."
    }

    val eintauchen = vorschub * (if (e.typ == FraeserTyp.KUGEL) 0.5 else 0.33)

    // 5) Rillenhoehe beim Kugelfraeser: h = R - sqrt(R^2 - (s/2)^2)
    val rillen = if (e.typ == FraeserTyp.KUGEL) {
        val r = d / 2
        r - sqrt(r * r - (ae / 2) * (ae / 2))
    } else null

    // 6) Warnungen und Tipps
    if (d <= 3.2) warnungen += "Kleiner Fräser: bricht leicht. Erst mit einem Probestück testen."
    if (e.typ == FraeserTyp.KUGEL && e.bearbeitung != Bearbeitung.RELIEF) {
        tipps += "Kugelfräser sind zum Schlichten gedacht. Zum Schruppen besser Schaftfräser."
    }
    if (e.typ != FraeserTyp.KUGEL && e.bearbeitung == Bearbeitung.RELIEF) {
        tipps += "Für schöne Reliefs zum Schlichten einen Kugelfräser nehmen."
    }
    when (e.typ) {
        FraeserTyp.DOWNCUT -> tipps += "Downcut: saubere Oberkante, aber Späne bleiben in der Nut. Nicht zu tief zustellen."
        FraeserTyp.KOMPRESSION -> tipps += "Kompression: erste Zustellung tiefer als der Upcut-Teil, sonst keine saubere Oberkante."
        else -> {}
    }
    if (e.bearbeitung == Bearbeitung.RELIEF) {
        tipps += "Schlichten etwa 45 Grad zur Faser fräsen, dann reißt weniger aus."
        tipps += "Weniger seitliche Zustellung = glatter, aber deutlich länger."
    }
    tipps += e.holz.hinweis

    return Ergebnis(
        drehzahl = (drehzahl / 100).roundToInt() * 100,
        vorschub = (vorschub / 10).roundToInt() * 10,
        eintauchen = (eintauchen / 10).roundToInt() * 10,
        zustelltiefe = ap,
        seitlich = (ae * 100).roundToInt() / 100.0,
        spandicke = echteSpandicke,
        rillenhoehe = rillen,
        warnungen = warnungen,
        tipps = tipps,
    )
}
