# 05 — Empfehlung: Die Zielarchitektur

> Der begründete „beste Weg". Er kombiniert die Gewinner aus Doc 03 und
> Doc 04 zu einer Hybrid-Architektur: **drei Schichten, drei
> Repräsentationen — plus ein typgesteuertes Lane-Modell für Präzision.**

## Das Prinzip: Repräsentation folgt Schicht

Die Analyse (Doc 01/02) zeigt, dass die drei Schichten der Engine
unterschiedliche Anforderungen haben — und dass die Fehler des Status quo
genau daraus entstehen, dass *eine* Repräsentation (In-Band-double-Sentinel)
allen dreien übergestülpt wurde:

```
┌────────────────────────────────────────────────────────────────────┐
│ Calc-Schicht (Ausdrücke, Operatoren, Aggregatfunktionen)           │
│   NULL = Java-null   (Calc<Double> ist schon boxed)                │
│   Semantik zentral in NullSemantics                                │
├────────────────────────────────────────────────────────────────────┤
│ Cell-/Cache-Grenze (CellReader, RolapResult, Cell-API)             │
│   Zustand = sealed CellValue:                                      │
│     NullValue | NotLoaded | ErrorValue(Throwable) | ObjectValue(v) │
│   exhaustiver Pattern-Switch statt Identitätsvergleiche            │
├────────────────────────────────────────────────────────────────────┤
│ Segment-Storage (agg-Datasets)                                     │
│   unverändert: double[]/int[] + Null-Indikator-BitSet              │
│   (das Muster ist dort bereits richtig)                            │
└────────────────────────────────────────────────────────────────────┘
```

### 1. Calc-Grenze: Java-`null` statt Sentinel

**Entscheidung:** `DoubleCalc` (und Geschwister) geben Java-`null` für
MDX-NULL zurück. `DOUBLE_NULL`, `EmptyValue` und `BOOLEAN_NULL` entfallen
(Übergangs-Deprecation, Doc 06 Phase 3).

**Begründung:**

- **Kollisionsfreiheit per Konstruktion** — beseitigt P1/P2 vollständig
  statt sie unwahrscheinlicher zu machen.
- **Kostenlos:** Das Interface ist bereits `Calc<Double>`; ein
  `null`-Rückgabewert allokiert nichts. Der Sentinel hat keinerlei
  Performance-Rechtfertigung (Doc 03, Vorbemerkung).
- **Minimal-invasiv:** `BaseExpressionCompiler.java:382`
  (`case null -> DOUBLE_NULL`) ist der einzige zentrale
  Normalisierungspunkt; dazu drei Konverter
  (`UnknownToDoubleCalc`, `IntegerToDoubleCalc`, `CurrentValueDoubleCalc`).
  Viele Calcs prüfen heute schon `== DOUBLE_NULL || == null` — die Schicht
  ist de facto null-tolerant, die Migration invertiert nur die
  Normalisierungsrichtung.
- **Vereinheitlichung:** `RolapEvaluator.evaluateCurrent` (:829) übersetzt
  für die Objekt-Welt heute schon Sentinel → Java-`null`. Danach gilt eine
  einzige Konvention überall.

**Flankierung — `NullSemantics`:** Damit `null`-Checks nicht wieder über 44
Stellen streuen (P6), wird die MDX-NULL-*Semantik* in einer zentralen
Helferklasse gebündelt (Paket `fun` oder `calc.base`):

```java
public final class NullSemantics {
    public static boolean isNull(Double v)            { return v == null; }
    public static Double  plus(Double a, Double b)    { … }  // null+x=x, null+null=null (MSAS)
    public static Double  divide(Double a, Double b)  { … }  // null-Nenner → +Infinity (MSAS)
    public static int     compare(Double a, Double b) { … }  // −∞ < NULL < Werte < NaN < +∞
}
```

Das beseitigt zugleich das FunUtil/Sorter-Komparator-Duplikat und die vier
inkonsistenten Prüfidiome.

### 2. Cell-/Cache-Grenze: sealed `CellValue`

**Entscheidung:** `CellReader.get`, `SegmentWithData.getCellValue`,
`RolapStar.getCellFromCache` und die `CellInfo`-Speicherung arbeiten mit

```java
sealed interface CellValue permits NullValue, NotLoaded, ErrorValue, ObjectValue { }
```

`NullValue`/`NotLoaded` als allokationsfreie Record-Singletons;
`ErrorValue(Throwable)` ersetzt Throwable-als-Wert; `ObjectValue(Object)`
trägt das ohnehin geboxte Wertobjekt. Bewusst **kein**
`NumericValue(double)`-Record (Allokation pro Zelle im Hot Path; erst mit
Valhalla-value-classes attraktiv).

**Begründung:**

- Genau hier multiplizieren sich die Zustände; heute sind sie auf drei
  Ad-hoc-Kodierungen verteilt (Java-`null`, drei Double-Sentinels,
  Throwable-Instanz), unterschieden per Identitätsvergleich.
- Der exhaustive `switch` macht das Protokoll **compilergeprüft**: Ein
  vergessener `NotLoaded`-Fall ist ein Kompilierfehler, keine stille
  Datenkorruption.
- **Die Double(0)-Lüge stirbt** (P3): `NotLoaded` ist nicht
  arithmetikfähig — versehentliche Verwendung schlägt sofort fehl
  (fail fast) statt still `0` zu verrechnen. Die bewährte
  Batching-Architektur (Phase-Schleife, Dirty-Flag) bleibt unverändert;
  ersetzt wird nur der Marker-Typ an seinen **zwei** Nutzungsstellen
  (FastBatchingCellReader:192, RolapResult:1304).
- Gleiches Muster heilt P4 (`SumAggregator`-`Double.MIN_VALUE`-Marker).

### 3. Storage: unverändert

`DenseDoubleSegmentDataset` (double[] + Null-BitSet) ist bereits die richtige
Konstruktion — primitiv-kompakt, NULL out-of-band. Sie bleibt und dient als
Vorbild; einzige spätere Ergänzung ist ein BigDecimal-Dataset für die
Decimal-Lane (Doc 06 Phase 6).

### 4. Typen: compile-time, aber erzwungen

**Entscheidung:** Kein Runtime-Type-Tagging pro Zelle. Das vorhandene
Typsystem bleibt Compile-/Validierungszeit-Instrument — aber der Compiler
**setzt es durch**: Die Lane-Wahl (double vs. decimal) erfolgt anhand von
`DecimalType` bzw. Measure-`Datatype`; Konverter zwischen den Lanes sind
explizit und dokumentiert gerundet.

**Begründung:** Typinformation pro Zelle zur Laufzeit mitzuführen kostet
Speicher und Zeit in der heißesten Schleife des Systems und dupliziert, was
der Compiler statisch weiß. Die Lücke des Status quo ist nicht *fehlendes*
Runtime-Tagging, sondern dass die *vorhandene* statische Information beim
Kompilieren weggeworfen wird (`DecimalType` → `ConstantDoubleCalc`).
Laufzeit-Typinformation fällt an der Cell-Grenze ohnehin ab (Klasse des in
`ObjectValue` getragenen Objekts).

**Antwort auf die Leitfrage „Datentypen in Kalkulationen validieren und
transportieren?":** Validieren ja — an den Compiler-Grenzen (dort sitzen
schon heute `requiresType`-Assertions). Transportieren nein — nicht als
Tag pro Wert; der Typ bestimmt stattdessen *statisch*, welche Calc-Lane
überhaupt instanziiert wird.

### 5. Präzision: Dual-Lane + Kahan

**Entscheidung** (Herleitung Doc 04):

- Default-Lane bleibt `double`, bekommt **Kahan/Neumaier-Summation** in
  `FunUtil` und `SumAggregator`.
- Neue opt-in **Decimal-Lane**: `DecimalCalc extends Calc<BigDecimal>` +
  `compileDecimal` im `ExpressionCompiler`, gewählt bei **`DecimalType` mit
  Skala > 0** — nicht bei `instanceof DecimalType`, denn
  `DecimalType(MAX_VALUE, 0)` ist heute das allgegenwärtige Integer-Idiom
  (Doc 09 §3, Regel 1). Voraussetzung sind zwei neue Typ-Zuführungen
  (Doc 09 §3, Regel 2): Schema-Measure-`Datatype` → `DecimalType(p,s)` und
  Literal → `DecimalType` statt pauschal `NumericType`.
  `BigDecimal` end-to-end: Intake ohne `doubleValue()`
  (SqlStatement:478, SegmentLoader:667/746), BigDecimal-Aggregatoren,
  `compareTo`-Vergleich, Format ohne `BasicFormat:76`-Verengung. Literale
  (bereits `BigDecimal`) kompilieren in der Decimal-Lane verlustfrei.
- Politik: `MathContext.DECIMAL128` (konfigurierbar); Division durch 0 in
  der Decimal-Lane degradiert in die Double-Lane → ±Infinity
  (MSAS-kompatibel); Alternative `ErrorValue` als wählbare Property.
- Mixed-Type-Ausdrücke degradieren in die Double-Lane (dokumentiert).

**Begründung:** Exaktheit ist eine Eigenschaft der *Daten* (Geld), nicht der
Engine. Die Dual-Lane liefert sie genau dort, wo `DecimalType` sie
deklariert, ohne die aggregationsdominierte Default-Last zu verlangsamen —
und die Engine ist dafür sichtbar vorbereitet (BigDecimal-Literale,
Object-Aggregator-API, PDI-16761-Pfad, Format-Zweig).

## Verworfene Alternativen (Kurzfassung, Details Doc 03/04)

| Alternative | Verworfen weil |
|---|---|
| Sentinel behalten | stille Wert-Kollision; `BOOLEAN_NULL==false`; im eigenen Code als Provisorium markiert |
| `Optional<Double>` | Doppel-Boxing + Allokation pro Aufruf; kein Pattern-Matching; trägt nur einen Zustand |
| Primitiv + `wasNull()`-Kanal in der Calc-API | setzt primitive Calc-Signaturen voraus (anderes Großprojekt); Zustands-/Threading-Komplexität; kein Gewinn solange Interface boxed |
| `CellValue` überall (auch intra-calc) | Allokation pro Zwischenwert bis Valhalla; 155-Dateien-Big-Bang |
| Exception für „nicht geladen" | zerstört das Batch-Sammeln — der Grund, warum die Lüge überhaupt erfunden wurde |
| Überall BigDecimal | 1–2 Größenordnungen langsamer, Speicher vervielfacht, kein Infinity, Division braucht trotzdem Rundungspolitik |
| Long-scaled Fixed-Point (v1) | Skalen-Heterogenität, Overflow-Buchführung; als spätere Optimierung des Decimal-Lane-Storage vorgemerkt (Doc 08) |
| Runtime-Type-Tagging pro Zelle | Kosten in der heißesten Schleife; dupliziert statisches Wissen des Compilers |

## Warum dieser Weg der beste ist — die Kernargumente

1. **Er behebt alle drei Krankheitsbilder aus Doc 02** (In-Band-Signalisierung,
   Streuung, Typ-Ignoranz) an der jeweils richtigen Stelle, statt eine
   Universallösung über alle Schichten zu zwingen.
2. **Er arbeitet mit dem Bestand, nicht gegen ihn:** geboxte Calc-API,
   `case null`-Pattern-Matching, Records, `DecimalType`, BitSet-Datasets,
   BigDecimal-Literale — alles ist schon da; die Architektur wird
   vervollständigt, nicht ersetzt.
3. **Er ist inkrementell und jederzeit abbrechbar:** Jede Phase (Doc 06) ist
   unabhängig shipbar; nach jeder Phase ist das System konsistent und besser
   als davor.
4. **Er bewahrt die Semantik-Kompatibilität** (MSAS-NULL-Ordnung,
   NULL-Arithmetik, Infinity-Division) und macht sie erstmals an einer
   Stelle explizit und testbar (`NullSemantics`, Charakterisierungstests).
5. **Performance-Risiko ≈ 0 im Default-Pfad:** Java-`null` statt Sentinel
   ist allokationsneutral; `CellValue` liegt außerhalb des arithmetischen
   Hot Path; Kahan kostet ~2 Additionen; die Decimal-Lane zahlt nur, wer
   sie bestellt.

→ Umsetzung: [Doc 06 — Migrationsplan](06-migrationsplan-phasen.md)
