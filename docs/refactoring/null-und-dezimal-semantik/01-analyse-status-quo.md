# 01 — Analyse des Status quo

> Bestandsaufnahme über beide Repos: `org.eclipse.daanse.olap` (Engine/Calc)
> und `org.eclipse.daanse.rolap` (ROLAP-Backend). Alle Pfade relativ zum
> jeweiligen Repo-Root; Zeilenangaben Stand Juli 2026 (Branch `main`).

## 1. Das Sentinel-Inventar

Alle Wert-Zustands-Sentinels sind im olap-Repo definiert
(`common/src/main/java/org/eclipse/daanse/olap/common/Util.java`):

| Sentinel | Definition | Bedeutung |
|---|---|---|
| `DOUBLE_NULL` | Util.java:179 — `Double.valueOf(0.000000012345)` | „Eine double-Berechnung hat MDX-NULL ergeben" |
| `nullValue` | Util.java:184 — `== DOUBLE_NULL` (gleiche Objektidentität) | „Der Zellwert IST NULL" (geladen, aber SQL/MDX-NULL) |
| `valueNotReadyException` | Util.java:189 — `Double.valueOf(0)` | „Zelle noch nicht im Cache" — die „Lüge" des Batch-Readers |
| `EmptyValue` | Util.java:194 — `Double.valueOf(-0.000000012345)` | MDX-EMPTY |
| `sqlNullValue` | Util.java:199 — `UtilComparable.INSTANCE` | Platzhalter für NULL-**Achsen-Keys** in Segmentdaten (≠ NULL-Measure) |
| `DOUBLE_EMPTY` | FunUtil.java:114 — `-0.000000012345` (primitiv) | dito EMPTY, primitive Fassung |
| `BOOLEAN_NULL` | FunUtil.java:125 — `false` | „NULL" für boolesche Calcs (nicht von `false` unterscheidbar!) |
| `Throwable`-Instanz als Wert | z. B. RolapCell.java:177 (rolap) | Fehlerzelle (`isError()`, Formatierung als `#ERR:`) |
| Java-`null` | — | Im Ladepfad: „kein Segment enthält diese Zelle → nachladen" |

Der zentrale Null-Test ist ein **Identitätsvergleich**:

```java
// Util.java:225
public static boolean isNull(Object o) { return o == null || o == nullValue; }
```

`FunUtil extends Util` (fun/FunUtil.java:103) — `FunUtil.DOUBLE_NULL` und
`Util.DOUBLE_NULL` sind dasselbe Objekt.

## 2. Die Calc-Hierarchie: bereits boxed

`api/…/calc/Calc.java` definiert `interface Calc<E>` mit
`E evaluate(Evaluator)` und `Type getType()`. Die typisierten
Sub-Interfaces — `DoubleCalc`, `IntegerCalc`, `LongCalc`, `FloatCalc`,
`ByteCalc`, `BooleanCalc`, `StringCalc`, `DateTimeCalc`, `MemberCalc`,
`TupleCalc`, … — sind **alle geboxt**:

```java
// api/…/calc/DoubleCalc.java:17
public interface DoubleCalc extends Calc<Double> { }
```

Das ist die wichtigste Einzelbeobachtung der ganzen Analyse: **Es gibt keinen
primitiven Hot Path, den ein Sentinel schützen müsste.** Jede Evaluation
liefert ohnehin ein `Double`-Objekt; NULL wird lediglich als das *geboxte
Sentinel-Objekt* statt als Java-`null` transportiert. Ein `null`-Rückgabewert
wäre allokationsneutral.

Die Konvertierungs-Adapter normalisieren Java-`null` → Sentinel und tragen
den Hack als TODO im Code:

- `calc/base/type/doublex/UnknownToDoubleCalc.java:34–39` — inneres `null` →
  `DOUBLE_NULL`, mit `// TODO: !!! JUST REFACTORING 0 must be null`; Zeile 38
  prüft per `Objects.equals(o, FunUtil.DOUBLE_NULL)` (Wert- statt
  Identitätsvergleich, siehe [Doc 02](02-problemfaelle.md#kollision)).
- `calc/base/type/doublex/IntegerToDoubleCalc.java:33–35` — dito.
- `calc/base/value/CurrentValueDoubleCalc.java:30` — dito.
- `calc/base/compiler/BaseExpressionCompiler.java:382` —
  `case null -> FunUtil.DOUBLE_NULL;` — **der eine zentrale
  Normalisierungspunkt** beim Konstanten-Folding (Pattern-Switch, Java 25).

Rückrichtung (Sentinel → Java-`null`): `FunUtil.box(double)` (FunUtil.java:658),
`Sorter.box(double)` (fun/sort/Sorter.java:658), `FunUtil.sum` (FunUtil.java:803).

## 3. Das Typsystem: vorhanden, aber evaluationszeitlich ungenutzt

`api/…/type/` enthält ein vollständiges Typsystem: `Type`, `ScalarType`,
`NumericType`, **`DecimalType`** (mit Präzision und Skala!), `BooleanType`,
`StringType`, `DateTimeType`, `NullType`, `EmptyType`, `MemberType`,
`TupleType`, `SetType`, ….

Es wird ausschließlich zur **Compile-/Validierungszeit** benutzt:

- `BaseExpressionCompiler.compileDouble` (Zeilen 376–395) wählt den Wrapper
  nach statischer Calc-Art und stempelt `NumericType.INSTANCE`.
- Konstruktor-Assertions: `AbstractProfilingNestedDoubleCalc.java:34`
  (`requiresType(NumericType.class)`).
- Vorbedingungs-Asserts: `FunUtil.evaluateSet` (FunUtil.java:886),
  `Sorter.java:94,139`.

Zur **Evaluationszeit** verzweigt der Code auf die Java-Klasse des
Laufzeitwerts (`instanceof Number/String/Date`) und auf Identität gegen die
Sentinels — nie auf `calc.getType()`. Insbesondere kompiliert `DecimalType`
zu Double-Calcs (`calc/base/constant/ConstantCalcs.java:30–31`), d. h. die
einzige Typinformation, die Präzisionsentscheidungen tragen könnte, wird
verworfen.

Zwei verschärfende Befunde zum Typsystem (Details und vollständiges
Paket-Inventar in [Doc 09](09-typsystem-api-type.md)): `DecimalType` mit
Skala ≠ 0 wird **nirgendwo** instanziiert — jede Verwendung ist das
Integer-Idiom `new DecimalType(Integer.MAX_VALUE, 0)`; und selbst das
Zahlen-Literal meldet als Typ pauschal `NumericType.INSTANCE`
(`NumericLiteralImpl.java:68–70`), obwohl sein `BigDecimal`-Wert
Präzision/Skala trüge.

## 4. Der Ladepfad: drei-wertiges Protokoll mit einer Lüge

Alle Pfade rolap-Repo, `core/src/main/java/org/eclipse/daanse/rolap/`.

### CellReader-Kontrakt (`common/result/CellReader.java:35–69`)

| Rückgabe | Bedeutung |
|---|---|
| Java-`null` | Zelle in keinem Cache-Segment → muss geladen werden |
| `Util.nullValue` | Zelle ist geladen und ihr Wert ist NULL |
| sonstiges Objekt | echter Wert |

### FastBatchingCellReader (`common/result/FastBatchingCellReader.java:152–193`)

Bei Cache-Miss wird der Request gesammelt (`recordCellRequest`, :191) und
statt Java-`null` die Lüge zurückgegeben:

```java
// FastBatchingCellReader.java:192
return Util.valueNotReadyException;   // == Double.valueOf(0) !
```

Ein echtes `Double(0)` fließt also während der Collect-Phase durch die
komplette Arithmetik. `isDirty()` (:228) meldet ausstehende Requests.

### Die Re-Evaluations-Schleife (`common/result/RolapResult.java`)

`phase()` (:766) lädt gesammelte Requests (`loadAggregations`) und die
Treiber-Schleifen (`executeBody` :1112–1159, `loadMembers` :902) re-evaluieren
so lange, bis nichts mehr dirty ist. Die Lüge wird beim Speichern per
Identität aussortiert:

```java
// RolapResult.java:1304
if (ci != null && o != Util.valueNotReadyException) { ci.value = o; }
```

`CellRequestQuantumExceededException` dient als Kontrollfluss-Signal zum
Flushen eines Batches (gefangen und ignoriert, z. B. :1121–1125).

### Segment-Ebene

`common/agg/SegmentWithData.getCellValue(Object[])` (:172–206) implementiert
das Protokoll sauber: Java-`null` = „gehört nicht in dieses Segment"
(woanders suchen), `Util.nullValue` = „gehört hierher, ist aber NULL".
`common/star/RolapStar.getCellFromCache` (:207–237) scannt Segmente und gibt
bei Erschöpfung Java-`null` (Miss) zurück.

### Rückkonvertierung

`common/evaluator/RolapEvaluator.evaluateCurrent()` (:829, :855) übersetzt
`nullValue` → Java-`null` für die Calc-Schicht; `valueNotReadyException` wird
**nicht** ausgepackt und fließt als `Double(0)` weiter.
`common/result/RolapCell` versteckt das Sentinel vor der API
(`getValue()` :154, `isNull()` :172).

## 5. Storage: das Positivbeispiel existiert bereits

`Segment.createDataset` (`common/agg/Segment.java:285–293`) wählt das
physische Dataset nach `BestFitColumnType`:

| Spaltentyp | Dataset | NULL-Kodierung |
|---|---|---|
| `INT` | `DenseIntSegmentDataset` (`int[]`) | separates Null-Indikator-`BitSet` |
| `DOUBLE`, `DECIMAL` | `DenseDoubleSegmentDataset` (`double[]`) | separates Null-Indikator-`BitSet` |
| `OBJECT`, `LONG`, `STRING` | `DenseObjectSegmentDataset` (`Object[]`) | Java-`null` |

```java
// DenseDoubleSegmentDataset.getObject — 0-vs-NULL sauber über BitSet:
if (value == 0 && isNull(offset)) { return null; }
```

Der Speicher löst das 0-vs-NULL-Problem also **ohne Magic Number** — über
einen separaten Null-Kanal. Genau dieses Muster fehlt der Calc-Schicht.

Beim Laden (`common/agg/SegmentLoader.processData`, :560–757) werden
SQL-NULLs je nach Spaltenart kodiert: NULL-Achsen-Key → `Util.sqlNullValue`
(:599), NULL-Measure in `OBJECT`-Spalte → `Util.nullValue` (:696), NULL in
numerischer Spalte → `wasNull()` + Null-Indikator.

## 6. Der Präzisionspfad: `double` überall, `BigDecimal` nur an den Rändern

Die gesamte Rechenschicht ist double-primitiv; `BigDecimal` taucht an genau
drei Rändern auf und wird dort verengt:

**JDBC-Intake (rolap):**

- `common/SqlStatement.java:471–485` — der Spaltentyp `DECIMAL` existiert
  laut Kommentar „only … to work around a defect in the Snowflake jdbc
  driver. there is currently no plan to support the DECIMAL/BigDecimal type
  internally"; Zeile 478: `resultSet.getBigDecimal(…).doubleValue()`
  (Überlauf-Guard auf ±Infinity, aber **Präzisionsverlust ohne Guard**).
- `common/agg/SegmentLoader.java:667` (Achsen) und `:746` (Measures) — dito.
- **Einziger erhaltender Pfad:** `SegmentLoader.java:697–703` — ist die
  Spalte `OBJECT`-typisiert und liefert der Treiber ein `BigDecimal`, bleibt
  es erhalten: `// PDI-16761 if we cast it to double type we lose precision`.

**MDX-Literale (olap):**

- `api/…/query/component/NumericLiteral.java:18` —
  `extends Literal<BigDecimal>`: der Parser bewahrt die exakte Nutzereingabe.
- `query/component/NumericLiteralImpl.java:78–79` — beim Kompilieren:
  `new ConstantDoubleCalc(NumericType.INSTANCE, getValue().doubleValue())` —
  die Präzision wird im ersten Schritt verworfen.

**Rechenkern (olap):**

- Operatoren `function/def/operators/{plus,minus,multiply,divide}` — reine
  `double`-Arithmetik (z. B. PlusCalc.java:29–45). `DivideCalc.java:45–62`:
  NULL-Nenner → `Double.POSITIVE_INFINITY` („consistent with MSAS"),
  0-Nenner → IEEE Infinity/NaN.
- Aggregation: `FunUtil.evaluateSet` (FunUtil.java:907) —
  `retval.v.add(number.doubleValue())` — **die zentrale Verengungsstelle**,
  durch die jeder set-evaluierte Wert (auch `BigDecimal` aus dem
  OBJECT-Pfad) läuft. Akkumulatoren sind naive double-Summen, keine
  Kahan-Kompensation (`sumDouble` :806–840, `avg` :789, `var` :676).
- rolap-Aggregatoren: `aggregator/SumAggregator.java:70–79` — double-Summe
  mit `Double.MIN_VALUE` als „noch kein Wert"-Marker (weiteres
  Kollisions-Smell, siehe Doc 02); `Min-/MaxAggregator.java:67` — `doubleValue()`.
- Vergleiche: `FunUtil.compareValues(Object,Object)` (:484–485) und
  `fun/sort/Sorter.java:641–642` verengen `Number` → `double`; nirgendwo
  Epsilon-Toleranz. MDX-Totalordnung: −∞ < NULL < Werte < NaN < +∞
  (FunUtil.java:404–446, dupliziert in Sorter.java:560–603).

**Format-Layer (olap, `util.format`):**

- `Format.java:516–537` hat einen `BigDecimal`-Dispatch-Zweig (:525), aber
  `internal/BasicFormat.java:75–76` verengt: `format(bigDecimal.doubleValue(), sb)`.
- Gegenbeispiel (rolap): `format/DefaultFormatter.java:43–53` formatiert
  Member-Properties über `new BigDecimal(value.toString())` exakt.

## 7. Blast-Radius-Zahlen

| Messgröße | Wert |
|---|---|
| Dateien, die `DoubleCalc` referenzieren (beide Repos) | **155** |
| Dateien, die `IntegerCalc` referenzieren | 116 |
| Entboxte `== DOUBLE_NULL`-Vergleiche | **44** |
| `Util.isNull(…)`-/`nullValue`-Identitätsstellen (beide Repos) | ~157 |
| Nutzungsstellen von `valueNotReadyException` in rolap | **2** (FastBatchingCellReader:192, RolapResult:1304) |
| `BigDecimal.doubleValue()`-Verluststellen im Intake | 4 (SqlStatement:478,573; SegmentLoader:667,746) |
| Java-Version / Records im Codebase | Java 25 (`pom.xml:39`); 34 Record-Dateien; Pattern-Switch mit `case null` bereits in Benutzung |

Interpretation: Der *lesende* Blast-Radius (alles, was `DoubleCalc` berührt)
ist groß, aber die *semantischen Drehpunkte* sind wenige und zentral —
ein Normalisierungspunkt im Compiler, drei Konverter, zwei
Lüge-Nutzungsstellen, vier Intake-Verengungen. Das prägt den Phasenplan in
[Doc 06](06-migrationsplan-phasen.md).

→ Weiter mit den konkreten Fehlerszenarien: [Doc 02](02-problemfaelle.md).
