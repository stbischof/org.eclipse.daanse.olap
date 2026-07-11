# 99 — Handover / Arbeitsnotizen für spätere Sessions

> **Meta-Dokument, kein Analyse-Inhalt.** Notizen für die Session (Mensch
> oder KI-Agent), die eine Phase aus [Doc 06](06-migrationsplan-phasen.md)
> umsetzt oder die Analyse fortführt. Erst hier lesen, dann Code anfassen.

## Gültigkeitsstand

Alle Zeilenangaben in Doc 01–09 wurden verifiziert am **2026-07-03** gegen:

| Repo | Branch | Commit |
|---|---|---|
| `org.eclipse.daanse.olap` (dieses Repo) | main | `91d233a` |
| `org.eclipse.daanse.rolap` (`/home/stbischof/git/daanse/org.eclipse.daanse.rolap`) | main | `0ed9ef3` |

Zeilennummern verschieben sich. **Grep-Anker benutzen, nicht Zeilennummern:**

```bash
# Die Sentinels (olap):
grep -n "0.000000012345\|valueNotReadyException\|EmptyValue\|sqlNullValue" \
  common/src/main/java/org/eclipse/daanse/olap/common/Util.java

# Der zentrale Normalisierungspunkt null→Sentinel (Phase 3 invertiert ihn):
grep -rn "case null -> FunUtil.DOUBLE_NULL" common/src/main/java

# Alle entboxten Sentinel-Vergleiche (Phase-1-Arbeitsliste, ~44 Treffer):
grep -rn "== FunUtil.DOUBLE_NULL\|== DOUBLE_NULL" common/src/main/java --include='*.java'

# Die abweichenden Prüfidiome (müssen in Phase 1 mit-normalisiert werden):
grep -rn "Objects.equals(o, FunUtil.DOUBLE_NULL)\|DOUBLE_NULL.equals" common/src/main/java

# Die zwei Nutzungsstellen der Lüge (Phase 2, rolap):
grep -rn "valueNotReadyException" core/src/main/java   # im rolap-Repo

# Intake-Verengungen (Phase 5b, rolap):
grep -rn "getBigDecimal(.*).doubleValue()" core/src/main/java

# Integer-Idiom (Typ-Vorarbeit Phase 5b, beide Repos):
grep -rn "new DecimalType(Integer.MAX_VALUE, 0)" --include='*.java' -r .
```

## Vollständige Fundstellen-Inventare (Stand s. o.)

Diese Listen stammen aus der Explorations-Recherche und sind in den Docs nur
zusammengefasst; hier vollständig für die Phasen-Arbeitslisten.

### Entboxte `== DOUBLE_NULL`-Vergleiche (Phase 1/3) — olap `common/src/main/java/org/eclipse/daanse/olap/`

- `fun/FunUtil.java:429,435` (compareValues double), `:803` (sum), `:950` (evaluateSet DoubleCalc[])
- `fun/sort/Sorter.java:586,592` (compareValues double), `:659` (box)
- `calc/base/type/booleanx/DoubleToBooleanCalc.java:32`
- Operatoren: `function/def/operators/divide/DivideCalc.java:46,48,57`;
  `minus/MinusCalc.java:33,34,40`; `minus/MinusPrefixCalc.java:32`;
  `multiply/MultiplyCalc.java:35`; `plus/PlusCalc.java:33,34,40`;
  `greater/GreaterCalc.java:33`; `greater/GreaterOrEqualCalc.java:33`;
  `less/LessCalc.java:33`; `less/LessOrEqualCalc.java:33`;
  `equal/EqualCalc.java:35`; `notequal/NotEqualCalc.java:35`
- Excel-Funktionen (`function/def/excel/`): `acos/AcosCalc.java:31`,
  `acos/AcoshCalc.java:31`, `asin/AsinCalc.java:31`,
  `asin/AsinhCalc.java:31` (mit `|| == null`), `cosh/CoshCalc.java:31`,
  `degrees/DegreesCalc.java:31`, `log10/Log10Calc.java:31`,
  `radians/RadiansCalc.java:31`, `sinh/SinhCalc.java:31`,
  `sqrtpi/SqrtPiCalc.java:31`, `tanh/TanhCalc.java:31`
- `function/def/vba/exp/ExpCalc.java:31` (`DOUBLE_NULL.equals(number)`)
- `calc/base/type/doublex/UnknownToDoubleCalc.java:38` (`Objects.equals`!)

### Stellen, die `DOUBLE_NULL` *erzeugen* (Phase 3: geben künftig `null`)

- `calc/base/type/doublex/IntegerToDoubleCalc.java:33`; `UnknownToDoubleCalc.java:35,39`
- `calc/base/compiler/BaseExpressionCompiler.java:382` (Konstanten-Folding)
- `calc/base/value/CurrentValueDoubleCalc.java:30`
- `function/def/aggregate/AggregateCalc.java:132` (leere Distinct-Count-Liste)
- `function/def/linreg/PointCalc.java:49`; `linreg/LinRegCalc.java:54`
- `function/def/nonstandard/CastCalc.java:109`
- `function/def/udf/nullvalue/NullValueCalc.java:29`
- `fun/FunUtil.java:532,602` (percentile/quartile leer), `:814,832` (sumDouble leer)
- Operatoren: divide `:47,58`; minus `:35`; minusprefix `:33`; multiply `:36`; plus `:35`

### `nullValue`-Objektvergleiche (Phase 1) — olap

- `fun/FunUtil.java:476,478,629,653,680,743,782,803,899`
- `fun/sort/Sorter.java:104,146,633,635`; `fun/sort/MemberComparator.java:76`;
  `fun/sort/TupleExpMemoComparator.java:123`;
  `fun/sort/HierarchicalTupleKeyComparator.java:54,57`
- `function/def/rank/Rank3MemberCalc.java:80`; `Rank3TupleCalc.java:89`
- `function/def/topbottompercentsum/TopBottomPercentSumCalc.java:75`
- `Util.java:226` (isNull selbst)
- Achtung, **nicht** verwechseln: `query/component/NullLiteralImpl.java` /
  `MdxToQueryConverter.java:320` betreffen das MDX-NULL-*Literal* (AST),
  nicht den Laufzeitwert.

### `nullValue`/Sentinel-Kontaktpunkte rolap (`core/src/main/java/org/eclipse/daanse/rolap/`)

- `common/result/CellReader.java` (Kontrakt-Javadoc :35–69)
- `common/result/FastBatchingCellReader.java:157,165,192`
- `common/result/RolapResult.java:766,1067–1074,1304`
- `common/result/RolapCell.java:154,172,177`
- `common/RolapAggregationManager.java:826–833,947–963`
- `common/star/RolapStar.java:204–236`; `common/agg/SegmentWithData.java:159–206`
- `common/agg/SegmentLoader.java:599,667,696,700–703,746`
- `common/evaluator/RolapEvaluator.java:334–337,829–855,1057–1078`
- Writeback (Phase-3/4-Risiko R4): `writeback/BatchInsertEmitter.java:101`,
  `writeback/ScenarioImpl.java:187–188,423,481,705` (doubleValue-Konversionen)

### `BigDecimal.doubleValue()`-Verluststellen (Phase 5b)

- rolap: `common/SqlStatement.java:478,573`; `common/agg/SegmentLoader.java:667,746`;
  `common/agg/SegmentBuilder.java:453` (`bigValueCount.doubleValue()` — prüfen, ob relevant)
- olap: `query/component/NumericLiteralImpl.java:79`;
  Format: `util.format/…/internal/BasicFormat.java:76` (und `AlternateFormat`
  überschreibt `format(BigDecimal)` NICHT → erbt die Verengung)

## Befunde aus Phase 0 (Charakterisierung, 2026-07-11)

Die Phase-0-Suite (`olap common/src/test/.../nullsemantics/`, 115 Tests) hat
Abweichungen gegenüber den Annahmen in Doc 01/02 festgestellt:

1. **Objekt-Overloads invertieren die NULL-Ordnung:** Die dokumentierte
   Totalordnung `−∞ < NULL < Werte < NaN < +∞` gilt nur für die
   `(double,double)`-Overloads. `FunUtil.compareValues(Object,Object)` und
   `Sorter.compareValues(Object,Object)` sortieren `Util.nullValue` unter
   **alles, auch −Infinity**. Für die Komparator-Zusammenführung (Phase 1)
   heißt das: beide Semantiken getrennt erhalten, nicht blind vereinigen.
2. **Identitäts- vs. Wert-Vergleich ist gemischt:** Operatoren-/Excel-Calcs
   prüfen das Sentinel per Boxed-`==` (Referenz!) — ein zur Laufzeit
   berechnetes wertgleiches `Double` gilt dort als echter Wert. Wertbasiert
   (und damit kollisionsanfällig) sind: `ExpCalc` (`equals`),
   `UnknownToDoubleCalc` (`Objects.equals`), die entboxenden
   `compareValues(double,double)` und `FunUtil.sum` (primitiver Re-Check).
3. **`NULL <> x` liefert `false`:** `BOOLEAN_NULL == false` macht NULL in
   allen sechs Vergleichs-Calcs von FALSE ununterscheidbar.
4. **Java-`null`-Operanden werfen heute NPE** in den sechs Vergleichs-Calcs,
   `DoubleToBooleanCalc`, `AcosCalc`, `Log10Calc`, `SqrtPiCalc`, `ExpCalc`
   (Unboxing vor Guard); Arithmetik-Operatoren und `AsinhCalc` tolerieren
   `null`. Für Phase 3 (R1-Sweep) ist das die präzise Ausgangslage.
5. **MDX-/Cell-Ebene (rolap `testkit/core/.../nullsemantics/`, 18 Tests):**
   `Cell.getValue()` liefert für NULL-Zellen bereits heute Java-`null`
   (widerspricht dem „nie null"-Javadoc — Phase 4 passt nur noch die Doku
   an, nicht das Verhalten). Die Kollisions-Zelle (`0.000000012345`) meldet
   `isNull() == true`, `getValue()` gibt aber zugleich das rohe Sentinel
   zurück — inkonsistentes Doppelgesicht, eingefroren im Companion-Test.
   `1/NULL` rendert als String `"Infinity"`. Sortierung: BASC stellt NULLs
   nach vorn, TopCount überspringt NULL-Member vollständig. `Avg` teilt
   durch die Anzahl der Nicht-NULL-Zeilen. DECIMAL(19,4): exakte Summe
   112345678901234.5682 kommt als double 1.1234567890123456E14 an
   (.0082-Rest weg). Merker fürs Testkit: `Column.setIsNullable(…)` nötig,
   sonst NOT-NULL-DDL; `Position.getMembers()` liefert dort null —
   Position direkt als `List<Member>` verwenden.
6. XMLA-Konverter (`XmlaNullCellCharacterizationTest`, xmla/bridge):
   NULL-Unterdrückung läuft ausschließlich über `Cell.isNull()`; NULL-Zelle
   mit nicht-null FORMATTED_VALUE wird **mit** Zelleintrag, aber **ohne**
   `<Value>`-Element serialisiert; alle-Properties-null → Zelle entfällt
   (außer Ordinal 0); `+Infinity` → `"INF"`.

## Offene Recherchelücken (vor Umsetzung der jeweiligen Phase klären)

1. **Measure-Typisierung (für Doc 09 Regel 2 / Phase 5b):** Die Stelle, an
   der ein rolap-Measure seinen Ausdrucks-`Type` erhält, wurde noch nicht
   lokalisiert (vermutlich `RolapBaseCubeMeasure`/`MemberType`-Aufbau im
   rolap-Repo). Dort muss das Schema-`Datatype`(p,s) → `DecimalType(p,s)`-
   Mapping ansetzen. Suchstart: `grep -rn "getDatatype()" core/src/main/java`.
2. **XMLA-NULL-Serialisierung (Phase 4, Risiko R3):** Wie genau NULL-/
   Fehlerzellen im XMLA-Modul serialisiert werden, wurde nicht inspiziert
   (olap-Repo, Modul `xmla`). Vor Phase 4 Byte-Referenztests bauen.
3. **`SegmentBuilder.java:453`** (rolap): Kontext des
   `bigValueCount.doubleValue()` unklar (Rollup-Zählung?) — prüfen, ob
   Phase-5b-relevant oder harmlos.
4. **Dritte Sentinel-Familie `EmptyValue`:** Verwendungsstellen wurden nicht
   einzeln inventarisiert (nur Definition Util:194/FunUtil:114). Vor Phase 1:
   `grep -rn "EmptyValue\|DOUBLE_EMPTY" --include='*.java'` über beide Repos.
5. **`Rank2MemberCalc`/`RankFunDef.coerceValue`:** Null-Pfad nur oberflächlich
   gesichtet; bei Phase 3 in die Differenzläufe aufnehmen.
6. **Externe Konsumenten:** Es existiert offenbar ein weiteres abhängiges
   Repo (`org.eclipse.daanse.emondrian`-Verweise im TestKit,
   `org.opencube.junit5`). Vor Phase 3/4 klären, welche Repos die
   exportierten Pakete (`olap.calc*`, `olap.function*`) konsumieren.

## Test-Infrastruktur (für Phase 0)

- TestKit: `org.opencube.junit5` (im rolap-Repo unter
  `org.eclipse.daanse.emondrian`-Testquellen), zentrale Helfer:
  `TestUtil.assertCellSetValid(CellSet)`, Foodmart-artige Referenz-Cubes.
- olap-Repo hat ein Modul `testkit` (Repo-Root) — für Phase 0 prüfen, ob
  die Charakterisierungstests dort oder im rolap-Repo leben sollen
  (Ladepfad-Tests brauchen rolap; reine Calc-/Sortier-Semantik geht in olap).

## Entscheidungen, die bei Umsetzung noch zu treffen sind

| # | Entscheidung | Kontext | Vorschlag aus den Docs |
|---|---|---|---|
| E1 | Name/Ort der `NullSemantics`-Klasse | Phase 1 | Paket `fun` oder `calc.base`; exportiert? (bnd.bnd prüfen — `calc*` ist exportiert, `fun` nicht) |
| E2 | Property-Name für Divisions-Politik der Decimal-Lane | Phase 5b | analog bestehendem `nullDenominatorProducesNull`-Flag (DivideCalc) |
| E3 | Integer-Marker-Design | Phase 5b Vorarbeit | `IntegerType extends NumericType` (neuer Typ, API-Zuwachs) vs. `NumericType.INTEGER_INSTANCE` (kein neuer Typ) — Doc 09 §3 lässt beides zu |
| E4 | Dreiwertige Boolesche Logik einführen? | nach Phase 3 | Eigenständige Entscheidung, MSAS-Referenz; Doc 09 §4 |
| E5 | `@Deprecated`-Fenster (wie viele Releases bis Phase 6 löscht) | Phase 3→6 | mind. ein Minor-Release |

## Stil-/Prozesskonventionen dieser Doc-Serie

- Sprache: Deutsch (explizite Nutzerentscheidung, keine EN/RU-Übersetzung —
  abweichend von der god-classes-Initiative).
- Nummerierung: `01–09` Inhalt, `99` Meta/Handover.
- Bei Umsetzungsbeginn: Status-Tabelle im README ergänzen (Phase → Stand),
  siehe Definition of Done in Doc 07 §4.
- Zeilenangaben in den Docs beim Berühren der Dateien bitte mitpflegen oder
  durch Grep-Anker ersetzen.
