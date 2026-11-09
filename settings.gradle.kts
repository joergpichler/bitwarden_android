import org.gradle.authentication.http.BasicAuthentication
import java.io.FileInputStream
import java.util.Properties

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

/**
 * Loads local user-specific build properties that are not checked into source control.
 */
val userProperties = Properties().apply {
    val buildPropertiesFile = File(rootDir, "user.properties")
    if (buildPropertiesFile.exists()) {
        FileInputStream(buildPropertiesFile).use { load(it) }
    }
}

fun String?.trimToNull(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

// GitHub Packages only accepts classic PATs. Fine-grained tokens return 401.
val githubPackagesToken = (userProperties["gitHubToken"] as String?).trimToNull()
    ?: System.getenv("GPR_KEY").trimToNull()
    ?: System.getenv("GITHUB_TOKEN").trimToNull()
    ?: System.getenv("GH_TOKEN").trimToNull()
    ?: error(
        "Missing GitHub Packages token for com.bitwarden:sdk-android. " +
            "Create a classic PAT (not fine-grained) with the read:packages scope at " +
            "https://github.com/settings/tokens then set gitHubToken in " +
            "user.properties or the GITHUB_TOKEN environment variable.",
    )

// GitHub Packages 401s on a blank username. Prefer the PAT owner's GitHub username.
val githubPackagesUser = (userProperties["gitHubUser"] as String?).trimToNull()
    ?: System.getenv("GITHUB_ACTOR").trimToNull()
    ?: System.getenv("GITHUB_USER").trimToNull()
    ?: "x-access-token"

fun RepositoryHandler.bitwardenGitHubPackages(repository: String) {
    maven {
        name = "GitHubPackages (bitwarden/$repository)"
        url = uri("https://maven.pkg.github.com/bitwarden/$repository")
        credentials {
            username = githubPackagesUser
            password = githubPackagesToken
        }
        authentication {
            create<BasicAuthentication>("basic")
        }
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // sdk-android is published from sdk-internal; keep `sdk` as a fallback.
        bitwardenGitHubPackages("sdk-internal")
        bitwardenGitHubPackages("sdk")
        if ((userProperties["localSdk"] as String?).toBoolean()) {
            mavenLocal()
        }
    }
}

rootProject.name = "Bitwarden"
include(
    ":annotation",
    ":app",
    ":authenticator",
    ":authenticatorbridge",
    ":core",
    ":cxf",
    ":data",
    ":network",
    ":testharness",
    ":ui",
)
