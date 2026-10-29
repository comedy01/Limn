# Handoff: Forge 1.16.5 ports (2026-09-27)

Goal: add Forge 1.16.5 to Limn, Lidless and Identify (all in `C:\Users\Alten\IdeaProjects\`). All three are uploaded to CurseForge and committed (2026-09-27, all added comments stripped first). Left uncommitted: this file. No comments in code. Ask before committing or uploading (short one-line commit messages, no Claude attribution).
Full background and gotchas are in the memory file `project_forge_1165_ports.md`.

## Done
- **Identify:** Forge 1.16.5 PASSES in a real install (see step 1 below).
- **Limn:** Forge 1.16.5 PASSES in a real install. Jar: `docs/release/limn-forge-1.0.1+mc1.16.5.jar`. Notes: `docs/CURSEFORGE.md`.
- **Lidless:** Forge 1.16.5 ALL CHECKS PASSED in a real install. Jar: `docs/release/lidless-forge-0.2.0+mc1.16.5.jar`. Notes: `docs/CURSEFORGE.md`.
  - Shared-code changes were regression-tested:
    - every version compiles;
    - self-tests pass on Forge 1.18.2 and 1.20.1, Fabric 1.20.1, and NeoForge 1.21.1 and 26.3.
- **Lidless Fabric 1.20-1.20.6** was uploaded to CurseForge (file ids 8990367-8990369).

## How it works
- Each repo has a separate build in `legacy-forge/` (Architectury Loom 1.17.493, Gradle 9.5.1).
  - It reads `../versions/forge/1.16.5.properties` (`packs=`, plus `main_excludes=` for shared files that a 1.16.5 pack replaces).
  - Commands: `cd legacy-forge && ./gradlew build -Pselftest`. This builds `<mod>-forge-*.jar` and `<mod>-selftest-*.jar` in `build/libs`.
- **Java 8 output:**
  - Limn uses Jabel.
  - Lidless and Identify use JvmDowngrader, because they contain records and `List.of`.
- **Testing:** `runClient` crashes (a Loom mapping gap), so test the real jars in a real install:
  - Copy both jars into `C:/Users/Alten/.portablemc-test/<work-dir>/mods/`.
  - Run: `python -m portablemc --main-dir C:/Users/Alten/.portablemc-test --work-dir C:/Users/Alten/.portablemc-test/<work-dir> start forge:1.16.5-36.2.42 -u Dev`.
  - Then grep the output for the self-test result line.
- **Regression check after shared-code edits:** `sh C:/Users/Alten/IdeaProjects/Limn/docs/compile-all.sh <repo dir>` compiles every version.
  - Then run `./gradlew runSelftest -Pminecraft_version=X -Ploader=Y` on a spread of versions.
- Run only one Gradle build or Minecraft client at a time.

## Still to do
1. ~~Identify Forge 1.16.5~~ DONE: RESULT=PASS in a real install. Jar: `Identify/docs/release/identify-forge-0.2.0+mc1.16.5.jar`. Notes: `Identify/docs/CURSEFORGE.md`.
   - Regression: every version compiles. Self-tests PASS on Forge 1.18.2 and 1.20.1. Fabric gametests PASS on 1.21.4, 1.21.8 and 26.3.
   - `compile-all.sh` does not fit Identify. On NeoForge its test source set is `nftest`, not `selftest`. Fabric 1.21-1.21.3 has neither a gametest nor a self-test. So pass the right task names.
2. ~~Re-run Limn's main-build regression~~ DONE: all 32 version configs compile, including the forgetest/nftest/gametest sources. The only failure was Fabric 1.21.1 gametests, a script artifact: that release props file has no `gametest=false`, and its main code compiles fine. Limn's test sets are `forgetest`/`nftest`, not `selftest`. Old note: It was only compiled on 5 versions plus a Forge 1.18.2 self-test, because its shared change was tiny (log4j + `Files.write`). Optionally run `compile-all.sh` on it.
3. ~~Upload the 3 Forge 1.16.5 jars~~ DONE 2026-09-27: CF file ids Limn 8991537, Lidless 8991539, Identify 8991540 (changelog "Added support for Forge 1.16.5."). Version ids: 1.16.5=8203, Forge=7498, Client=9638, Java 8=4458.
   - CurseForge game version id for 1.16.5: look it up via `https://minecraft.curseforge.com/api/game/versions`, type "Minecraft 1.16". Java 8 = its own id.
   - Upload script pattern: `feedback_curseforge_uploads` memory. Upload the newest MC version last, because the search card shows the last upload's version.
4. The user can tick NeoForge on the existing Forge 1.20.1 CurseForge files of Limn, Lidless, Identify, Swivel and Easy Crosshair (no upload needed).
5. After that, the next-mod discussion. Recommendation: damage indicators / mob health bars (see memory `project_next_mod_research`).
