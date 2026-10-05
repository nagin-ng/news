import os

files = {
"settings.gradle.kts": """
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "ElectricityBillCalculatorPro"
include(":app")
""",

"build.gradle.kts": """
buildscript {
    ext {
        compose_version = "1.5.4"
        room_version = "2.6.1"
        kotlin_version = "1.9.20"
        nav_version = "2.7.5"
    }
}
plugins {
    id("com.android.application") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.20" apply false
    id("com.google.devtools.ksp") version "1.9.20-1.0.14" apply false
}
""",

"gradle.properties": """
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
""",

"gradle/wrapper/gradle-wrapper.properties": """
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.2-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
""",

"app/build.gradle.kts": """
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("kotlin-parcelize")
}

android {
    namespace = "com.nh.electricitybillcalculator"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.nh.electricitybillcalculator"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val room_version = "2.6.1"
    val nav_version = "2.7.5"
    val lifecycle_version = "2.6.2"

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycle_version")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:$lifecycle_version")
    implementation("androidx.activity:activity-compose:1.8.1")
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:$nav_version")
    
    // Room
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    ksp("androidx.room:room-compiler:$room_version")
    
    // Test
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
""",

"app/src/main/AndroidManifest.xml": """<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <application
        android:name=".ElectricityBillApp"
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.ElectricityBillCalculatorPro"
        tools:targetApi="31">
        
        <provider
            android:name="androidx.core.content.FileProvider"
            android:authorities="${applicationId}.fileprovider"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/filepaths" />
        </provider>

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.ElectricityBillCalculatorPro">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
""",

"app/src/main/res/xml/filepaths.xml": """<?xml version="1.0" encoding="utf-8"?>
<paths>
    <cache-path name="pdfs" path="pdfs/" />
</paths>
""",

"app/src/main/res/xml/data_extraction_rules.xml": """<?xml version="1.0" encoding="utf-8"?>
<data-extraction-rules>
    <cloud-backup>
        <include domain="file" path="."/>
        <include domain="database" path="."/>
        <include domain="sharedpref" path="."/>
    </cloud-backup>
</data-extraction-rules>
""",

"app/src/main/res/xml/backup_rules.xml": """<?xml version="1.0" encoding="utf-8"?>
<full-backup-content>
    <include domain="file" path="."/>
    <include domain="database" path="."/>
    <include domain="sharedpref" path="."/>
</full-backup-content>
""",

"app/src/main/res/values/strings.xml": """<resources>
    <string name="app_name">Electricity Bill Calculator Pro</string>
</resources>
""",

"app/src/main/res/values/themes.xml": """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.ElectricityBillCalculatorPro" parent="android:Theme.Material.Light.NoActionBar" />
</resources>
""",

"app/src/main/java/com/nh/electricitybillcalculator/ui/theme/Color.kt": """package com.nh.electricitybillcalculator.ui.theme

import androidx.compose.ui.graphics.Color

val ElectricBlue = Color(0xFF0D6EFD)
val DarkCharcoal = Color(0xFF212529)
val SoftGray = Color(0xFFF8F9FA)
val GreenPositive = Color(0xFF198754)
val RedWarning = Color(0xFFDC3545)
val OrangeAlert = Color(0xFFFD7E14)

val PrimaryLight = ElectricBlue
val OnPrimaryLight = Color.White
val PrimaryContainerLight = Color(0xFFD3E2FF)
val OnPrimaryContainerLight = Color(0xFF001A41)

val SecondaryLight = Color(0xFF535F70)
val OnSecondaryLight = Color.White
val SecondaryContainerLight = Color(0xFFD7E3F7)
val OnSecondaryContainerLight = Color(0xFF101C2B)

val BackgroundLight = SoftGray
val OnBackgroundLight = DarkCharcoal
val SurfaceLight = Color.White
val OnSurfaceLight = DarkCharcoal

val PrimaryDark = Color(0xFFA4C8FF)
val OnPrimaryDark = Color(0xFF003062)
val PrimaryContainerDark = Color(0xFF00468A)
val OnPrimaryContainerDark = Color(0xFFD3E2FF)

val SecondaryDark = Color(0xFFBBC7DB)
val OnSecondaryDark = Color(0xFF253140)
val SecondaryContainerDark = Color(0xFF3B4758)
val OnSecondaryContainerDark = Color(0xFFD7E3F7)

val BackgroundDark = Color(0xFF1A1C1E)
val OnBackgroundDark = Color(0xFFE2E2E6)
val SurfaceDark = Color(0xFF1A1C1E)
val OnSurfaceDark = Color(0xFFE2E2E6)
""",

"app/src/main/java/com/nh/electricitybillcalculator/ui/theme/Type.kt": """package com.nh.electricitybillcalculator.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)
""",

"app/src/main/java/com/nh/electricitybillcalculator/ui/theme/Theme.kt": """package com.nh.electricitybillcalculator.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
)

@Composable
fun ElectricityBillCalculatorProTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
""",

"app/src/main/java/com/nh/electricitybillcalculator/data/local/entity/Entities.kt": """package com.nh.electricitybillcalculator.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bill_calculations")
data class BillCalculationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateMillis: Long,
    val previousReading: Double,
    val currentReading: Double,
    val unitsConsumed: Double,
    val energyCharge: Double,
    val fixedCharge: Double,
    val electricityDuty: Double,
    val otherCharges: Double,
    val discount: Double,
    val totalAmount: Double
)

@Entity(tableName = "meter_readings")
data class MeterReadingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateMillis: Long,
    val reading: Double,
    val note: String
)

@Entity(tableName = "appliances")
data class ApplianceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val watts: Double,
    val quantity: Int,
    val hoursPerDay: Double,
    val daysPerMonth: Int,
    val electricityRate: Double = 0.0
)

@Entity(tableName = "tariff_slabs")
data class TariffSlabEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val minUnits: Double,
    val maxUnits: Double, // -1 means infinity
    val rate: Double
)
""",

"app/src/main/java/com/nh/electricitybillcalculator/data/local/dao/Daos.kt": """package com.nh.electricitybillcalculator.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nh.electricitybillcalculator.data.local.entity.ApplianceEntity
import com.nh.electricitybillcalculator.data.local.entity.BillCalculationEntity
import com.nh.electricitybillcalculator.data.local.entity.MeterReadingEntity
import com.nh.electricitybillcalculator.data.local.entity.TariffSlabEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BillCalculationDao {
    @Query("SELECT * FROM bill_calculations ORDER BY dateMillis DESC")
    fun getAllBills(): Flow<List<BillCalculationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: BillCalculationEntity): Long

    @Delete
    suspend fun deleteBill(bill: BillCalculationEntity)

    @Query("DELETE FROM bill_calculations")
    suspend fun deleteAll()
    
    @Query("SELECT * FROM bill_calculations WHERE id = :id")
    suspend fun getBillById(id: Long): BillCalculationEntity?
}

@Dao
interface MeterReadingDao {
    @Query("SELECT * FROM meter_readings ORDER BY dateMillis DESC")
    fun getAllReadings(): Flow<List<MeterReadingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReading(reading: MeterReadingEntity)

    @Delete
    suspend fun deleteReading(reading: MeterReadingEntity)
    
    @Query("DELETE FROM meter_readings")
    suspend fun deleteAll()
}

@Dao
interface ApplianceDao {
    @Query("SELECT * FROM appliances ORDER BY name ASC")
    fun getAllAppliances(): Flow<List<ApplianceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppliance(appliance: ApplianceEntity)

    @Update
    suspend fun updateAppliance(appliance: ApplianceEntity)

    @Delete
    suspend fun deleteAppliance(appliance: ApplianceEntity)
    
    @Query("DELETE FROM appliances")
    suspend fun deleteAll()
}

@Dao
interface TariffSlabDao {
    @Query("SELECT * FROM tariff_slabs ORDER BY minUnits ASC")
    fun getAllSlabs(): Flow<List<TariffSlabEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSlab(slab: TariffSlabEntity)

    @Update
    suspend fun updateSlab(slab: TariffSlabEntity)

    @Delete
    suspend fun deleteSlab(slab: TariffSlabEntity)
    
    @Query("DELETE FROM tariff_slabs")
    suspend fun deleteAll()
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(slabs: List<TariffSlabEntity>)
}
"""

}

for path, content in files.items():
    full_path = os.path.join('/data/data/com.termux/files/home/projects/ElectricityBillCalculatorPro', path)
    os.makedirs(os.path.dirname(full_path), exist_ok=True)
    with open(full_path, 'w', encoding='utf-8') as f:
        f.write(content)

print("Part 1 created successfully!")
