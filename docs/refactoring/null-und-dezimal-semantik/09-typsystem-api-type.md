# 09 — Das Typsystem `org.eclipse.daanse.olap.api.type` im Detail

> Vollständige Bewertung des Typ-Pakets (`api/src/main/java/org/eclipse/daanse/olap/api/type`,
> 17 Klassen, ~1900 Zeilen) und seiner Rolle in dieser Initiative: Was leistet
> es heute, was davon ist für Null-/Dezimal-Semantik relevant, was ändert
> sich, was bleibt bewusst unangetastet.

## 1. Inventar und Rollen

Das Paket beschreibt den **statischen Typ eines MDX-Ausdrucks** — nicht den
Laufzeitwert einer Zelle. Basis-Interface `Type` (Type.java:42) mit
`usesDimension`/`usesHierarchy`/`getDimension`/`getHierarchy`/`getLevel`
(dimensionale Metadaten), `computeCommonType` (Typ-Verallgemeinerung, z. B.
für `Iif`), `isInstance` (Wert-Validierung), `getArity`.

| Typ | Art | Rolle | Relevanz für diese Initiative |
|---|---|---|---|
| `ScalarType` | Basis skalarer Typen | Oberklasse von Numeric/String/Boolean/DateTime/Null | mittelbar |
| `NumericType` | Singleton `INSTANCE` | Typ *aller* numerischen Ausdrücke; `isInstance` = `Number`/`Character` | **hoch** — heute der Einheitstyp, der jede Präzisionsinformation nivelliert |
| `DecimalType` | mit `precision`/`scale` (DecimalType.java:38) | „Dezimal mit fester Skala; Skala 0 = Integer" | **hoch — aber siehe §2: wird heute NUR als Integer-Idiom benutzt** |
| `NullType` | Singleton (NullType.java:31) | Typ des `NULL`-**Literals** (NullLiteralImpl.java:47) | mittel — kompiliert-time-Typ von NULL, nicht Laufzeit-NULL |
| `EmptyType` | Singleton | Typ des `<Empty>`-Ausdrucks (TypeUtil.java:388, Descendants/DrilldownLevel) | gering |
| `BooleanType`, `StringType`, `DateTimeType` | skalare Typen | Validierung/Dispatch | gering (Boolean: siehe §4) |
| `MemberType`, `TupleType`, `SetType`, `LevelType`, `HierarchyType`, `DimensionType`, `CubeType`, `SymbolType` | dimensionale/strukturelle Typen | Validator, Funktions-Resolver, Achsen-Prüfung | **keine** — außerhalb des Scopes, bleiben unangetastet |

Verwendungswege heute (alle **compile-/validierungszeitlich**, Beleg
Doc 01 §3):

1. **Funktions-Resolution/Validierung** — Resolver prüfen Argumenttypen,
   `TypeUtil.canConvert` (TypeUtil.java:285) entscheidet implizite
   Konversionen.
2. **Kategorisierung** — `TypeUtil.typeToCategory` (TypeUtil.java:212)
   mappt `Type` → `DataType`-Kategorie (`NullType` →, `EmptyType` →,
   `DecimalType && scale==0` → INTEGER :219, `NumericType` → NUMERIC, …).
3. **Compiler-Dispatch** — `BaseExpressionCompiler` wählt Wrapper/Lane
   (heute: alles Numerische → Double-Calc); `:259` prüft
   `DecimalType && getScale() == 0` für den Integer-Pfad.
4. **Assertions** — `requiresType(NumericType.class)`
   (AbstractProfilingNestedDoubleCalc:34) u. ä.
5. **`computeCommonType`** — Typ-Verallgemeinerung für `Iif`,
   Set-Vereinigung etc.; laut Javadoc (Type.java:124): *„The common type
   for NumericType and DecimalType(4, 2) is NumericType."*

## 2. Zentrale Befunde (Korrekturen zur bisherigen Darstellung)

### B1: `DecimalType` mit Skala ≠ 0 existiert im gesamten Code **nicht**

Grep über beide Repos: **jede** Instanziierung ist
`new DecimalType(Integer.MAX_VALUE, 0)` — das etablierte **Integer-Idiom**
(`BaseExpressionCompiler.INTEGER_TYPE:104`, HeadTail, ParallelPeriod, die
vba/*-Defaults, TypeUtil:333 …). Eine `DecimalType(19, 4)` für einen
Geldbetrag wird nirgendwo erzeugt. Die beiden einzigen `getScale()`-Abfragen
(`BaseExpressionCompiler:259`, `TypeUtil:219`) testen auf `== 0`, also auf
Ganzzahligkeit.

**Konsequenz:** Die Aussage „`DecimalType` existiert und wartet auf die
Decimal-Lane" (Doc 01/05) ist strukturell richtig (Präzision+Skala sind
modelliert), aber praktisch ist der Typ heute **semantisch umgewidmet**:
Er bedeutet im Code „Integer", nicht „exakte Dezimalzahl".

### B2: Auch das Zahlen-Literal verliert seinen Typ

`NumericLiteralImpl.getType()` (:68–70) liefert pauschal
`NumericType.INSTANCE` — obwohl der Wert als `BigDecimal` vorliegt und
Präzision/Skala trivial ableitbar wären (`bd.precision()`, `bd.scale()`).
Der Typverlust beginnt also schon *vor* dem `doubleValue()` in `:79`.

### B3: Es gibt keine Zuführung vom Schema-Datentyp zum Ausdrucks-Typ

Der logische Measure-`Datatype` (rolap, aus dem Schema; z. B.
`isNumeric()`-Checks in SegmentLoader:685) wird nirgendwo in einen
`DecimalType(p,s)`-Ausdruckstyp übersetzt. Eine `DECIMAL(19,4)`-Spalte kommt
in der Calc-Schicht als `NumericType` an — die Information, dass Exaktheit
gefordert ist, existiert im Typsystem schlicht nie.

## 3. Umgang in dieser Initiative: drei präzisierte Regeln

### Regel 1 — Lane-Schlüssel ist `DecimalType && scale > 0` (bzw. explizites Mapping), **nicht** `instanceof DecimalType`

Wegen B1 wäre `instanceof DecimalType` als Lane-Kriterium fatal: **Jeder
Integer-Ausdruck des Systems würde in die BigDecimal-Lane fallen** (das
Integer-Idiom ist die häufigste DecimalType-Verwendung). Die Lane-Wahl in
Doc 05 §5 / Doc 06 Phase 5b lautet daher präzise:

```
DecimalType mit scale > 0  (oder precision endlich und Exaktheit gefordert) → Decimal-Lane
DecimalType mit scale == 0                                                  → Integer-Pfad (wie heute)
NumericType                                                                 → Double-Lane (wie heute)
```

Sauberer noch: das Integer-Idiom mittelfristig durch einen expliziten
Marker ersetzen (z. B. `NumericType.INTEGER_INSTANCE` oder eigener
`IntegerType extends NumericType`), damit `DecimalType` wieder eindeutig
„exakte Dezimalzahl" bedeuten kann. Das ist eine kleine, mechanische
Vorarbeit der Phase 5b (alle Fundstellen sind das eine Idiom
`new DecimalType(Integer.MAX_VALUE, 0)` — grep-bar, ~15 Stellen).

### Regel 2 — Die Decimal-Lane braucht zwei neue Typ-Zuführungen

Ohne sie bliebe die Lane unerreichbar (B2/B3):

1. **Schema → Typ:** Mapping des Measure-`Datatype`
   (DECIMAL/NUMERIC mit p,s aus dem Schema/JDBC-Metadaten) auf
   `DecimalType(p, s)` beim Aufbau des Measure-Ausdruckstyps (rolap,
   Member-/Measure-Typisierung).
2. **Literal → Typ:** `NumericLiteralImpl.getType()` liefert
   `DecimalType(bd.precision(), bd.scale())` statt pauschal
   `NumericType.INSTANCE` — verlustfrei ableitbar, rückwärtskompatibel
   (DecimalType *ist* NumericType).

`computeCommonType` funktioniert dann unverändert als **Degradations-Regel**
für Mixed-Type-Ausdrücke: „NumericType ∪ DecimalType(4,2) = NumericType"
(Type.java:124) ist exakt die in Doc 04 §5 geforderte Politik
„gemischt → Double-Lane" — sie ist im Typsystem seit jeher dokumentiert,
nur nie wirksam geworden.

### Regel 3 — `NullType`/`EmptyType` bleiben; sie sind Compile-Zeit-Typen, keine Laufzeitwerte

Wichtige Abgrenzung zweier gleichnamiger Konzepte:

- **`NullType`** ist der statische Typ des MDX-Literals `NULL` im
  Ausdrucksbaum (`NullLiteralImpl.getType()`:47) — er sagt dem Validator
  „dieser Ausdruck ist das NULL-Literal", damit `canConvert` es überall
  zulässt. Er hat mit der *Laufzeit*-Repräsentation von NULL (Sentinel
  heute, Java-`null` morgen) nichts zu tun und ist von der Migration
  **nicht betroffen** — nur der vom Compiler erzeugte Konstanten-Calc
  liefert künftig `null` statt `DOUBLE_NULL`
  (BaseExpressionCompiler:382, Doc 06 Phase 3).
- **`EmptyType`** typisiert `<Empty>`-Argumente (z. B. `Descendants`-
  Überladungen) — rein strukturell, unberührt.

Ein Runtime-Typ-Tagging über dieses Paket bleibt verworfen (Doc 05 §4):
Das Paket ist als statisches Instrument konstruiert (dimensionale
Metadaten, `computeCommonType`) und dafür gut geeignet; als
Laufzeit-Wertbeschreibung wäre es das falsche Werkzeug — dafür steht an der
Cell-Grenze `CellValue` (Doc 05 §2).

## 4. Randnotiz: `BooleanType` und die dreiwertige Logik

`BooleanType` selbst ist unproblematisch. Erst die Kombination aus
`BooleanCalc extends Calc<Boolean>` (boxed, könnte `null`!) und
`BOOLEAN_NULL = false` (FunUtil:125) verhindert dreiwertige Logik (P2).
Nach Phase 3 kann `Calc<Boolean>` `null` liefern; ob die logischen
Operatoren (`AND`/`OR`/`NOT`) dann echte dreiwertige Semantik bekommen,
ist eine **eigenständige, bewusste Entscheidung** (MSAS-Verhalten als
Referenz, Charakterisierungstest in Phase 0) — das Typsystem braucht dafür
keine Änderung.

## 5. Was am Paket bewusst NICHT angefasst wird

- Die dimensionalen/strukturellen Typen (Member/Tuple/Set/Level/Hierarchy/
  Dimension/Cube/Symbol) — vollständig außerhalb des Scopes.
- Die `Type`-Interface-Signatur — keine neuen Methoden nötig; Präzision/
  Skala sind in `DecimalType` bereits modelliert.
- `computeCommonType`-Regeln — die dokumentierte Verallgemeinerung
  (Decimal ∪ Numeric = Numeric) ist genau die gewünschte
  Degradations-Politik.
- Eine Sealed-Modernisierung der Hierarchie (`sealed interface Type
  permits …`) wäre stilistisch schön, ist aber orthogonal und wegen des
  OSGi-Exports (`api` ist öffentlich) ein eigenes, API-brechendes Thema —
  hier nicht empfohlen.

## 6. Auswirkung auf den Phasenplan

Doc 06 / Phase 5b erhält durch diese Analyse drei konkretisierte Arbeitspakete:

1. Integer-Idiom entwirren: `new DecimalType(Integer.MAX_VALUE, 0)`-Stellen
   (~15, grep-bar) auf expliziten Integer-Marker umstellen; die beiden
   `getScale() == 0`-Weichen (BaseExpressionCompiler:259, TypeUtil:219)
   nachziehen.
2. Typ-Zuführung Schema → `DecimalType(p,s)` (rolap Measure-Typisierung)
   und Literal → `DecimalType` (NumericLiteralImpl.getType).
3. Lane-Weiche im Compiler auf `DecimalType && scale > 0` (nach 1. dann
   schlicht `instanceof DecimalType`).

→ zurück zur Übersicht: [README](README.md)
