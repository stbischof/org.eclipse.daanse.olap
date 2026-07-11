# 11 — Ergebnis der Initiative & Nutzer-/Entwicklerdoku

> Stand nach Abschluss der Umsetzung (2026-07-11, Branch
> `refactor/null-dezimal-semantik` in olap + rolap). Dieses Dokument
> beschreibt die **geltenden Regeln** nach der Migration — als Referenz für
> Entwickler und Modellierer.

## 1. NULL-Repräsentation (das neue Fundament)

| Schicht | NULL ist … | Regelwerk |
|---|---|---|
| Calc-Schicht (`Calc<Double>` & Co.) | **Java `null`** | `org.eclipse.daanse.olap.calc.base.NullSemantics` |
| Cell-/Cache-Grenze (CellReader → RolapCell) | **`NullValue.INSTANCE`** im sealed `CellValue` (`NullValue \| NotLoaded \| ErrorValue \| ObjectValue`, `api.result`) | exhaustive `switch` |
| Segment-Storage | unverändert `double[]`/`int[]` + Null-Indikator-`BitSet` | — |
| Öffentliche `Cell`-API | `getValue()` liefert Java `null`, `isNull()`/`isError()` sind Zustandschecks | Javadoc `api.result.Cell` |

Der magische Sentinel `0.000000012345` ist aus der Semantik entfernt: Eine
Berechnung oder ein Datenbankwert von exakt `1.2345e-8` ist überall ein
echter Wert (Kollisions-Regressionstests in olap `nullsemantics/` und rolap
testkit `CollisionDemoTest`).

## 2. NULL-Verhaltensregeln (MSAS-kompatibel, testfixiert)

- Arithmetik: `NULL + x = x`, `NULL − x = −x`, `NULL * x = NULL`,
  `NULL / x = NULL`, `x / NULL = +Infinity`
  (Flag `nullDenominatorProducesNull` unverändert).
- Vergleiche mit NULL liefern `false` (`BOOLEAN_NULL`; bewusst keine
  dreiwertige Logik — Entscheidung E4, Doc 99).
- Aggregate: `Sum({}) = NULL`, `Avg`/`Min`/`Max` ignorieren NULL-Zellen,
  Fehlerzellen poisonen zu `NaN`.
- Sortierordnung entboxt: `−∞ < Werte < NaN < +∞` (kein NULL-Slot mehr —
  NULL existiert nur geboxt); geboxt/Objekt-Ebene: Java-`null` (und der
  Legacy-`nullValue`) sortieren unter alles, inkl. `−∞` (historische
  Asymmetrie, bewusst erhalten).
- Paar-Statistiken (`Covariance`, `Correlation`): seit Phase 6 paarweise
  ausgerichtet — ein Tupel zählt nur, wenn *beide* Werte non-NULL sind
  (Heilung P5).

## 3. „Nicht geladen" und Fehler

- Cache-Miss im Batch-Lauf → `NotLoaded.INSTANCE` (nicht arithmetikfähig;
  der Dirty-Durchlauf wird verworfen). Code, der damit rechnet, crasht
  laut — jeder solche Crash ist ein echter Bug (R10, gewollt).
- Evaluationsfehler → `ErrorValue(Throwable)` an der Zelle;
  `Cell.getValue()` liefert das Throwable, Formatierung `#ERR: …`,
  XMLA-Serialisierung unverändert (Referenztests xmla/bridge).

## 4. Präzision

- Die Rechenschicht bleibt **`double`** (IEEE-754, 53-Bit-Mantisse ≈ 15–17
  signifikante Stellen).
- Aggregation nutzt **kompensierte Summation** (Neumaier,
  `calc.base.CompensatedSum`) in `FunUtil.sumDouble`/`avg`/`var`/
  `covariance` und im rolap-`SumAggregator`-Fast-Path: Rundungsfehler
  wachsen nicht mehr mit der Set-Größe.
- **Bekannte, akzeptierte Grenze:** `DECIMAL(p,s)`-Werte jenseits von ~15
  signifikanten Stellen verlieren beim Intake Präzision
  (`SqlStatement`/`SegmentLoader` verengen auf double). Eine exakte
  Decimal-Lane wurde entworfen (Plan liegt vor) und die Komplettumstellung
  auf BigDecimal evaluiert ([Doc 10](10-evaluation-komplettumstellung-bigdecimal.md):
  6–323× Laufzeit, 5,3× Speicher, Infinity/NaN nicht darstellbar) — beides
  per User-Entscheidung **verworfen**. Dokumentiert im dauerhaft
  `@Disabled` rolap-Test `DecimalPrecisionRoundtripTest`.

## 5. Deprecations und ihr Lösch-Fahrplan (E5)

`@Deprecated(forRemoval = true)`, Löschung frühestens ein Minor-Release
nach dieser Migration (externe Konsumenten der exportierten Pakete!):

| Symbol | Ersatz |
|---|---|
| `Util.DOUBLE_NULL` | Java `null` |
| `Util.EmptyValue`, `FunUtil.DOUBLE_EMPTY` | ersatzlos (war tot) |
| `Util.valueNotReadyException` | `NotLoaded.INSTANCE` |
| `NullSemantics.isSentinelOnly` | `NullSemantics.isNull` |

**Nicht deprecated:** `Util.nullValue` — es dient weiterhin als
In-Band-Placeholder im Objekt-Segment-Storage (`SegmentLoader`) und als
Legacy-Eingabe der Formatter-Kette. Vollständige Ablösung setzt einen
Storage-Placeholder-Umbau voraus, der mit dem verworfenen
BigDecimal-Dataset geplant war; `NullSemantics.isNull(Object)` behandelt
beide Repräsentationen transparent.

## 6. Regressionsnetz

- olap `common/src/test/.../nullsemantics/` — 120+ Unit-Tests
  (Operatoren-/Vergleichs-/Excel-Matrizen, Ordnung, Aggregate, Koerzionen,
  CompensatedSum, Paar-Statistik-Ausrichtung).
- olap `xmla/bridge` — NULL-/Fehler-/INF-Serialisierungsreferenz.
- rolap `testkit/core/.../nullsemantics/` — MDX-End-to-End (NULL-Zellen,
  Arithmetik, Ordnung, Batch-Protokoll, Kollisions-Regression).
- Volle testkit-Suite (2182 Tests) als Differenzlauf-Gate jeder Phase.

→ Phasenverlauf und Entscheidungen: [README](README.md), [Doc 99](99-handover-arbeitsnotizen.md)
