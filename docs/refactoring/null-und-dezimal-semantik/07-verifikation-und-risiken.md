# 07 — Verifikation und Risikoregister

## 1. Teststrategie

### 1.1 Charakterisierungstests (Phase 0 — das Fundament)

Prinzip: Erst das Ist-Verhalten einfrieren, dann umbauen. Die Suite aus
Doc 06/Phase 0 deckt vier Verhaltensfamilien ab:

| Familie | Beispiele | Absicherung für Phase |
|---|---|---|
| NULL-Semantik | Ordnung (−∞ < NULL < Werte < NaN < +∞) in `Order`/`TopCount`/`Rank`; `null+1`, `x/null → +Infinity`, `IsEmpty`, `CoalesceEmpty`; `Sum({})=NULL`, `Avg` ignoriert NULL | 1, 3 |
| Ladeprotokoll | Cache-Miss → Batch → Re-Evaluation; `isDirty`; virtuelle Cubes; `currentIsEmpty` | 2, 4 |
| API-/XMLA-Oberfläche | NULL-Zelle, Fehlerzelle (`#ERR:`), formatierte Werte im XMLA-Response byte-stabil | 4 |
| Präzisions-Ist | `DECIMAL(19,4)`-Roundtrip (dokumentiert heutigen Verlust, wird in 5b grün); Kollisions-Demo `0.000000012345` (heute NULL, ab Phase 3 echter Wert) | 3, 5b |

Die beiden „Erwartet-heute-falsch"-Tests (Kollisions-Demo, Decimal-Roundtrip)
werden als solche markiert (z. B. `@Disabled` mit Verweis hierher oder
invertierte Assertion mit Kommentar) und beim Heilen zur regulären
Regression umgedreht.

### 1.2 Differenzläufe

Für Phasen 3 und 4: identische MDX-Query-Sätze gegen Alt- und Neustand auf
Referenz-Cubes (Foodmart-artige Testdaten aus dem bestehenden
`org.opencube.junit5`-TestKit, `TestUtil.assertCellSetValid`), Ergebnisse
zellweise diffen. Für Phase 5a mit Toleranz (ULP-basiert), sonst exakt.

### 1.3 Toleranz-Policy für Phase 5a

Kahan ändert Summen in den letzten Bits. Bestandstests mit exakten
double-Erwartungswerten werden auf `assertEquals(expected, actual, ulps)`
umgestellt — **nur** für Aggregat-Ergebnisse, nicht flächendeckend (sonst
verlieren die Tests Aussagekraft). Referenzwerte für adversariale Fälle
(10⁷ Summanden gemischter Größenordnung) via BigDecimal-Nachrechnung.

### 1.4 Benchmarks

Mikro-Benchmarks (JMH oder vorhandenes Pendant) für:

- Aggregations-Hot-Loop double-Lane vor/nach Kahan (Budget: < 5 %),
- Zell-Evaluation vor/nach Phase 3 (Erwartung: neutral bis minimal besser —
  ein Feldzugriff weniger),
- `CellValue`-Switch vs. Identitätsvergleich (Phase 4; Erwartung: neutral,
  liegt außerhalb der Arithmetik),
- Decimal-Lane vs. Double-Lane (dokumentierter, erwarteter Faktor — kein
  Budget, aber Transparenz für Nutzerdoku).

## 2. Risikoregister

| # | Risiko | Phase | Eintritt | Wirkung | Gegenmaßnahme |
|---|---|---|---|---|---|
| R1 | Auto-Unboxing-NPE nach Sentinel-Entfernung (`double d = calc.evaluate(e)`) | 3 | mittel | Query-Abbruch (laut, nicht still) | systematische Suche nach Unboxing-Stellen; Phase-0-Suite; Differenzläufe |
| R2 | Vergessener NULL-Check, der bisher zufällig funktionierte (Sentinel rechnete als winzige Zahl „weiter") | 3 | mittel | Verhaltensänderung einzelner Funktionen | Phase 1 hat alle Checks zentralisiert → Grep-bare Restmenge; excel/*-Calcs gezielt testen |
| R3 | XMLA-Response ändert sich (NULL-/Fehlerzellen) | 4 | niedrig–mittel | Client-Inkompatibilität | Byte-Vergleichstests Phase 0; Cell-API-Kontrakt explizit: `getValue()` liefert künftig Java-`null` — Konsumenten im Monorepo-Umfeld (xmla-Modul, odc, testkit) mitziehen |
| R4 | Writeback-Modul (rolap) umgeht die neuen Pfade | 3/4 | mittel | Writeback-Regression | Writeback-Testsuite; `ScenarioImpl`-`doubleValue()`-Stellen im Review |
| R5 | Segment-Cache-Serialisierung inkompatibel | 4/6 | niedrig | Cache-Invalidierung nach Deploy | Storage-Format bewusst NICHT ändern (CellValue lebt oberhalb); Roundtrip-Tests; für Phase 6 (BigDecimal-Dataset) Versionierung des Segment-Headers |
| R6 | Kahan bricht exakte Testerwartungen | 5a | sicher | rote Tests | Toleranz-Policy 1.3, vorab kommuniziert |
| R7 | Decimal-Lane-Semantiklücken (Division, Mixed-Type) | 5b | mittel | falsche Erwartungen bei Nutzern | Politik explizit in Doc 04 §5 / Nutzerdoku; Property-Schalter; Degradation dokumentiert |
| R8 | Externe OSGi-Konsumenten der exportierten Pakete (`olap.calc*`, `olap.function*`) brechen | 3 | niedrig | Kompilierfehler downstream | `@Deprecated`-Aliase bis Phase 6; SemVer-Bump; Release-Notes |
| R9 | Performance-Regression im Hot Path | 3/4 | niedrig | Query-Latenz | Benchmarks 1.4 als Gate pro Phase |
| R10 | Lücken in der Phase-Loop-Logik werden durch `NotLoaded` sichtbar (heute stille Fehlrechnung, morgen ClassCastException) | 2 | mittel | Crash statt stillem Fehler | gewollt (fail fast); jeder Fund ist ein realer Bug des Status quo — dokumentieren und fixen |

## 3. Rollback-Strategie

- Phasen 1, 5a: reine Revert-Commits, keine Datenformate betroffen.
- Phasen 2, 3: Revert möglich, solange Phase 6 (Sentinel-Löschung) nicht
  gelaufen ist — die Deprecated-Aliase halten beide Welten kompilierfähig.
- Phase 4: Revert vor Release trivial; nach Release nur API-verträglich
  (Cell-Kontrakt), daher Phase 4 hinter Feature-Branch/Release-Gate.
- Phase 5b/6: opt-in bzw. additiv — Abschalten der Decimal-Lane per
  Konfiguration ist der eingebaute Rollback.

## 4. Definition of Done je Phase

1. Phase-0-Suite grün (bzw. dokumentiert-invertierte Tests umgedreht).
2. Differenzlauf ohne unerklärte Zell-Diffs.
3. Benchmark-Gates eingehalten.
4. Keine neuen Warnungen im OSGi-Manifest (bnd-Export unverändert außer
   dokumentierter Ergänzungen wie `DecimalCalc`).
5. Doc-Update in diesem Ordner (Status-Tabelle im README ergänzen, sobald
   Phasen starten).
