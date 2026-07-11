# 02 — Konkrete Problemfälle

> Jeder Abschnitt: Fehlerszenario, Code-Beleg, Bewertung der praktischen
> Relevanz. Grundlage für die Optionenabwägung in Doc 03/04.

## <a name="kollision"></a>P1: Wert-Kollision des NULL-Sentinels

**Szenario.** Eine Measure oder ein berechneter Ausdruck ergibt exakt
`0.000000012345` (bzw. `-0.000000012345` für EMPTY) — etwa ein Verhältnis,
ein wissenschaftlicher Messwert oder schlicht ein aus der Datenbank geladener
Wert. Die Engine behandelt ihn als NULL: Er verschwindet aus Aggregaten,
sortiert wie −∞, `IsEmpty()` liefert `true`, die Zelle rendert leer.

**Belege.** Die Prüfungen laufen auf *entboxten* `double`-Werten per `==`,
sind also Wert- und nicht Objektvergleiche:

```java
// function/def/operators/plus/PlusCalc.java:33
if (v0 == FunUtil.DOUBLE_NULL || v0 == null) { … }

// fun/sort/Sorter.java:659 — box():
return d == FunUtil.DOUBLE_NULL ? null : d;
```

Insgesamt 44 solcher Stellen (Operatoren, ~11 Excel-Funktionen,
Komparatoren, `DoubleToBooleanCalc.java:32`). Verschärfend:

```java
// calc/base/type/doublex/UnknownToDoubleCalc.java:38
} else if (Objects.equals(o, FunUtil.DOUBLE_NULL)) {
```

Hier kollidiert nicht nur das geteilte Singleton, sondern **jedes**
wertgleiche `Double`-Objekt — auch eines, das frisch aus JDBC oder einer
Berechnung stammt.

**Bewertung.** Wahrscheinlichkeit pro Einzelwert gering (ein Bitmuster von
2⁶⁴), aber: OLAP-Systeme evaluieren Milliarden Zellen, der Wert liegt in
einem plausiblen Bereich (1,2345·10⁻⁸), und der Fehler ist **still und
nicht diagnostizierbar** — es gibt keinen Log, keine Exception, nur ein
falsches Ergebnis. Zusätzlich ist die Semantik *by design* verletzt: Der
Wertebereich von `double` ist für Nutzdaten reserviert und enthält trotzdem
zwei reservierte Engine-Werte.

## P2: `BOOLEAN_NULL == false` — NULL ist von `false` ununterscheidbar

`FunUtil.java:125` definiert `BOOLEAN_NULL = false`. Ein boolescher Ausdruck,
der NULL ergibt (z. B. Vergleich mit einer NULL-Zelle,
`DoubleToBooleanCalc.java:32`), ist vom legitimen Ergebnis `false` nicht
unterscheidbar. Dreiwertige Logik (SQL/MDX: `NULL AND false = false`,
`NULL OR true = true`, sonst NULL) ist damit prinzipiell nicht
implementierbar. Ein auskommentiertes `INTEGER_NULL` (FunUtil.java:120)
zeigt, dass das Sentinel-Muster für Integer bereits aufgegeben wurde.

## P3: Die Double(0)-Lüge des Batch-Readers

**Szenario.** Während der Collect-Phase (Zellen werden gesammelt, um sie
gebatcht per SQL zu laden) liefert `FastBatchingCellReader.get`:

```java
// rolap …/result/FastBatchingCellReader.java:192
return Util.valueNotReadyException;   // == Double.valueOf(0)
```

Dieses `Double(0)` ist **arithmetikfähig**: Es fließt durch `+`, `*`,
Aggregate, Formatierung — produziert also systematisch Müllergebnisse, die
ausschließlich deshalb nicht sichtbar werden, weil `RolapResult.java:1304`
sie per **Identitätsvergleich** (`o != Util.valueNotReadyException`) vor dem
Speichern aussortiert und die Phase-Schleife neu evaluiert.

**Fragilität.** Der Schutz bricht überall dort, wo der Wert *nicht identisch
durchgereicht* wird: Sobald irgendein Calc mit dem Wert *rechnet*
(`0 + x = x`) oder ihn neu boxt, ist die Identität weg und ein falscher
Zwischenwert kann als echtes Ergebnis durchgehen, falls die betroffene Zelle
im selben Durchlauf nicht erneut angefasst wird. Die Korrektheit hängt am
Zusammenspiel „Lüge einspeisen → alles verwerfen → komplett neu rechnen" —
einem Protokoll, das nirgendwo im Typsystem sichtbar ist, sondern nur in
zwei Codezeilen und einem Javadoc-Kommentar lebt („RolapAggregationManager
never lies", RolapAggregationManager.java:961).

Historische Fußnote: Der Name `valueNotReadyException` ist ein Fossil — im
Ur-Mondrian wurde tatsächlich eine Exception geworfen. Das Batching hat die
Exception durch die Lüge ersetzt, den Namen behalten und die Typsicherheit
verloren.

## P4: `Double.MIN_VALUE` als „noch kein Wert"-Marker im SumAggregator

```java
// rolap …/aggregator/SumAggregator.java:70–79
double sumDouble = Double.MIN_VALUE;
…
return sumDouble == Double.MIN_VALUE ? null : sumDouble;
```

Gleiche Krankheit, dritte Ausprägung: `Double.MIN_VALUE` (4.9·10⁻³²⁴) ist
ein legaler double-Wert. Zusätzlich subtil falsch: Startet die Summe bei
`MIN_VALUE` statt 0, ist jede echte Summe um `MIN_VALUE` verfälscht (praktisch
unsichtbar, konzeptionell falsch), und eine Summe, die zufällig exakt
`MIN_VALUE` ergibt, wird zu `null`. Das analoge `Integer.MIN_VALUE`-Muster
(:56–68) kann eine legitime Summe von `Integer.MIN_VALUE` verschlucken.

## P5: NULL-Verwerfen in `evaluateSet` verschiebt Paar-Aggregationen

`FunUtil.evaluateSet` (FunUtil.java:880–913) zählt NULLs nur
(`nullCount++`, :899) und nimmt sie **nicht** in die Werteliste auf. Für
`sum`/`avg` ist das die gewollte MDX-Semantik. Für **Paar-Statistiken**
(`covariance`, `correlation`) zerstört es aber die Positions-Korrespondenz
der beiden Listen: Enthält Reihe X an Position i ein NULL und Reihe Y nicht,
sind alle Folgepaare verschoben. Der Code weiß das selbst:

```java
// FunUtil.java:732–733 (Kommentar an covariance)
// TODO: … evaluateSet drops nulls …
// FunUtil.java:743: ungleiche Listenlängen → return Util.nullValue;
```

Das Symptom wird abgefangen (ungleiche Länge → NULL), der eigentliche Fehler
(gleiche Länge, aber verschobene Paare) nicht.

## P6: Streuung statt Zentralisierung der NULL-Checks

Die NULL-Prüfung ist über ~44 Stellen dupliziert und **inkonsistent**:

- `PlusCalc`, `MinusCalc`, `DivideCalc`, `AsinhCalc` prüfen
  `v == DOUBLE_NULL || v == null` (beide Repräsentationen),
- die meisten `function/def/excel/*`-Calcs prüfen nur `v == DOUBLE_NULL`,
- `ExpCalc.java:31` nutzt `Util.DOUBLE_NULL.equals(number)`,
- `UnknownToDoubleCalc.java:38` nutzt `Objects.equals`.

Vier verschiedene Prüfidiome für dieselbe Frage. Dass etliche Calcs
*zusätzlich* auf Java-`null` prüfen (und
`AbstractProfilingNestedDoubleCalc` ein Debug-Log schreibt, wenn ein echtes
`null` durchsickert), belegt: **Die Schicht ist heute schon de facto
null-tolerant** — der Sentinel bringt keine Sicherheit mehr, nur noch
Streuung. Die MDX-Ordnungssemantik (−∞ < NULL < Werte < NaN < +∞) ist
zwischen `FunUtil.compareValues` (:404–446) und `Sorter` (:560–603)
vollständig dupliziert.

## P7: Präzisionsverlust bei Dezimalwerten (Geld)

**Szenario.** Eine Measure-Spalte ist `DECIMAL(19,4)` (Geldbetrag). Der Wert
`123456789012345.6789` ist in `double` nicht darstellbar
(`double` hat 53 Bit ≈ 15–16 signifikante Dezimalstellen); gespeichert wird
`123456789012345.671875` o. ä. Aufsummiert über Millionen Fact-Rows driftet
das Ergebnis zusätzlich durch Rundungsfehler der naiven Summation.

**Belege der Verengungskette** (Details in Doc 01 §6):

1. Intake: `SqlStatement.java:478`, `SegmentLoader.java:667,746` —
   `getBigDecimal().doubleValue()`; erklärtermaßen ohne Absicht auf mehr
   („no plan to support the DECIMAL/BigDecimal type internally",
   SqlStatement.java:471, :551–552).
2. Literale: `NumericLiteralImpl.java:79` — exakt geparstes `BigDecimal`
   wird zu `double` kompiliert.
3. Aggregation: `FunUtil.evaluateSet:907` und `SumAggregator:76` —
   `((Number) o).doubleValue()`, auch wenn `o` ein via OBJECT-Pfad
   erhaltenes `BigDecimal` ist. Der PDI-16761-Erhalt (SegmentLoader:701)
   ist damit **wirkungslos für Aggregate** — er bewahrt die Präzision nur
   bis zur nächsten Rechenoperation.
4. Vergleich: `FunUtil.compareValues:484` — zwei `BigDecimal`, die sich erst
   jenseits der 16. Stelle unterscheiden, vergleichen als gleich.
5. Format: `BasicFormat.java:76` — `BigDecimal` wird vor dem Formatieren zu
   `double`.

**Bewertung.** Für typische OLAP-Kennzahlen ist `double` ausreichend und
branchenüblich (MSAS rechnet ebenfalls in float/double bzw. Currency).
Für Finanz-Use-Cases mit Abstimmungspflicht (Summe muss auf den Cent mit dem
Quellsystem übereinstimmen) ist der Status quo **nicht korrekt herstellbar**,
selbst wenn der Nutzer alles richtig modelliert (`DecimalType` existiert,
wird aber ignoriert — Doc 01 §3).

## P8: Naive Summation (kein Kahan) in der double-Lane

`FunUtil.sumDouble` (:806–840), `avg` (:789), `var` (:676) und
`SumAggregator` akkumulieren mit `sum += x`. Bei großen Zellzahlen mit
gemischten Größenordnungen wächst der relative Fehler mit O(n) statt O(1)
(Kahan/Neumaier) — vermeidbar für ~2 zusätzliche Additionen pro Element,
ohne API-Änderung.

## P9: TODO-Inventar (der Code kennt seine Schulden)

| Stelle | Kommentar |
|---|---|
| `UnknownToDoubleCalc.java:37`, `IntegerToDoubleCalc.java:35` | `// TODO: !!! JUST REFACTORING 0 must be null` |
| `SqlStatement.java:471, 551–552, 570–571` | DECIMAL nur als Snowflake-Workaround, „nothing seems to call this method anyway" |
| `SegmentLoader.java:701` | `// PDI-16761 if we cast it to double type we lose precision` |
| `FunUtil.java:732–733, 787, 794` | covariance-Fehlausrichtung, avg-NULL-Behandlung nicht parametrisiert |
| `NumericLiteralImpl.java:40–47` | „Using a BigDecimal allows us to store the precise value that the user typed. We will have to fit the value into a native double or int later on…" |

## Zusammenfassung: drei Krankheitsbilder

| # | Krankheitsbild | Probleme | Behandlung (Doc) |
|---|---|---|---|
| A | In-Band-Signalisierung: Zustände (NULL, nicht geladen, kein Wert) als reguläre Werte im Nutzdaten-Wertebereich kodiert | P1, P2, P3, P4 | Doc 03 |
| B | Verstreute, inkonsistente Semantik statt zentraler Definition | P5, P6 | Doc 03 / Phasenplan |
| C | Typinformation wird verworfen statt genutzt → Präzisionsverlust | P7, P8 | Doc 04 |

→ Optionen zur NULL-Repräsentation: [Doc 03](03-optionen-null-repraesentation.md)
