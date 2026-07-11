# Initiative: Null- und Dezimal-Semantik in der Calculation-Schicht

> Analyse und Refactoring-Empfehlung für die Behandlung von NULL-Werten,
> Zell-Zuständen und numerischer Präzision in der Daanse-OLAP-Engine
> (Mondrian-Fork), Repos `org.eclipse.daanse.olap` und
> `org.eclipse.daanse.rolap`.

## Warum diese Initiative existiert

Die Engine kodiert den MDX-Wert **NULL** in der numerischen Berechnungsschicht
als magische Gleitkommazahl:

```java
// org.eclipse.daanse.olap.common.Util (Util.java:179, 184)
public static final Double DOUBLE_NULL = Double.valueOf(0.000000012345);
public static final Object nullValue   = DOUBLE_NULL;
```

Das ist kein Randdetail, sondern das Fundament der gesamten Wert-Semantik.
Daraus folgen drei konkrete Probleme:

1. **Wert-Kollision.** Jede legitime Berechnung, die exakt `1.2345e-8` ergibt,
   wird als NULL fehlinterpretiert — ca. 44 Stellen vergleichen entboxte
   `double`-Werte per `==` gegen die Konstante. Eine Stelle
   (`UnknownToDoubleCalc.java:38`) nutzt sogar `Objects.equals`, wodurch nicht
   nur das Singleton, sondern *jedes* wertgleiche `Double` kollidiert.
2. **Die „Lüge" des Ladepfads.** Solange eine Zelle noch nicht aus der
   Datenbank geladen ist, gibt der batchende Cell-Reader
   `Util.valueNotReadyException = Double.valueOf(0)` zurück — eine echte Null
   (Zahl!), die durch reale Arithmetik fließt und deren Ergebnisse später
   per Identitätsvergleich wieder aussortiert werden müssen
   (`FastBatchingCellReader.java:192`, `RolapResult.java:1304`).
3. **Stiller Präzisionsverlust.** Die gesamte Rechenschicht arbeitet in
   `double`. `BigDecimal`-Werte aus der Datenbank (Geldbeträge!) werden an
   mehreren Stellen per `doubleValue()` verengt; MDX-Literale werden zwar als
   `BigDecimal` geparst, aber zu `double` kompiliert. Das vorhandene Typsystem
   (`DecimalType` etc.) wird zur Evaluationszeit gar nicht genutzt.

Gleichzeitig zeigt der Code, dass die saubere Lösung punktuell schon existiert:
Der Segment-Speicher unterscheidet `0` von NULL über ein Null-Indikator-`BitSet`
(`DenseDoubleSegmentDataset`), und der `OBJECT`-Ladepfad erhält `BigDecimal`
bereits verlustfrei (`SegmentLoader.java:701`, Kommentar „PDI-16761 … we lose
precision").

## Leitfragen

Diese Initiative beantwortet fünf Fragen:

| # | Frage |
|---|-------|
| 1 | Wie soll NULL in der Calc-Schicht repräsentiert werden — Sentinel, Java-`null`, `Optional`, sealed Record? |
| 2 | Wie soll „noch nicht geladen" von „ist NULL" unterschieden werden? |
| 3 | Sollen die Datentypen in den Kalkulationen validiert und transportiert werden? |
| 4 | Reicht `double`, oder sollte immer mit `BigDecimal` gerechnet werden — was ist der beste Weg, korrekt mit Kommazahlen zu rechnen? |
| 5 | In welcher Reihenfolge lässt sich das risikoarm umbauen? |

## Umsetzungsstand

> Umsetzung auf Branch `refactor/null-dezimal-semantik` (olap + rolap),
> ein Commit pro Phase. Getroffene Entscheidungen: eigener CSV-/EMF-Testkatalog
> statt School; „erwartet-heute-falsch"-Tests als `@Disabled` mit Verweis.

| Phase | Inhalt | Stand |
|---|---|---|
| 0 | Charakterisierungstests | **fertig** (2026-07-11): olap 115 Calc-Unit-Tests (`common/.../nullsemantics/`) + 5 XMLA-Konverter-Tests (xmla/bridge); rolap 18 MDX-Tests (`testkit/core/.../nullsemantics/`, 2 davon `@Disabled` bis Phase 3/5b); Befunde in Doc 99 |
| 1 | `NullSemantics` zentralisieren | **fertig** (2026-07-11): E1 = `calc.base` (exportiert); 49 olap- + 8 rolap-Stellen umgestellt, `Util.isNull` delegiert, Komparator-Duplikat zusammengeführt; sanktionierte Härtung ExpCalc/UnknownToDoubleCalc; `EmptyValue`/`DOUBLE_EMPTY` deprecated (Löschung Phase 6) |
| 2 | `NotLoaded` statt Double(0) | **fertig** (2026-07-11): `NotLoaded`-Record in `api.result` (Vorgriff auf CellValue); `Util.valueNotReadyException` = deprecated Alias auf die Instanz; Koerzions-Calcs tolerieren Marker explizit; `SumAggregator` ohne MIN_VALUE-Marker; testkit-Suite 2183 Tests grün |
| 3 | Sentinel → Java-`null` | **olap-Seite fertig** (2026-07-11): `NullSemantics.isNull(Double)` = `v == null`, primitive `isNull(double)` gelöscht, `compare(double,double)` ohne NULL-Slot; Compiler-Inversion (`case null -> null`, BOOLEAN bleibt per E4); alle Produzenten liefern `null` (Konverter, Operatoren, excel/vba, FunUtil percentile/quartile/sumDouble → `Double`); `Sorter.box` gelöscht; R1-Sweep: Null-Guards in ~30 Calcs; `Util.DOUBLE_NULL`/`isSentinelOnly` deprecated (Löschung Phase 6); Phase-0-Suite auf Heilungs-Regression geflippt (Doc 07 §1.1). **rolap-Seite fertig**: Cell-Layer tolerant (`isNull` statt `Objects.equals`), Kollisions-Demo aktiviert; Differenzlauf = volle testkit-Suite 2182 grün |
| 4 | `CellValue` Cell-/Cache-Grenze | **fertig** (2026-07-11): sealed `CellValue` (`NullValue`\|`NotLoaded`\|`ErrorValue`\|`ObjectValue`) in `api.result`; Kette Segment→Star→AggManager→CellReader auf `CellValue`, zwei Entpack-Nähte (`RolapEvaluator.evaluateCurrent`, `CellInfo`/`RolapCell`); `isError` = Zustandscheck statt Throwable-instanceof; `Cell.getValue()`-Kontrakt: Java-`null` für NULL (Javadoc korrigiert); XMLA-Fehlerzellen-Referenztest vorab; Storage-Format unverändert. `Util.nullValue` bleibt bis Phase 6 (Storage-Placeholder SegmentLoader + FunUtil.sum-Objektpfad) |
| 5a | Kahan-Summation | **fertig** (2026-07-11): `CompensatedSum` (Neumaier) in `calc.base`; `FunUtil.sumDouble`/`avg`/`var`/`covariance` + rolap `SumAggregator`; Bestandstests bit-identisch grün (keine Toleranz-Umstellung nötig); adversarialer 10⁷-Test gegen BigDecimal-Referenz. Benchmark-Gate: kein JMH im Repo — informelle Einschätzung (Boxed-Unwrap dominiert die Schleife), dokumentierte Abweichung von Doc 07 §1.4 |
| 5b | Decimal-Lane | offen |
| 6 | Politur / Sentinel-Löschung | offen |

## Dokumentenübersicht

| Doc | Inhalt |
|-----|--------|
| [`01-analyse-status-quo.md`](01-analyse-status-quo.md) | Vollständige Bestandsaufnahme: Sentinel-Inventar, Calc-Hierarchie, Typsystem, Ladepfad-Protokoll, Storage, Präzisionspfad, Blast-Radius-Zahlen |
| [`02-problemfaelle.md`](02-problemfaelle.md) | Konkrete Fehlerszenarien und Code-Belege (Kollisionen, die Double(0)-Lüge, Marker-Smells, TODO-Inventar) |
| [`03-optionen-null-repraesentation.md`](03-optionen-null-repraesentation.md) | Optionen für die NULL-Repräsentation und die Not-loaded-Frage, mit Pro/Contra |
| [`04-optionen-dezimal-praezision.md`](04-optionen-dezimal-praezision.md) | double vs. BigDecimal vs. Fixed-Point vs. Kahan — Optionen mit Pro/Contra |
| [`05-empfehlung-zielarchitektur.md`](05-empfehlung-zielarchitektur.md) | Der empfohlene beste Weg (Hybrid-Architektur, Lane-Modell), begründet, mit verworfenen Alternativen |
| [`06-migrationsplan-phasen.md`](06-migrationsplan-phasen.md) | Phasen 0–6: Scope, Risiko, Größe, Abhängigkeiten, Abbruchpunkte |
| [`07-verifikation-und-risiken.md`](07-verifikation-und-risiken.md) | Charakterisierungstests, Risikoregister (XMLA, Writeback, Cache-Serialisierung), Benchmark-Ansatz |
| [`08-anhang-msas-und-fixed-point.md`](08-anhang-msas-und-fixed-point.md) | MSAS-Semantikvergleich, Long-scaled-Fixed-Point-Alternative, Valhalla-Ausblick |
| [`09-typsystem-api-type.md`](09-typsystem-api-type.md) | Das Typ-Paket `api/…/olap/api/type` im Detail: Inventar, Rollen, das DecimalType-Integer-Idiom, nötige Typ-Zuführungen für die Decimal-Lane |
| [`99-handover-arbeitsnotizen.md`](99-handover-arbeitsnotizen.md) | Meta: Handover für Umsetzungs-Sessions — Fundstellen-Inventare, Grep-Anker, offene Recherchelücken, ausstehende Entscheidungen |

## Empfehlung in einem Satz

**Drei Schichten, drei Repräsentationen:** Java-`null` an der (ohnehin
geboxten) Calc-Grenze statt des Sentinels; ein sealed `CellValue`-Typ an der
Cell-/Cache-Grenze, wo sich die Zustände (NULL / nicht geladen / Fehler / Wert)
multiplizieren; unverändert `double[]` + Null-BitSet im Segment-Speicher —
plus eine **typgesteuerte, opt-in exakte Dezimal-Lane** (`BigDecimal`
end-to-end) für `DecimalType`-Measures, während die Default-Lane `double`
bleibt und per Kahan-Summation genauer wird. Begründung in
[Doc 05](05-empfehlung-zielarchitektur.md).

## Non-Goals

- **Keine Code-Änderungen in dieser Initiative** — dies ist eine Analyse- und
  Planungslieferung; die Umsetzung folgt dem Phasenplan in Doc 06.
- Keine Umstellung der Calc-Schicht auf primitive Signaturen (separates,
  größeres Vorhaben; siehe verworfene Option c in Doc 03).
- Keine Änderung der MSAS-/Mondrian-Verhaltenskompatibilität (NULL-Ordnung,
  NULL-Arithmetik, Division-durch-Null-Semantik) — Abweichungen werden nur
  dort vorgeschlagen, wo heute Bugs vorliegen, und explizit markiert.
- Keine Parser-/MDX-Grammatik-Änderungen (Literale sind bereits `BigDecimal`).
