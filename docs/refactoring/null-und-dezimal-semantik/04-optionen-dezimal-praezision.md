# 04 — Optionen: Numerische Präzision (double vs. BigDecimal)

> Beantwortet die Frage: **Reicht `double`? Sollte man immer mit `BigDecimal`
> rechnen? Was ist der beste Weg, immer vollständig richtig mit Kommazahlen
> zu rechnen?**

## §0 Grundlagen: Was `double` kann und was nicht

IEEE-754 `binary64` hat 53 Bit Mantisse ≈ **15–16 signifikante
Dezimalstellen** und stellt Dezimalbrüche binär dar:

- `0.1 + 0.2 == 0.30000000000000004` — Dezimalbrüche sind i. d. R. binär
  **nicht exakt darstellbar**. Nicht einmal `0.1` selbst.
- Ganzzahlen bis 2⁵³ (≈ 9·10¹⁵) sind exakt; darüber Lücken.
- `DECIMAL(19,4)` (üblicher Geldtyp) überschreitet die 16 Stellen →
  garantierter Darstellungsfehler bei großen Beträgen.
- Naive Summation akkumuliert Rundungsfehler mit O(n); bei 10⁸ Zellen und
  gemischten Größenordnungen ist der Drift real messbar.
- Assoziativität gilt nicht: `(a+b)+c ≠ a+(b+c)` — Aggregationsreihenfolge
  (Segment-Rollup vs. Direktsumme!) kann das Ergebnis in den letzten Bits
  ändern.

Daraus folgt die zentrale Einsicht: **„Vollständig richtig mit Kommazahlen
rechnen" ist keine Eigenschaft eines Datentyps, sondern eine Anforderung des
Wertetyps der Daten.** Für Messwerte, Quoten, Prozentsätze ist binäre
Gleitkommadarstellung angemessen und der Fehler irrelevant. Für **dezimal
definierte Größen mit Exaktheitsanforderung** — Geld — ist jede binäre
Darstellung per Definition falsch, egal wie präzise.

## §1 Option: `double` überall behalten (Status quo)

| Pro | Contra |
|---|---|
| Schnellste Option: Hardware-Arithmetik, kompakte Arrays (`double[]`-Segmente), keine Allokation | Geld ist nicht exakt darstellbar; Abstimmung mit Quellsystem („auf den Cent") prinzipiell unmöglich (P7) |
| Branchenüblich: MSAS rechnet in float/double bzw. Currency; Mondrian immer schon double | Summations-Drift O(n) (P8) |
| Kein Migrationsaufwand | `DecimalType` des eigenen Typsystems wird stillschweigend ignoriert — der Nutzer *kann* Exaktheit nicht einmal anfordern |
| | Verengungskette ist heute inkonsistent (OBJECT-Pfad erhält BigDecimal, alles andere nicht) |

**Bewertung:** Als *Default* richtig, als *einzige* Lane unzureichend.

## §2 Option: Immer `BigDecimal` („alles exakt")

> **Update 2026-07-11:** Diese Option wurde auf User-Wunsch vertieft
> evaluiert — mit JMH-Messungen auf dieser Codebasis und vollständigem
> Blast-Radius-Inventar. Ergebnis (Ablehnung bestätigt, Zahlen ersetzen
> die Behauptungen unten): [Doc 10](10-evaluation-komplettumstellung-bigdecimal.md).

| Pro | Contra |
|---|---|
| Exakte Dezimalarithmetik für Addition/Subtraktion/Multiplikation; keine Darstellungsfehler | **1–2 Größenordnungen langsamer** als double-Hardware-Arithmetik; Allokation pro Operation (BigDecimal ist immutable) — in einem aggregationsdominierten System (Milliarden Additionen) prohibitiv |
| Kollisionsfrage entfällt nebenbei (kein double-Sentinel mehr möglich) | Speicher: Segment-Arrays würden `Object[]` mit ~40+ Bytes/Wert statt 8 Bytes — Cache-Größe und GC-Druck vervielfacht |
| | Division bleibt inexakt (1/3): erfordert explizite `MathContext`-/Rundungspolitik; `BigDecimal.divide` ohne MathContext **wirft** bei nicht-terminierenden Ergebnissen |
| | Kein Infinity/NaN: Die MSAS-kompatible Semantik „Division durch 0 → Infinity" (DivideCalc.java:50) ist nicht darstellbar |
| | Irrationale Funktionen (sqrt, exp, log, trig — die ganzen Excel-/VBA-Calcs) existieren nicht in BigDecimal → ohnehin Umweg über double |
| | Blast-Radius maximal: 155 DoubleCalc-Dateien, alle Operatoren, alle Aggregatoren, Storage, Format |
| | Auch exakt ≠ „richtig": AVG dreier Drittel rundet auch in BigDecimal |

**Bewertung: verworfen.** „Immer BigDecimal" beantwortet die Nutzerfrage mit
**Nein** — es kauft Exaktheit dort, wo sie niemand braucht, zum Preis der
Konkurrenzfähigkeit, und liefert sie bei Division trotzdem nicht ohne
Politik-Entscheidungen.

## §3 Option: Typgesteuerte Dual-Lane (double default, Decimal opt-in) ✔

Der Compiler wählt anhand des **deklarierten Typs** (Measure-`Datatype`
bzw. `DecimalType` des Ausdrucks) die Lane:

- **Double-Lane (Default):** wie heute, für alle `NumericType`-Ausdrücke.
- **Decimal-Lane (opt-in):** neues `DecimalCalc extends Calc<BigDecimal>`;
  gewählt, wenn der Typ `DecimalType` ist. `BigDecimal` **end-to-end ohne
  Verengung**: JDBC-Intake (`getBigDecimal()` ohne `.doubleValue()`) →
  Segment (Object-Dataset, später dediziertes Dataset) → Aggregator
  (BigDecimal-Akkumulator, exakt ohne Kahan) → Vergleich
  (`compareTo` statt `doubleValue()`) → Format (der `Format.java:525`-Zweig
  existiert; `BasicFormat:76`-Verengung fixen). Literale sind bereits
  `BigDecimal` — nur der Kompilierschritt (`NumericLiteralImpl:79`) ändert
  sich in der Decimal-Lane.

| Pro | Contra |
|---|---|
| Bezahlt wird nur, wo Exaktheit angefordert ist; Default-Lane bleibt unberührt (Regressionssicherheit) | Zwei Lanes = zwei Codepfade für Operatoren/Aggregatoren; Konverter Decimal↔Double an Lane-Grenzen nötig (explizit, mit dokumentierter Rundung) |
| Nutzt das **vorhandene** Typsystem — `DecimalType` (Präzision+Skala) existiert seit jeher und wartet auf genau diese Aufgabe | Mixed-Type-Ausdrücke (`decimalMeasure / doubleMeasure`) brauchen Promotionsregeln (Vorschlag: in die Double-Lane degradieren, dokumentiert) |
| Infrastruktur teilweise vorhanden: OBJECT-Pfad erhält BigDecimal (SegmentLoader:701), Aggregator-API gibt `Object` zurück (keine Signaturänderung), Format hat BigDecimal-Zweig | Divisions-/Rundungspolitik muss definiert werden (§5) |
| Architektonisch gleiche Idee wie MSAS: dort ist Currency (Fixed-Point) ein *eigener* Typ neben float/double | |

**Bewertung: empfohlen.** Detailbegründung und Lane-Auswahlregeln in Doc 05.

## §4 Option: Long-scaled Fixed-Point (Cent-Arithmetik)

Geldbeträge als `long` in kleinster Einheit (Cent, Zehntel-Cent):
exakt, primitiv-schnell, 8 Bytes.

| Pro | Contra |
|---|---|
| Exakt UND hardware-schnell; MSAS-Currency ist genau das (64-bit, Skala 10⁻⁴) | Skala ist typfremd: verschiedene Measures mit verschiedenen Skalen (2, 4, 6 Nachkommastellen) müssen beim Rechnen normalisiert werden |
| Kompakte Segmente (`long[]` + BitSet, Muster existiert) | Overflow: 9,2·10¹⁸ absolut klingt viel, aber Skala 10⁻⁴ + SUM über Milliarden Rows nähert sich dem Limit; Overflow-Erkennung nötig (`Math.addExact`) |
| | Multiplikation/Division brauchen Skalen-Buchführung (de facto eine eigene Mini-BigDecimal-Implementierung) |
| | Zweite exakte Repräsentation neben BigDecimal → doppelte Politik, doppelte Tests |

**Bewertung: für v1 verworfen, als Optimierung der Decimal-Lane
vorgemerkt** (Anhang, Doc 08): Wenn die Decimal-Lane sich als heiß erweist,
kann ihr *Storage/Akkumulator* auf skaliertes `long` wechseln, ohne die
API (`DecimalCalc<BigDecimal>`) zu ändern.

## §5 Querschnittsfragen der Decimal-Lane

**Rundungspolitik:** `MathContext.DECIMAL128` (34 signifikante Stellen,
HALF_EVEN) als konfigurierbarer Default für Division und irrationale
Zwischenschritte. IEEE-754-2008-dezimal-kompatibel, mehr als jede
DB-DECIMAL-Präzision.

**Division durch 0 / NULL:** MSAS liefert Infinity; `BigDecimal` kennt kein
Infinity. Zu entscheiden (Empfehlung Doc 05): bei Division durch 0 in der
Decimal-Lane in die Double-Lane degradieren (→ ±Infinity, MSAS-kompatibel);
Alternative — `ErrorValue`-Zelle — dokumentieren und per Property wählbar
machen.

**Irrationale Funktionen** (sqrt, exp, …): in der Decimal-Lane via
double-Umweg mit anschließender Re-Skalierung, dokumentiert als inhärent
gerundet. (Wer `Sin(betrag)` rechnet, hat keine Exaktheitserwartung.)

## §6 Orthogonal und immer sinnvoll: Kahan/Neumaier-Summation

Unabhängig von jeder Lane-Entscheidung: kompensierte Summation in
`FunUtil.sumDouble`/`avg`/`var` und `SumAggregator` reduziert den
Summations-Drift der double-Lane von O(n) auf O(1) — für ~2 zusätzliche
Additionen pro Element, ohne API-Änderung, ohne Semantikänderung (Ergebnisse
ändern sich nur in den letzten Bits, *hin zum mathematisch richtigen Wert*).
**Der größte Genauigkeitsgewinn pro Aufwandseinheit im gesamten Vorhaben.**

Einziges Risiko: Bestandstests mit exakten double-Erwartungswerten müssen
auf Toleranz-Assertions umgestellt werden (Doc 07).

## §7 Antwort auf die Nutzerfrage

> Reicht double, oder sollte man immer mit BigDecimal rechnen?

**`double` reicht als Default — „immer BigDecimal" wäre falsch.** Der beste
Weg ist nicht die Wahl *eines* Typs, sondern die **typgesteuerte
Zuordnung**: binäre Gleitkommazahlen für Mess-/Verhältnisgrößen (schnell,
branchenüblich, Fehler irrelevant), exakte Dezimalarithmetik für dezimal
definierte Größen (Geld) — angefordert über das bereits existierende
`DecimalType`, transportiert über eine eigene Calc-Lane ohne
`doubleValue()`-Verengung, plus Kahan-Summation als kostenloser
Genauigkeitsgewinn für die double-Lane. Genau darauf ist die Engine
vorbereitet, sie setzt es nur nicht um: Literale sind schon `BigDecimal`,
`DecimalType` existiert, der OBJECT-Ladepfad erhält Präzision, die
Aggregator-API ist `Object`-typisiert.

→ Gesamtempfehlung: [Doc 05](05-empfehlung-zielarchitektur.md)
