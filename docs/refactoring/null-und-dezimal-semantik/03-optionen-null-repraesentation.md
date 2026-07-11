# 03 — Optionen: NULL-Repräsentation und Zell-Zustände

> Bewertet werden fünf Repräsentationen für „Wert ist NULL" in der
> Calc-Schicht (§1) sowie die getrennte Frage „nicht geladen vs. NULL" im
> Ladepfad (§2). Bewertungskriterien: Korrektheit (Kollisionsfreiheit),
> Performance im Hot Path (Millionen Zell-Evaluationen), Migrationsrisiko
> (155 `DoubleCalc`-Dateien), Lesbarkeit mit Java 25, Kompatibilität zur
> MSAS-/Mondrian-Semantik.

## Vorbemerkung: die entscheidende Ausgangslage

Zwei Fakten aus Doc 01/02 dominieren die Abwägung:

1. **`Calc<E>` ist bereits boxed** (`DoubleCalc extends Calc<Double>`).
   Jede Evaluation allokiert/reicht heute schon ein `Double`-Objekt. Es gibt
   keinen primitiven Hot Path, den ein Sentinel schützen würde.
2. **Die Schicht ist de facto schon null-tolerant**: etliche Calcs prüfen
   `== DOUBLE_NULL || == null`, und `BaseExpressionCompiler.java:382`
   (`case null -> FunUtil.DOUBLE_NULL`) ist der einzige zentrale Punkt, der
   Java-`null` in den Sentinel übersetzt.

Der Sentinel ist damit kein Performance-Feature, sondern nur noch ein
historisches Artefakt mit Kollisionsrisiko.

## §1 Optionen für die Calc-Schicht

### Option a — Status quo: Sentinel behalten

| Pro | Contra |
|---|---|
| Null Migrationsaufwand | Wert-Kollision (P1), `Objects.equals`-Verschärfung, `BOOLEAN_NULL == false` (P2) |
| Bekanntes Verhalten | Vier inkonsistente Prüfidiome (P6); jede neue Funktion kann den Check vergessen |
| | „NULL-heit" ist im Typsystem unsichtbar; Compiler kann nicht helfen |
| | TODO-Kommentare markieren es selbst als Übergangslösung |

**Bewertung: nicht haltbar.** Die Kollision ist ein stiller, nicht
diagnostizierbarer Korrektheitsfehler in einem Analytik-System, dessen
einziger Zweck korrekte Zahlen sind.

### Option b — Java-`null` an der Calc-Grenze

`DoubleCalc.evaluate` gibt `null` zurück, wenn der Wert MDX-NULL ist.

| Pro | Contra |
|---|---|
| Kollisionsfrei per Konstruktion | `null` ist „unsichtbar": vergessene Checks werden NPEs statt falscher Werte (immerhin laut statt still) |
| Allokationsneutral (Interface ist schon boxed); eher billiger (kein Feldzugriff) | Auto-Unboxing-Fallen: `double d = calc.evaluate(e);` wirft NPE — erzwingt disziplinierte Boxed-Handhabung |
| Java 25 `case null ->`-Pattern-Matching macht Behandlung explizit und lesbar; im Codebase bereits etabliert | Trägt keine weiteren Zustände (nicht-geladen, Fehler) — dafür braucht es weiterhin etwas anderes |
| Minimal-invasive Migration: ein Compiler-Normalisierungspunkt + drei Konverter invertieren; Dual-Checks existieren schon | 155 Dateien müssen langfristig auf das neue Idiom (`v == null` statt `== DOUBLE_NULL`) umgestellt werden — mechanisch, aber Fläche |
| `IsEmpty`/`isNull` wird triviale `== null`-Prüfung | |
| Identisch mit dem, was `RolapEvaluator.evaluateCurrent` (:829) der Objekt-Welt heute schon liefert (nullValue → Java-null) — vereinheitlicht beide Welten | |

**Bewertung: richtig für die Calc-Grenze** — unter der Bedingung, dass die
NULL-*Semantik* (Arithmetik, Ordnung) zentralisiert wird, damit `null`-Checks
nicht erneut über 44 Stellen streuen (→ `NullSemantics`-Helfer, Doc 05).

### Option c — Primitives `double` + separater `wasNull()`-Kanal

Das JDBC-/ResultSet-Muster bzw. das Muster der Segment-Datasets
(double[] + BitSet), angewendet auf die Calc-API: `double evaluateDouble(…)`
plus Seitenkanal.

| Pro | Contra |
|---|---|
| Kollisionsfrei, boxing-frei — die einzige Option, die Allokation wirklich eliminiert | Erfordert **Umbau der Calc-API auf primitive Signaturen** — das ist ein anderes, viel größeres Projekt als die NULL-Frage |
| Bewährtes Muster (JDBC, Segment-Storage) | Seitenkanal braucht Zustand pro Evaluation → Thread-Sicherheit im parallelisierten Evaluator, Reentranz bei geschachtelten Calcs |
| | Fehleranfällig: `wasNull()` nach dem falschen Aufruf gelesen = stiller Bug (bekannte JDBC-Falle) |
| | Gewinn erst real, wenn die gesamte Kette primitiv ist — solange `Calc<Double>` boxed bleibt, reine Zusatzkomplexität |

**Bewertung: verworfen für dieses Vorhaben.** Als *späteres*
Performance-Projekt („primitive Lane") denkbar; dann ist mit Option b die
NULL-Frage bereits sauber gelöst und der Seitenkanal kann sie 1:1 abbilden.
Im Storage bleibt das Muster unverändert richtig (dort existiert es schon).

### Option d — Sealed Interface / Records: `CellValue`

```java
public sealed interface CellValue permits NullValue, NotLoaded, ErrorValue, ObjectValue {
    record NullValue()                 implements CellValue { public static final NullValue INSTANCE = new NullValue(); }
    record NotLoaded()                 implements CellValue { public static final NotLoaded INSTANCE = new NotLoaded(); }
    record ErrorValue(Throwable cause) implements CellValue { }
    record ObjectValue(Object value)   implements CellValue { }
}
```

| Pro | Contra |
|---|---|
| Alle Zell-Zustände (NULL / nicht geladen / Fehler / Wert) **im Typsystem sichtbar**; exhaustiver `switch` — Compiler erzwingt Behandlung jedes Zustands | Im Calc-Hot-Path: eine zusätzliche Allokation (`ObjectValue`-Wrapper) pro Zwischenwert — bis Project Valhalla real messbarer Overhead |
| Ersetzt drei fragile Mechanismen auf einmal: Double(0)-Lüge, Throwable-als-Wert, Sentinel | Big-Bang, wenn überall eingeführt: alle 155 `DoubleCalc`-Dateien müssten Signaturen ändern |
| Records: `equals`/`hashCode` geschenkt, Valhalla-value-class-ready (Singletons ohnehin allokationsfrei) | Doppel-Wrapping an der Grenze zur Calc-Schicht nötig (CellValue ⇄ Double) |
| Pattern-Matching-Switch ist idiomatisches Java 25, im Codebase etabliert | |

**Bewertung: richtig, aber nur an der Cell-/CellReader-Grenze** — dort, wo
die Zustände sich tatsächlich multiplizieren und heute per
Identitätsvergleich auf drei verschiedenen Ad-hoc-Kodierungen unterschieden
werden. Nicht im arithmetischen Hot Path (Allokation, Fläche).

### Option e — `Optional<Double>`

| Pro | Contra |
|---|---|
| API macht Abwesenheit explizit | **Doppel-Boxing** (Optional um Double) und eine Allokation pro Aufruf im Hot Path (`Optional.of` cached nicht) |
| | Kein `case null`-Pattern-Matching; `Optional` in Feldern/Parametern ist ein Anti-Pattern laut API-Note |
| | Trägt genau einen Zusatzzustand — für die Cell-Grenze (4 Zustände) zu wenig, für die Calc-Grenze gegenüber `null` nur Kosten ohne Nutzen |

**Bewertung: verworfen.**

### §1-Ergebnis

Keine Einheitslösung, sondern **schichtenrichtige Zuordnung** (Details und
Begründung in Doc 05):

| Schicht | Repräsentation | Option |
|---|---|---|
| Calc-Grenze (`Calc<Double>` etc.) | Java-`null` | b |
| Cell-/CellReader-/Cache-Grenze | sealed `CellValue` | d |
| Segment-Storage | primitiv + Null-BitSet (unverändert) | c (existiert dort schon) |

## §2 „Nicht geladen" vs. NULL im Ladepfad

Heutiges Protokoll (Doc 01 §4): Java-`null` = Miss, `Util.nullValue` = NULL,
`valueNotReadyException` (= `Double(0)`) = Lüge während der Collect-Phase,
Phase-Schleife re-evaluiert bis nicht mehr dirty.

### Option 2a — Status quo (Double(0)-Lüge)

Contra siehe P3 (Doc 02): arithmetikfähiger Marker, identitätsbasierte
Filterung, Korrektheit hängt an einem unsichtbaren Protokoll. Pro: nichts
außer Bestandsschutz.

### Option 2b — Exception werfen (das Ur-Mondrian-Modell)

| Pro | Contra |
|---|---|
| Nicht ignorierbar; kein Müll fließt durch Arithmetik | Zerstört das Batching: Die Collect-Phase *will* ja weiterlaufen, um möglichst viele fehlende Zellen eines Durchlaufs zu sammeln — genau deshalb wurde die Exception historisch durch die Lüge ersetzt |
| | Exceptions als Kontrollfluss pro Zelle: Stacktrace-Kosten, unleserlich |

**Bewertung: verworfen.** Die Batching-Architektur (Phase-Schleife, Dirty-Flag)
ist gut; nur der Marker-*Typ* ist krank.

### Option 2c — Dediziertes, nicht-arithmetikfähiges `NotLoaded`-Singleton ✔

Das `NotLoaded`-Singleton aus dem `CellValue`-Modell (übergangsweise auch als
eigenständiges `record NotLoadedMarker()` einführbar):

| Pro | Contra |
|---|---|
| **Fail fast:** Jeder versehentliche Cast/Rechenversuch scheitert sofort (ClassCastException) statt still `Double(0)` zu verrechnen | Coercion-Punkte (`UnknownToDoubleCalc` u. a.) müssen den Marker explizit durchreichen statt zu casten — überschaubar, da das Ergebnis des dirty-Durchlaufs ohnehin verworfen wird („nicht crashen" genügt) |
| Minimaler Umfang: `valueNotReadyException` hat in rolap genau **2 Nutzungsstellen** (FastBatchingCellReader:192, RolapResult:1304) | |
| Batching/Phase-Loop bleibt unangetastet | |
| Gleiches Muster heilt P4: `SumAggregator`s `Double.MIN_VALUE`-Marker → `null`-Initialisierung bzw. expliziter Zustand | |

**Bewertung: empfohlen.** Zusammen mit `CellValue` an der Cell-Grenze wird
das drei-wertige Rückgabeprotokoll des `CellReader` (`null` / `nullValue` /
Wert / Lüge) zu einem exhaustiven, compilergeprüften Switch über
`NotLoaded | NullValue | ErrorValue | ObjectValue`.

### Soll beim Ladeprozess „echtes NULL" festgestellt werden?

Ja — und das passiert heute schon korrekt und muss erhalten bleiben:
`SegmentLoader` unterscheidet per `ResultSet.wasNull()` bzw.
Null-Indikator-BitSet echtes SQL-NULL von 0 (Doc 01 §5), und
`SegmentWithData.getCellValue` unterscheidet „gehört nicht in dieses Segment"
(→ Miss) von „gehört hierher, ist NULL". Die Schwäche liegt nicht in der
*Feststellung*, sondern in der *Repräsentation* auf dem Weg nach oben
(In-Band-Sentinels). Genau die ersetzt das `CellValue`-Modell.

→ Präzisions-Optionen: [Doc 04](04-optionen-dezimal-praezision.md) ·
Empfehlung: [Doc 05](05-empfehlung-zielarchitektur.md)
