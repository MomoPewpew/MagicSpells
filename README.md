<div align="center">
	<br>
	<img src="https://media.discordapp.net/attachments/335237931633606656/595352328341684427/Untitled.png" alt="MagicSpells Icon">
	<hr>
	<h2><i>Magic without writing Java</i></h2>
	<!--
            Build badge is commented out until it is fixed.
            <a href="https://travis-ci.org/TheComputerGeek2/MagicSpells"><img src="https://travis-ci.org/TheComputerGeek2/MagicSpells.svg?branch=main" alt="Build Status"></a>
        -->
	<a href="https://discord.magicspells.dev"><img src="https://img.shields.io/discord/335237931633606656?color=5562e9&logo=discord&logoColor=white" alt="Discord Server"></a>
	<a href="https://github.com/TheComputerGeek2/MagicSpells/releases"><img src="https://img.shields.io/github/downloads/TheComputerGeek2/MagicSpells/total.svg" alt="Github Releases"></a>
	<img src="https://img.shields.io/bstats/servers/892" alt="In MC servers">
</div>

[//]: # (These links are here for easier hyperlink referencing and less clutter in the actual text below.)
[Discord server]: https://discord.magicspells.dev
[showcase channel]: https://canary.discord.com/channels/335237931633606656/468537255925907466
[Releases]: https://github.com/TheComputerGeek2/MagicSpells/releases
[Wiki]: https://github.com/TheComputerGeek2/MagicSpells/wiki
[SpellRepo]: https://github.com/niblexis/ms-examples
[PAPIExp]: https://github.com/JasperLorelai/Expansion-MagicSpells
[Soundboard]: https://jasperlorelai.eu/soundboard/
[Nisovin]: https://nisovin.com/
[Bukkit]: https://dev.bukkit.org/projects/magicspells
[Spigot]: https://www.spigotmc.org/resources/magicspells.60847/
[PaperMC]: https://papermc.io/
[Modrinth]: https://modrinth.com/plugin/magicspells
[DiscordBadge]: https://img.shields.io/badge/Join%20our%20Discord-blue?style=for-the-badge&color=586ff2
[WelcomeChannel]: https://media.discordapp.net/attachments/423551934784339968/957725621503541288/unknown.png

MagicSpells is a [PaperMC] plugin which gives its users the ability to modify their Minecraft servers by configuring existing features without writing Java code. It provides you with the tools for playing with blocks of logic, bringing other plugins together, playing fantastic special effects, making your own new mechanics, and more.

Check out more examples of what this plugin can do in our [Discord server] (in the [showcase channel]).

---
## Resources 📝
* [Plugin Releases (Downloads)][Releases]
* [Community Wiki][Wiki]
* [Discord server]
* [Niblexis' Public Spell Repo][SpellRepo]
* [MagicSpells PAPI Expansion][PAPIExp]
* [JasperLorelai's Soundboard][Soundboard]

---
## Classical MagicSpells ⭐
This plugin was originally created by [Nisovin], and published on [Bukkit]. After some time TheComputerGeek2 took over the project and published it on [Spigot]. Since then we dropped support for Spigot and moved to [PaperMC], and the plugin was published on [Modrinth].

---
## Support 📞
If you need help with the plugin, our Discord server can provide you community support. You can also post suggestions there or find more resources that can be useful for you.

[![Join our Discord][DiscordBadge]][Discord server]

![Screenshot of the server][WelcomeChannel]

---
## Building 🧱
The move to Gradle has made building much easier. After cloning and navigating to the source's directory, you can simply run this command:
```
./gradlew build
```
Simple right?

---
## Publishing to Maven Central 📦

Artifacts are published under the **`io.github.team-sneakymouse`** namespace (for example `io.github.team-sneakymouse:magicspells-core`). Java package names in source remain `com.nisovin.magicspells`.

### 1. What to configure before publishing

Complete these steps once per organization / machine:

1. **Sonatype Central Portal account** — Register at [central.sonatype.com](https://central.sonatype.com/).

2. **Namespace verification** — Add and verify the namespace `io.github.team-sneakymouse` under [Publishing → Namespaces](https://central.sonatype.com/publishing/namespaces). For GitHub organizations this usually means creating a short-lived verification repository under `Team-Sneakymouse` as instructed by Sonatype.

3. **User token** — Generate a token at [central.sonatype.com/usertoken](https://central.sonatype.com/usertoken) and set:
   - `mavenCentralUsername`
   - `mavenCentralPassword`

4. **GPG signing key** — Maven Central requires signed artifacts. List keys with `gpg --list-secret-keys --keyid-format=long`, then in **`.gradle/gradle.properties`** (gitignored; see [`gradle.properties.example`](gradle.properties.example)):

   ```properties
   signing.keyId=38122A0D
   signing.password=your-gpg-passphrase
   signing.gnupg.useGpgCmd=true
   ```

   Use the last 8 characters of your key id for `signing.keyId`. Publish the public key to a keyserver if you have not already.

5. **Sonatype tokens** — Put `mavenCentralUsername` and `mavenCentralPassword` in the same `.gradle/gradle.properties` file. For uploads, also expose them to Gradle (either duplicate them in **`~/.gradle/gradle.properties`**, or use the helper script below).

6. **Version** — Set the version in [`gradle.properties`](gradle.properties) (currently `4.0-Beta-13`). It must **not** end with `-SNAPSHOT` for a release on Maven Central. Pre-release labels like `4.0-Beta-13` are allowed; see [Version format](#version-format) below.

Published modules and artifact ids:

| Gradle project | Maven artifact id |
|----------------|-------------------|
| `:core` | `magicspells-core` |
| `:shop` | `magicspells-shop` |
| `:factions` | `magicspells-factions` |
| `:memory` | `magicspells-memory` |
| `:teams` | `magicspells-teams` |
| `:towny` | `magicspells-towny` |
| `:nms:shared` | `magicspells-nms-shared` |

`:nms:v1_21_4` is excluded (Paperweight dev bundle only).

**What is published:** plain library JARs, sources, Javadoc, and POMs — not the shaded plugin JAR from `./gradlew :core:shadowJar`. Published POMs only declare dependencies on `io.github.team-sneakymouse` artifacts and Kotlin. `paper-api`, plugin JARs, and other SNAPSHOT or local-only libraries are omitted because Maven Central rejects them; consumers add Paper and other deps themselves (section 3).

### 2. Commands to publish

Dry-run locally (no Sonatype or GPG credentials required):

```bash
./gradlew publishToMavenLocal
```

Signing is skipped for `publishToMavenLocal` unless signing properties are configured (see [`gradle.properties.example`](gradle.properties.example)).

Publish all seven modules to Maven Central (upload + automatic release):

```bash
chmod +x scripts/publish-maven-central.sh
./scripts/publish-maven-central.sh
```

The script loads Sonatype tokens from `.gradle/gradle.properties` and runs `publishToMavenCentral`. If tokens are already in `~/.gradle/gradle.properties`, you can run `./gradlew publishToMavenCentral` instead.

**After a successful upload:** open [Central Portal → Deployments](https://central.sonatype.com/publishing/deployments). Wait until every component is **Validated**, then click **Publish** if you did not use automatic release. Artifacts usually appear on [Maven Central](https://central.sonatype.com/search) within 10–30 minutes.

Upload without automatic release:

```bash
ORG_GRADLE_PROJECT_mavenCentralUsername=... ORG_GRADLE_PROJECT_mavenCentralPassword=... \
  ./gradlew publishToMavenCentral -PmavenCentralAutomaticPublishing=false
```

(or use the script and set `mavenCentralAutomaticPublishing=false` in `gradle.properties` if you add that property to the plugin config later)

Publish a single module only:

```bash
./scripts/publish-maven-central.sh :core:publishToMavenCentral
```

### Version format

| Version | Maven Central release? | Notes |
|---------|------------------------|--------|
| `4.0-Beta-13` | Yes | Current project version; pre-release, not a SNAPSHOT. |
| `4.0-Beta-13-SNAPSHOT` | No | Snapshot suffix is rejected in release POMs and deployments. |
| `1.21.4-R0.1-SNAPSHOT` (Paper API) | N/A | Used only on your machine / Paper’s repo; not listed in published POMs. |

`4.0-Beta-13` is fine for Central: Sonatype validated all seven components. It is a **permanent** release coordinate once published (you cannot replace that version; publish `4.0-Beta-14` or similar for fixes). For stricter [semantic versioning](https://semver.org/), future releases could use something like `4.0.0-beta.13`, but there is no need to change the version that is already validated.

### 3. Using published artifacts in other projects

Artifacts appear on Maven Central after sync (usually within 10–30 minutes). Use the `version` from [`gradle.properties`](gradle.properties) (e.g. `4.0-Beta-13`).

**Maven** (`pom.xml`):

```xml
<dependency>
  <groupId>io.github.team-sneakymouse</groupId>
  <artifactId>magicspells-core</artifactId>
  <version>4.0-Beta-13</version>
  <scope>provided</scope>
</dependency>
```

**Gradle** (Groovy DSL, `build.gradle`):

```gradle
repositories {
    mavenCentral()
}

dependencies {
    compileOnly("io.github.team-sneakymouse:magicspells-core:4.0-Beta-13")
}
```

**Gradle** (Kotlin DSL, `build.gradle.kts`):

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    compileOnly("io.github.team-sneakymouse:magicspells-core:4.0-Beta-13")
}
```

Use `magicspells-shop`, `magicspells-factions`, and the other artifact ids from the table above for extension modules. Extension modules should depend on `magicspells-core` as well as Paper:

```kotlin
repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    compileOnly("io.github.team-sneakymouse:magicspells-core:4.0-Beta-13")
    compileOnly("io.github.team-sneakymouse:magicspells-factions:4.0-Beta-13") // example extension
}
```

Paper’s API stays on Paper’s repository as a snapshot; that is normal and separate from the MagicSpells version on Maven Central.
