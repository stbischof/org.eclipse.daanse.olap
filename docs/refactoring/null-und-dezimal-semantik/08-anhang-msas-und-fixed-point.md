# 08 — Anhang: MSAS-Kompatibilität, Fixed-Point, Valhalla

## A. MSAS-Semantikvergleich (Referenz für Charakterisierungstests)

Microsoft Analysis Services ist die De-facto-Referenz für MDX-Semantik.
Die Engine strebt Kompatibilität an; die Tabelle hält fest, was heute wie
implementiert ist und was erhalten bleiben muss:

| Verhalten | MSAS | Daanse heute | Beleg | Nach Umbau |
|---|---|---|---|---|
| `NULL + 1` | `1` (NULL wirkt als 0 in `+`/`−`) | `1` | PlusCalc.java:33–43 (ein NULL-Operand → anderer Operand) | unverändert (`NullSemantics.plus`) |
| `NULL * x` | NULL | NULL | MultiplyCalc.java:35–36 | unverändert |
| `x / NULL` | ±Infinity (Default) | `+Infinity`, per Flag `nullDenominatorProducesNull` auch NULL | DivideCalc.java:45–62 („consistent with MSAS") | unverändert, beide Zweige |
| `x / 0` | ±Infinity | IEEE `Infinity`/`NaN` (kein Sonderfall) | DivideCalc.java:61 | Double-Lane unverändert; Decimal-Lane degradiert zu double → ±Infinity (Doc 04 §5) |
| Sortierordnung | NULL < alle Werte | −∞ < NULL < Werte < NaN < +∞ | FunUtil.java:404–446, Sorter.java:560–603 | unverändert, zentralisiert |
| `Rank` mit NULL | NULL rankt hinter allen Werten | `values.length + 1` | Rank3MemberCalc.java:80 | unverändert |
| `Sum({})` | NULL | NULL (`nullValue`) | FunUtil.java:803/814 | unverändert (`null`) |
| `Avg` über NULLs | NULLs ausgeschlossen | ausgeschlossen (nullCount) | FunUtil.java:899 | unverändert; Paar-Statistik-Fix nur für covariance (P5) |
| `IsEmpty(NULL-Zelle)` | true | true | RolapEvaluator.currentIsEmpty:333 | unverändert |
| Boolesche NULL | dreiwertige Logik | **nicht abbildbar** (`BOOLEAN_NULL == false`) | FunUtil.java:125 | verbesserbar: `Calc<Boolean>` mit `null` ermöglicht dreiwertige Logik erstmals (bewusste, dokumentierte Abweichung vom heutigen Ist — nicht vom MSAS-Soll) |
| Zahlentyp | float/double; **Currency** = 64-bit Fixed-Point, Skala 10⁻⁴ | nur double | Doc 01 §6 | double-Lane + Decimal-Lane (Analogon zu Currency) |

Erkenntnis am Rande: Dass MSAS „Currency" als *eigenen Typ* neben
float/double führt, ist das historische Vorbild des hier empfohlenen
Lane-Modells — exakte Dezimalgrößen sind auch dort nicht in die
Gleitkomma-Arithmetik gezwängt.

## B. Long-scaled Fixed-Point als spätere Storage-Optimierung der Decimal-Lane

Die Decimal-Lane (Doc 05 §5) definiert ihre **API** als
`DecimalCalc extends Calc<BigDecimal>`. Die **Repräsentation** darunter ist
austauschbar. Sollte die Lane in der Praxis heiß werden (große
Decimal-Cubes), ist folgender Wechsel vorbereitet, ohne API-Bruch:

- Segment-Storage: `long[]`-Dataset + Skala im Segment-Header + Null-BitSet
  (Muster `DenseNativeSegmentDataset` existiert; 8 Bytes/Wert statt
  BigDecimal-Objekt).
- Akkumulator: `long` mit `Math.addExact` (Overflow → Eskalation auf
  BigDecimal, dem „BigDecimal-Spill").
- Grenzen, die den v1-Ausschluss begründeten (Doc 04 §4), bleiben real:
  Skalen-Normalisierung bei Multiplikation/Division, Overflow-Buchführung —
  deshalb erst bei nachgewiesenem Bedarf (Benchmark der Phase 5b).

Faustregeln für die Abwägung dann: `DECIMAL(p,s)` mit p ≤ 18 passt
verlustfrei in skaliertes `long`; SUM-Kapazität ≈ 9,2·10¹⁸ / 10^s / max|v|
Zeilen; bei Skala 4 und Beträgen bis 10⁹ sind das ~9·10⁵ Milliarden Zeilen —
praktisch ausreichend, aber `addExact` bleibt Pflicht.

## C. Valhalla-Ausblick (Project Valhalla, Value Classes)

Die Empfehlung ist so geschnitten, dass sie von Valhalla *profitiert*, ohne
darauf zu warten:

- Die `CellValue`-Records (`NullValue`, `NotLoaded`, `ErrorValue`,
  `ObjectValue`) sind kandidatenrein für `value record`: keine Identität
  nötig (Vergleiche laufen über Pattern-Matching statt `==`), unveränderlich,
  klein. Mit Valhalla verschwindet die `ObjectValue`-Wrapper-Allokation
  (Flattening/Scalarization).
- Erst dann wird auch ein `NumericValue(double)`-Record im Hot Path
  attraktiv (heute bewusst ausgelassen, Doc 05 §2) — als flacher
  Nullable-Double-Ersatz (`double + Null-Flag` in einem 9-Byte-Value).
- Ebenso könnte `Calc<Double>` mittelfristig von spezialisierten Generics
  (`Calc<double!>` in Valhalla-Notation) profitieren — das wäre die saubere
  Version der in Doc 03/Option c verworfenen „primitiven Lane", dann ohne
  Seitenkanal, weil der Null-Zustand im Value-Typ selbst kodiert ist.
- Vorbereitung heute: Vergleiche der Singletons nie über `==` schreiben
  (Pattern-Matching/`instanceof`), keine Identitäts-Hashes, keine
  Synchronisation auf den Records — alles ohnehin guter Stil.

## D. Begriffsglossar

| Begriff | Bedeutung in diesen Docs |
|---|---|
| Sentinel | Reservierter Wert innerhalb des Nutzdaten-Wertebereichs, der einen Sonderzustand kodiert (z. B. `0.000000012345` = NULL) |
| In-Band-Signalisierung | Zustandsübermittlung im selben Kanal wie Nutzdaten (Gegenteil: out-of-band, z. B. Null-BitSet) |
| Die „Lüge" | Rückgabe von `valueNotReadyException` (`Double(0)`) für noch nicht geladene Zellen während der Collect-Phase |
| Lane | Vom Compiler gewählter, typspezifischer Berechnungspfad (double-Lane, Decimal-Lane) |
| Collect-Phase | Query-Durchlauf, der Cache-Misses als Batch-Requests sammelt; Ergebnisse werden verworfen und nach dem Laden neu berechnet (RolapResult.phase-Schleife) |
| Charakterisierungstest | Test, der bestehendes Verhalten (auch fehlerhaftes) festschreibt, um Umbauten abzusichern |
| Kahan/Neumaier | Kompensierte Summation: führt den Rundungsfehler in einer Korrekturvariablen mit; Fehler O(1) statt O(n) |
