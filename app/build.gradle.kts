plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.saasvilcas"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.saasvilcas"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "API_BASE_URL", "\"https://api.vilcaspe.com\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    buildFeatures { buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    sourceSets.getByName("main") {
        res.setSrcDirs(listOf("src/main/res", "src/main/res-auth", "src/main/res-company",
            "src/main/res-dashboard", "src/main/res-sales", "src/main/res-profile", "src/main/res-inventory", "src/main/res-shared"))
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)
    implementation(libs.work.runtime)
    testImplementation(libs.junit)
    testImplementation("org.json:json:20240303")
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}
