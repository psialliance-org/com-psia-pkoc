// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.protobuf) apply false
}

allprojects {
    configurations.all {
        resolutionStrategy {
            // Pin all BouncyCastle artifacts to one known-good version.
            // 1.85 ships IANAObjectIdentifiers in BOTH bcprov and bcutil (bc-java #2356),
            // which fails :checkDebugDuplicateClasses. 1.84 has the clean split.
            force(
                "org.bouncycastle:bcprov-jdk15to18:1.84",
                "org.bouncycastle:bcutil-jdk15to18:1.84",
                "org.bouncycastle:bcpkix-jdk15to18:1.84"
            )
        }
    }
}
