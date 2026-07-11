# 10 — Evaluation: Komplettumstellung double → BigDecimal

> Auftrag (2026-07-11): Die in Doc 04 verworfene Alternative „Überall
> BigDecimal" ernsthaft evaluieren — mit gemessenen Zahlen auf dieser
> Codebasis statt Literaturbehauptungen. Ergebnis ist eine
> Entscheidungsvorlage: Komplettumstellung vs. der laufende Dual-Lane-Weg
> (Doc 05/06, Phase 5b).

## 1. Scope-Definition: Was „komplett" bedeutet

Die Komplettumstellung ersetzt `double` als Rechenrepräsentation überall:

| Schicht | Änderung |
|---|---|
| Calc-API | `DoubleCalc extends Calc<Double>` → `Calc<BigDecimal>` (bzw. Umbenennung); alle Compiler-Pfade (`compileDouble`) |
| Operatoren/Funktionen | plus/minus/multiply/divide, alle excel/* und vba/*, LinReg-/Statistik-Kette, Rank/Percentile |
| Komparatoren | `NullSemantics.compare(double,double)`/`compareCellValues`-Zahlenpfad → `compareTo` |
| Aggregation | `FunUtil.sumDouble`/`avg`/`var`/`covariance`, rolap `SumAggregator`-Fast-Path |
| Segment-Storage | `DenseDoubleSegmentDataset`/`DenseDoubleSegmentBody` (`double[]` + Null-BitSet) → Objekt-Dataset; SegmentCache-SPI-Serialisierung |
| Intake | `SqlStatement`/`SegmentLoader`: `getBigDecimal()` statt `getDouble()` überall |
| Format/XMLA | `Format`-double-Pfade (24 Stellen util.format), XMLA-`ValueInfo`/`"INF"`-Pfad |

## 2. Blast-Radius-Inventar (gemessen 2026-07-11, Branch-Stand nach Phase 5a)

| Metrik | Wert |
|---|---|
| Dateien mit `DoubleCalc`-Referenz (olap) | **144** (578 Stellen) |
| davon rolap-Konsumenten | **0** (rolap konsumiert `Calc<?>`/`Object`) |
| davon legacy.xmla-main | 0 (nur Testquellen) |
| primitive `double`-Deklarationen olap common | 177 |
| primitive `double`-Stellen rolap core | 105 |
| transzendente `Math.*`-Aufrufe (sin/cos/exp/log/sqrt/pow/…) | **43** |
| Infinity-/NaN-Verwendungen olap common | **31** |
| `double[]`-Storage-Klassen rolap | DenseDoubleSegmentDataset/Body + Loader-Puffer |
| OSGi-API | `api.calc.DoubleCalc` ist exportierte, öffentliche API → Major-Bruch |

Einordnung: Die Calc-Umstellung ist ein olap-internes (aber API-brechendes)
Großprojekt; der eigentliche Systembruch liegt in Storage/Intake/XMLA.

## 3. Semantische Blocker (unabhängig von Performance)

### 3.1 BigDecimal kennt weder ±Infinity noch NaN

Eingefrorene, MSAS-kompatible Verhaltensanker (Phase-0/3-Tests):

- `x / NULL → +Infinity`, `x / 0 → ±Infinity` (DivideCalc, beide Flag-Zweige)
- Fehler in Aggregat-Eingaben → `Double.NaN` (`SetWrapper.errorCount`-Pfad)
- Sortier-Totalordnung `−∞ < … < NaN < +∞` (NullSemantics.compare)
- XMLA serialisiert `POSITIVE_INFINITY` als literal `"INF"`
  (`XmlaResponseConverter`, per Referenztest gepinnt)

Jede Komplettumstellung muss diese Werte **wegdefinieren**. Die Optionen:

| Option | Konsequenz |
|---|---|
| Exception bei /0 | Verhaltensbruch (heute Infinity-Zelle, morgen Fehlerzelle) — verletzt das Non-Goal „keine MSAS-/Mondrian-Abweichung"; bricht XMLA-Clients, die INF kennen |
| `ErrorValue` bei /0 | wie oben, nur strukturierter |
| Hybrid-Escape: /0 und NaN-Fälle fallen auf double zurück | ist **de facto die Dual-Lane** — nur implizit, unkonfigurierbar und an der schlechtestmöglichen Stelle (mitten im Operator) versteckt |

**Es gibt keine verhaltenskompatible Komplettumstellung.** Das ist der
härteste Befund dieser Evaluation und war in Doc 04 nur als Nebensatz
(„kein Infinity") notiert.

### 3.2 Transzendente Funktionen bleiben double

43 Stellen rechnen `Math.sin/cos/exp/log/sqrt/pow/…` — für BigDecimal nicht
definiert. Optionen: (a) durch double round-trippen (Exaktheit endet dort
ohnehin — „BigDecimal überall" ist für diese Funktionsklasse eine
Illusion), (b) big-math-Bibliothek (neue Dependency, weitere
Größenordnung Laufzeit, Rundungspolitik pro Funktion). Realistisch ist (a)
— womit die Engine intern **sowieso zweigleisig** bleibt.

### 3.3 Division erzwingt Rundung

`BigDecimal.divide` ohne MathContext wirft bei nicht-terminierender
Expansion (`1/3`) `ArithmeticException`. Jede Division braucht also eine
Rundungspolitik → Exaktheit ist prinzipiell **partiell** (exakt sind +, −,
×; ÷ ist es nie). Der Exaktheitsgewinn der Komplettumstellung gegenüber
der Dual-Lane ist damit kleiner als er scheint: genau die Operationen, die
die Dual-Lane exakt macht, wären auch die einzigen exakten der
Komplettumstellung.

## 4. Performance und Speicher (JMH, gemessen)

JMH 1.37 auf JDK 25 (Fedora 43), avgt, 3 Forks × 3×500 ms nach
3×500 ms Warmup. Rohdaten:
[`10-anhang-jmh-rohdaten.json`](10-anhang-jmh-rohdaten.json).
Das JMH-Modul war ein Einweg-Messinstrument und wurde nach der
Evaluation wieder entfernt (Quellen bei Bedarf im Commit `bb3bdaa`
dieser Branch-Historie).

### 4.1 Aggregations-Hot-Loop (Summe, 10⁶ Werte gemischter Größenordnung)

| Variante | ms/op | Faktor vs. boxed |
|---|---|---|
| `double` primitiv | 0,64 | 0,11× |
| `Double` geboxt (heutiger Calc-Pfad) | 5,72 | 1× |
| **`CompensatedSum` (Phase 5a)** | **3,55** | **0,62× — kein Overhead** |
| BigDecimal (vor-geboxt) | 36,6 | **6,4×** |
| BigDecimal DECIMAL128 | 47,5 | **8,3×** |
| BigDecimal ab double konvertiert | 65,4 | **11,4×** |

> Nebenbefund: Das Benchmark-Gate aus Doc 07 §1.4 für Phase 5a (< 5 %
> Overhead) ist mit deutlichem Abstand bestanden — die kompensierte Summe
> ist gegen die geboxte Baseline sogar schneller (Messrauschen/JIT;
> jedenfalls kein Malus).

### 4.2 Operator-Arithmetik (10⁵ Paare)

| Operation | double | BigDecimal | Faktor |
|---|---|---|---|
| Multiplikation | 0,066 ms | 4,26 ms (unlimitiert) / 10,1 ms (DECIMAL128) | **64× / 153×** |
| Division | 0,066 ms | 21,3 ms (DECIMAL128) | **323×** |
| Calc-Baum-Proxy (boxen→addieren→boxen) | 0,066 ms | 5,96 ms | **90×** |

Die Doc-04-Behauptung „1–2 Größenordnungen" ist damit **belegt und für
Arithmetik eher untertrieben** (bis 2,5 Größenordnungen bei Division).

### 4.3 Sortierung (Order/TopCount-Pfad, 10⁵ Werte)

primitiv 5,4 ms → `Double.compareTo` 13,9 ms → `BigDecimal.compareTo`
22,1 ms (**1,6× vs. boxed**) — der mildeste Pfad.

### 4.4 Speicher (10⁷ Zellen, Heap-Delta)

| Repräsentation | Bytes/Zelle | Faktor |
|---|---|---|
| `double[]` + Null-BitSet (heutiges Segment) | 8,4 | 1× |
| `BigDecimal[]` | 44,3 | **5,3×** |

Konsequenz für den Segment-Cache: **~5× weniger Zellen pro GB**, plus
GC-Druck durch 10⁷+ langlebige Objekte statt eines primitiven Arrays;
das SegmentCache-SPI-Serialisierungsformat bricht (Risiko R5), externe
Cache-Bestände werden ungültig.

## 5. Migrationskosten-Vergleich

| | Komplettumstellung | Dual-Lane (Phase 5b, Plan liegt vor) |
|---|---|---|
| Calc-Schicht | 144 Dateien / 578 Stellen, exportierte API bricht (Major) | +7 neue Calc-/Coercion-Klassen, API additiv (`DecimalCalc`, `compileDecimal` default) |
| Storage | `double[]`-Datasets sterben → 5,3× Speicher für **alle** Cubes | unverändert; BigDecimal-Dataset nur für Decimal-Measures (Phase 6, additiv) |
| Verhalten | Infinity/NaN-Semantik **bricht zwingend** (§3.1) | Default-Lane byte-identisch; Decimal-Lane opt-in per Typ |
| Tests | alle double-Erwartungswerte beider Repos anfassen | Phase-0-Suite unverändert grün (nachgewiesen: 2182 testkit-Tests) |
| Performance | Faktor 6–323× je Pfad, überall, nicht abwählbar | Faktor nur auf deklarierten Decimal-Measures |
| Rollback | praktisch keiner (Storage + API + Semantik) | Konfiguration/Typ-Deklaration; Default unberührt |
| Restnutzen ggü. Dual-Lane | Exaktheit auch für nicht-deklarierte Measures (+, −, × — nicht ÷, nicht transzendent) | — |

Der einzige echte Zusatznutzen der Komplettumstellung — Exaktheit ohne
Typ-Deklaration — ist über eine spätere, kleine Erweiterung der Dual-Lane
erreichbar (Default-Lane-Umschalter „decimal als Default-Datentyp" auf
Katalog-Ebene), ohne die Engine für alle anderen zu verlangsamen.

## 6. Empfehlung

**Komplettumstellung ablehnen; Dual-Lane (Doc 05) bestätigen.**

1. **Semantik:** Es existiert keine verhaltenskompatible Variante (§3.1) —
   das Non-Goal der Initiative würde verletzt, XMLA-Clients brechen.
2. **Physik:** 6–323× Laufzeit und 5,3× Speicher treffen **jede** Query
   und **jeden** Cube, auch die 99 %, die keine exakte Dezimalarithmetik
   brauchen; der Segment-Cache verliert 80 % seiner effektiven Kapazität.
3. **Exaktheits-Illusion:** Division und transzendente Funktionen bleiben
   auch bei BigDecimal gerundet/double — exakt wären nur +, −, ×, und
   genau die liefert die Dual-Lane für deklarierte Decimal-Measures.
4. **Kosten:** 144 API-brechende Dateien + Storage-Rewrite + kompletter
   Erwartungswert-Umbau vs. ein additiver, rückrollbarer 7-Arbeitspakete-Plan.

Falls später „exakt per Default" gewünscht ist: als Katalog-/Kontext-Schalter
auf Basis der Dual-Lane nachrüsten (Lane-Wahl-Default umdrehen), nicht durch
Entfernen der double-Lane.

→ Verworfene-Alternativen-Tabelle in [Doc 05](05-empfehlung-zielarchitektur.md)
und Optionen in [Doc 04](04-optionen-dezimal-praezision.md) bleiben gültig;
diese Evaluation ersetzt deren Kurzbegründung durch Messwerte.
