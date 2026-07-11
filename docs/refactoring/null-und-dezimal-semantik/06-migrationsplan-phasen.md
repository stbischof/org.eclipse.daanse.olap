# 06 — Migrationsplan: Phasen 0–6

> Jede Phase ist **unabhängig shipbar** und hinterlässt einen konsistenten,
> gegenüber dem Vorzustand verbesserten Stand. Abbruch nach jeder Phase ist
> ein gültiger Endzustand. Reihenfolge-Prinzip: erst Sicherheitsnetz, dann
> mechanische Vorbereitung, dann die kleinen scharfen Schnitte, dann Fläche.

## Übersicht

| Phase | Inhalt | Repo | Risiko | Größe |
|---|---|---|---|---|
| 0 | Charakterisierungstests | beide | sehr gering | S |
| 1 | Null-Checks zentralisieren (`NullSemantics`) | beide | gering (mechanisch) | M |
| 2 | Ladepfad-Lüge ersetzen (`NotLoaded`) | rolap | mittel, eng begrenzt | S |
| 3 | Calc-Grenze: Sentinel → Java-`null` | olap | mittel–hoch (Fläche), mechanisch vorbereitet | L |
| 4 | `CellValue` an Cell-/Cache-Grenze | rolap (+api) | mittel–hoch | M |
| 5a | Kahan-Summation (double-Lane) | beide | gering | S |
| 5b | Decimal-Lane (opt-in exakt) | beide | mittel (Default unberührt) | L |
| 6 | Politur: BigDecimal-Dataset, Sentinel-Entfernung, Doku | beide | gering | M |

Abhängigkeiten: 0 → alle; 1 → 3; 2 und 3 unabhängig voneinander; 4 baut auf
2+3 auf; 5a jederzeit; 5b nach 3 (nutzt `NullSemantics` und saubere
Null-Konvention); 6 zuletzt.

---

## Phase 0 — Charakterisierungstests (Sicherheitsnetz)

Verhaltenskompatibilität ist das dominante Risiko des Vorhabens. Bevor
irgendetwas umgebaut wird, wird das *heutige* Verhalten festgeschrieben:

- NULL-Ordnung beim Sortieren (−∞ < NULL < Werte < NaN < +∞), `Order`,
  `TopCount`, `Rank` (NULL rankt zuletzt: Rank3MemberCalc:80).
- NULL-Arithmetik: `null + 1 = 1`, `null * x = null`, `x / null = +Infinity`
  (`nullDenominatorProducesNull`-Flag beide Zweige), `IsEmpty`,
  `CoalesceEmpty`.
- Aggregate leerer/teil-leerer Mengen: `Sum({}) = NULL`, `Avg` ignoriert
  NULLs, `Min/Max` leerer Menge = NULL, Fehler → NaN.
- **Kollisions-Demo:** ein Test, der eine Measure mit dem Wert
  `0.000000012345` lädt und dokumentiert, dass sie heute als NULL erscheint
  (erwartet: rot nach Phase 3 → wird zum Regressionstest der Heilung).
- Ladepfad: Batch-Verhalten (Cache-Miss → zweite Phase), `isDirty`,
  virtuelle Cubes, `currentIsEmpty`.
- XMLA-Serialisierung von NULL-Zellen, Fehlerzellen (`#ERR:`) und
  formatierter Ausgabe.
- Präzisions-Ist: `DECIMAL(19,4)`-Roundtrip dokumentiert heutigen Verlust
  (wird in 5b grün).

Verifikation: Tests laufen grün gegen `main`; sie sind das Regressionsnetz
aller Folgephasen.

## Phase 1 — Null-Checks zentralisieren (mechanisch)

Einführung `NullSemantics` (Doc 05 §1) und Umstellung aller Prüf-/
Vergleichsstellen auf die Helfer — **ohne Verhaltensänderung** (die Helfer
implementieren zunächst exakt den Sentinel-Check):

- 44 entboxte `== DOUBLE_NULL`-Stellen (Operatoren, excel/*, vba/*,
  Komparatoren, `DoubleToBooleanCalc`) → `NullSemantics.isNull(v)`.
- ~157 `Util.isNull`/`== nullValue`-Stellen → einheitlicher Helfer.
- FunUtil/Sorter-Komparator-Duplikat (`compareValues`) zusammenführen;
  `Sorter` delegiert.
- `Objects.equals(o, DOUBLE_NULL)` (UnknownToDoubleCalc:38) und
  `DOUBLE_NULL.equals(number)` (ExpCalc:31) auf Identitäts-Helfer
  normalisieren (bereits eine kleine Härtung).
- Analog `EmptyValue`-/`BOOLEAN_NULL`-Prüfungen kapseln.

Wert der Phase: Danach existiert **ein** Umschaltpunkt für Phase 3, und die
vier Prüfidiome sind Geschichte. Verifikation: Phase-0-Suite, Diff-Review
(rein mechanisch), keinerlei erwartete Verhaltensänderung.

## Phase 2 — Ladepfad-Lüge ersetzen (klein und scharf)

`Util.valueNotReadyException` (`Double(0)`) → nicht-arithmetikfähiges
`NotLoaded`-Singleton (Record; später Teil des sealed `CellValue`):

- `FastBatchingCellReader.java:192` (Erzeugung) und `RolapResult.java:1304`
  (Filterung) — die einzigen zwei Nutzungsstellen.
- Coercion-Punkte (`UnknownToDoubleCalc` u. ä.) reichen den Marker durch
  statt zu casten; da der dirty-Durchlauf ohnehin verworfen wird, genügt
  „nicht crashen". Jeder Crash wäre hier übrigens ein *gefundener Bug* des
  heutigen Systems (Stelle, an der die Lüge in Arithmetik geriet).
- Im selben Zug: `SumAggregator` `Double.MIN_VALUE`-Marker (:56–79) →
  `null`-Initialisierung/expliziter Leerzustand.

Risiko: mittel (Kontrollfluss der Collect-Phase), aber eng begrenzt.
Verifikation: batch-lastige Queries, Cache-Miss-Pfade, virtuelle Cubes,
Phase-0-Suite.

## Phase 3 — Calc-Grenze: Sentinel → Java-`null`

Der Kernschnitt, durch Phase 1 mechanisch vorbereitet:

- `BaseExpressionCompiler.java:382` invertieren: `case null -> null`
  (bzw. Zeile entfällt) statt `-> DOUBLE_NULL`; dito `:361` für
  `BOOLEAN_NULL`.
- Konverter (`UnknownToDoubleCalc`, `IntegerToDoubleCalc`,
  `CurrentValueDoubleCalc`) geben `null` durch statt zu ersetzen — erfüllt
  wortwörtlich deren TODO („0 must be null").
- `NullSemantics`-Helfer intern von Sentinel-Check auf `== null` umstellen
  (der eine Umschaltpunkt).
- `box()`-Methoden (FunUtil:658, Sorter:658) und `sum`-Rückweg (:803)
  entfallen ersatzlos.
- `DOUBLE_NULL`/`nullValue`/`EmptyValue`/`BOOLEAN_NULL` bleiben als
  `@Deprecated`-Aliase bestehen (Alias `nullValue = null` geht nicht —
  deshalb: deprecaten, in Phase 6 löschen), damit externe Konsumenten des
  exportierten `calc*`-Pakets übergangsweise kompilieren.
- `RolapEvaluator.evaluateCurrent` (:829/:855): Konversion Sentinel→null
  wird zur No-op bzw. entfällt.

Risiko: mittel–hoch wegen Fläche (155 Dateien lesen `DoubleCalc`), aber:
zentraler Umschaltpunkt, Dual-Checks existieren, Phase-0-Suite + der
Kollisions-Demo-Test schlagen bei Fehlern an. Besondere Aufmerksamkeit:
Auto-Unboxing-NPEs (`double d = calc.evaluate(e)`) — per Suche nach
Unboxing-Stellen systematisch abräumen. Verifikation: volle
MDX-Funktionstests (insbesondere excel/*, vba/*), Vergleichsläufe alt/neu
auf Referenz-Cubes.

## Phase 4 — `CellValue` an der Cell-/Cache-Grenze

- Sealed `CellValue` einführen (`NullValue | NotLoaded | ErrorValue |
  ObjectValue`); `NotLoaded` aus Phase 2 wandert hinein.
- `CellReader.get`, `SegmentWithData.getCellValue`,
  `RolapStar.getCellFromCache`, `CellInfo.value` auf `CellValue` umstellen;
  Identitätsvergleiche werden exhaustive Switches.
- Throwable-als-Wert → `ErrorValue` (RolapCell.isError:177, format
  `#ERR:`-Pfad RolapEvaluator:1061).
- Öffentliche `Cell`-API: `getValue()` liefert weiterhin Java-`null` für
  NULL-Zellen (Kontrakt-Anpassung im Javadoc: heute „nie null, sondern
  Util.nullValue" — Konsumenten prüfen!), `isNull()`/`isError()` über den
  Zustand.
- XMLA-Mapping prüfen (NULL-Zelle → leeres `<Value/>`-Element etc. muss
  byte-identisch bleiben).

Risiko: mittel–hoch (API-sichtbar, XMLA, Writeback). Verifikation:
XMLA-Kompatibilitätstests, Writeback-Tests (rolap `…/writeback`),
Segment-Cache-SPI (Serialisierungsformat der Segmente ändert sich NICHT —
`CellValue` lebt oberhalb des Storage).

## Phase 5a — Kahan/Neumaier-Summation

`FunUtil.sumDouble` (:806–840), `avg` (:789), `var`/`stdev`-Kette und
`SumAggregator` auf kompensierte Summation. Keine API-Änderung.
Risiko: gering — Ergebnisse ändern sich in den letzten Bits (Richtung
mathematisch korrekt). Verifikation: Toleranz-Assertions statt exakter
double-Vergleiche in betroffenen Tests; Mikro-Benchmark (erwartet < 5 %
auf Aggregations-Hot-Loop); adversarialer Testfall (10⁷ Werte gemischter
Größenordnung, Referenz via BigDecimal).

## Phase 5b — Decimal-Lane (opt-in exakt)

- Typ-Vorarbeit (Doc 09 §6): Integer-Idiom
  `new DecimalType(Integer.MAX_VALUE, 0)` (~15 Stellen) durch expliziten
  Integer-Marker ersetzen; die beiden `getScale() == 0`-Weichen
  (BaseExpressionCompiler:259, TypeUtil:219) nachziehen.
- Typ-Zuführung: Schema-Measure-`Datatype` (DECIMAL/NUMERIC mit p,s) →
  `DecimalType(p,s)` bei der Measure-Typisierung (rolap);
  `NumericLiteralImpl.getType()` → `DecimalType(bd.precision(), bd.scale())`
  statt pauschal `NumericType.INSTANCE`.
- `DecimalCalc extends Calc<BigDecimal>` (api), `compileDecimal` im
  `ExpressionCompiler`/`BaseExpressionCompiler`; Lane-Wahl bei
  `DecimalType` mit Skala > 0 (nach der Vorarbeit: `instanceof DecimalType`).
- Operator-Calcs der Lane (`plus/minus/multiply/divide` in
  BigDecimal, `MathContext.DECIMAL128` konfigurierbar; Division durch 0 →
  Degradation in Double-Lane → ±Infinity; Property für `ErrorValue`-
  Alternative).
- Intake ohne Verengung: `SqlStatement.java:478` und
  `SegmentLoader.java:667/746` erhalten für Decimal-Lane-Spalten
  `BigDecimal` (Weg über OBJECT-Dataset; der PDI-16761-Pfad :701 zeigt die
  Route).
- BigDecimal-Aggregatoren (Sum/Avg/Min/Max; API ist `Object` — keine
  Signaturänderung).
- `FunUtil.evaluateSet:907`-Verengung lane-abhängig; `compareValues`
  (:484) und Sorter (:641) nutzen `compareTo` für `BigDecimal`.
- Literal-Pfad: `NumericLiteralImpl:79` kompiliert in der Decimal-Lane zu
  `ConstantDecimalCalc` (BigDecimal bleibt exakt).
- Format: `BasicFormat.java:76`-Verengung durch BigDecimal-native
  Formatierung ersetzen (Zweig `Format.java:525` existiert).
- Mixed-Type-Promotionsregeln dokumentieren (Degradation in Double-Lane).

Risiko: mittel — neuer Code, aber Default-Lane unberührt; opt-in.
Verifikation: neue Decimal-Testsuite (Geldbeträge, Skalen,
`DECIMAL(19,4)`-Roundtrip aus Phase 0 wird grün, Divisionsrundung,
Cent-genaue Abstimmungssumme gegen Quelldatenbank); Regressionsfreiheit der
Default-Lane (Phase-0-Suite unverändert grün).

## Phase 6 — Politur

- Dediziertes BigDecimal-Segment-Dataset (statt Object-Dataset) für
  Decimal-Measures; Cache-Serialisierungs-Roundtrip-Tests.
- `@Deprecated`-Sentinels (`DOUBLE_NULL`, `nullValue`, `EmptyValue`,
  `BOOLEAN_NULL`, `valueNotReadyException`, `DOUBLE_EMPTY`) endgültig
  entfernen; `Util.isNull` auf `o == null` reduzieren oder inline'n.
- covariance-Fehlausrichtung (P5) beheben: `evaluateSet`-Variante mit
  positionserhaltenden NULLs für Paar-Statistiken.
- IEEE-754-Grenzen und Lane-Modell in Nutzer-Doku (Measure-Modellierung:
  „wann DecimalType wählen").

---

## Querschnittsrisiken (Register in Doc 07)

- **XMLA-Kompatibilität** (Phase 4): NULL-/Fehlerzellen-Serialisierung.
- **Writeback-Modul** (Phasen 3/4): eigene Wertpfade, `ScenarioImpl`
  nutzt `doubleValue()`-Konversionen.
- **Cache-/Segment-Serialisierung** (Phasen 4/6): Storage-Format bleibt
  stabil (BitSet bereits serialisiert); nur oberhalb ändern.
- **Bestandstests mit exakten double-Erwartungen** (Phase 5a).
- **Externe Konsumenten der exportierten Pakete** (`olap.calc*`,
  `olap.function*` laut bnd.bnd): Deprecation-Fenster einhalten.
