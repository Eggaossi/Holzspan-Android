package de.ossi.holzspan

/** Materialgruppe, fuer die es Hersteller-Spandicken gibt. */
enum class Gruppe(val titel: String) {
    WEICH("Weichholz"),
    HART("Hartholz"),
    MDF("MDF"),
    SPERRHOLZ("Sperrholz"),
}

/**
 * Eine Holzart. [faktor] passt die Spandicke innerhalb der Gruppe an
 * (weicher = etwas mehr, haerter = etwas weniger). Startwerte, keine Messdaten.
 */
data class Holzart(
    val name: String,
    val gruppe: Gruppe,
    val faktor: Double,
    val hinweis: String,
)

val HOLZARTEN = listOf(
    Holzart("Linde", Gruppe.WEICH, 1.10, "Sehr gut für Reliefs, feine Details"),
    Holzart("Pappel", Gruppe.WEICH, 1.05, "Gut für Reliefs, leicht faserig"),
    Holzart("Zirbe", Gruppe.WEICH, 1.10, "Weich, Äste vorsichtig fräsen"),
    Holzart("Fichte / Kiefer", Gruppe.WEICH, 1.00, "Harzig, Jahresringe reißen gern aus"),
    Holzart("Erle", Gruppe.WEICH, 0.95, "Gleichmäßig, gut für Reliefs"),
    Holzart("Nussbaum", Gruppe.HART, 1.05, "Sehr schön für Reliefs"),
    Holzart("Kirsche", Gruppe.HART, 1.00, "Feinporig, neigt zu Brandspuren"),
    Holzart("Ahorn", Gruppe.HART, 0.90, "Hart, Brandspuren bei zu wenig Vorschub"),
    Holzart("Buche", Gruppe.HART, 0.90, "Hart, gedämpft besser für Reliefs"),
    Holzart("Eiche", Gruppe.HART, 0.95, "Grobporig, für feine Details schlecht"),
    Holzart("Esche", Gruppe.HART, 0.90, "Grobporig, zäh"),
    Holzart("MDF", Gruppe.MDF, 1.00, "Viel Staub, Absaugung wichtig"),
    Holzart("Multiplex Birke", Gruppe.SPERRHOLZ, 1.00, "Kompressionsfräser für saubere Kanten"),
    Holzart("Pappel-Sperrholz", Gruppe.SPERRHOLZ, 1.10, "Weich, Deckschicht reißt leicht aus"),
)

enum class FraeserTyp(val titel: String) {
    UPCUT("Upcut"),
    DOWNCUT("Downcut"),
    KOMPRESSION("Kompression"),
    KUGEL("Kugel"),
}

enum class Bearbeitung(val titel: String) {
    NUT("Nut"),
    KONTUR("Kontur"),
    TASCHE("Tasche"),
    RELIEF("Relief schlichten"),
}

enum class Steifigkeit(val titel: String, val faktor: Double, val beschreibung: String) {
    LEICHT("Leicht", 0.30, "Hobby-Fräse mit Riemen oder Alu-Profil"),
    MITTEL("Mittel", 0.45, "Stabile Hobby-Fräse mit Spindeln"),
    STEIF("Steif", 0.70, "Stahl- oder Gussrahmen"),
}

val DURCHMESSER = listOf(2.0, 3.0, 3.175, 4.0, 6.0, 6.35, 8.0, 10.0, 12.0)
val SCHNEIDEN = listOf(1, 2, 3)

/**
 * Spandicke in mm pro Schneide, Mitte der Freud-Tabelle
 * (Hartmetall, Schnitttiefe 1x D) fuer 1/8", 1/4", 3/8", 1/2".
 */
private val TABELLE_D = doubleArrayOf(3.175, 6.35, 9.525, 12.7)
private val TABELLE: Map<Gruppe, DoubleArray> = mapOf(
    Gruppe.WEICH to doubleArrayOf(0.125, 0.275, 0.445, 0.545),
    Gruppe.HART to doubleArrayOf(0.090, 0.240, 0.385, 0.495),
    Gruppe.MDF to doubleArrayOf(0.140, 0.380, 0.495, 0.635),
    Gruppe.SPERRHOLZ to doubleArrayOf(0.105, 0.190, 0.420, 0.495),
)

/** Hersteller-Spandicke fuer Durchmesser [d] (linear interpoliert). */
fun herstellerSpandicke(gruppe: Gruppe, d: Double): Double {
    val werte = TABELLE.getValue(gruppe)
    if (d <= TABELLE_D[0]) return werte[0] * d / TABELLE_D[0]
    if (d >= TABELLE_D.last()) return werte.last()
    for (i in 0 until TABELLE_D.size - 1) {
        val d0 = TABELLE_D[i]
        val d1 = TABELLE_D[i + 1]
        if (d in d0..d1) {
            val t = (d - d0) / (d1 - d0)
            return werte[i] + t * (werte[i + 1] - werte[i])
        }
    }
    return werte.last()
}
