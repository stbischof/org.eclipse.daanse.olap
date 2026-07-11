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
