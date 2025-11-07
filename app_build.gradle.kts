// ملف build.gradle محدث للتطبيق مع دعم Kotlin
plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
}

android {
    compileSdk 30

    defaultConfig {
        applicationId "com.fileagent.mobile"
        minSdk 21
        targetSdk 30
        versionCode 1
        versionName "1.0.0"

        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            minifyEnabled true
            shrinkResources true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
            
            // إعدادات التوقيع (تحديث قبل النشر)
            signingConfig signingConfigs.debug  // تغيير إلى release في النسخة النهائية
        }
        
        debug {
            minifyEnabled false
            applicationIdSuffix ".debug"
            versionNameSuffix "-debug"
        }
    }
    
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
        
        // تفعيل ألعاب تجريبية
        coreLibraryDesugaringEnabled true
    }
    
    kotlinOptions {
        jvmTarget = '1.8'
        
        // تحسين إعدادات Kotlin
        freeCompilerArgs += [
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.coroutines.FlowPreview"
        ]
    }
    
    // إعدادات البناء
    buildFeatures {
        viewBinding true
        dataBinding true
        buildConfig true
    }
    
    // إعدادات إضافية
    lintOptions {
        abortOnError false
        checkReleaseBuilds false
    }
    
    packagingOptions {
        pickFirst 'META-INF/DEPENDENCIES'
        pickFirst 'META-INF/LICENSE'
        pickFirst 'META-INF/LICENSE.txt'
        pickFirst 'META-INF/NOTICE'
        pickFirst 'META-INF/NOTICE.txt'
        pickFirst 'META-INF/*.kotlin_module'
    }
    
    // إعدادات التجميع
    bundle {
        language {
            enableSplit = true
        }
        density {
            enableSplit = true
        }
        abi {
            enableSplit = true
        }
    }
}

dependencies {
    // إعدادات Java 8
    coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.5'
    
    // مكتبات Android الأساسية
    implementation 'androidx.core:core-ktx:1.6.0'
    implementation 'androidx.appcompat:appcompat:1.3.1'
    implementation 'com.google.android.material:material:1.4.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.0'
    implementation 'androidx.cardview:cardview:1.0.0'
    implementation 'androidx.activity:activity-ktx:1.3.1'
    implementation 'androidx.fragment:fragment-ktx:1.3.6'
    
    // AndroidX
    implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.4.0'
    implementation 'androidx.lifecycle:lifecycle-livedata-ktx:2.4.0'
    implementation 'androidx.lifecycle:lifecycle-runtime-ktx:2.4.0'
    implementation 'androidx.navigation:navigation-fragment-ktx:2.3.5'
    implementation 'androidx.navigation:navigation-ui-ktx:2.3.5'
    implementation 'androidx.room:room-ktx:2.3.0'
    kapt 'androidx.room:room-compiler:2.3.0'
    
    // مكتبات Kotlin
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.5.2'
    implementation 'androidx.datastore:datastore-preferences:1.0.0'
    implementation 'androidx.security:security-crypto:1.0.0'
    
    // مكتبات معالجة الملفات - Java
    implementation 'com.itextpdf:itextpdf:5.5.13.2' // PDF
    implementation 'org.apache.poi:poi:5.2.2' // Excel
    implementation 'org.apache.poi:poi-ooxml:5.2.2' // Excel
    implementation 'com.google.code.gson:gson:2.8.8' // JSON
    implementation 'com.jcraft:jsch:0.1.55' // SSH/Compression
    
    // مكتبات الصور والوسائط
    implementation 'com.github.bumptech.glide:glide:4.12.0'
    kapt 'com.github.bumptech.glide:compiler:4.12.0'
    implementation 'jp.wasabeef:glide-transformations:4.3.0'
    implementation 'de.hdodenhof:circleimageview:3.1.0'
    
    // مكتبات الشبكة
    implementation 'com.squareup.okhttp3:okhttp:4.9.2'
    implementation 'com.squareup.retrofit2:retrofit:2.9.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
    implementation 'com.squareup.retrofit2:converter-moshi:2.9.0'
    
    // مكتبات واجهة المستخدم
    implementation 'com.airbnb.android:lottie:3.4.4'
    implementation 'com.github.PhilJay:MPAndroidChart:v3.1.0'
    implementation 'io.github.tonnyl.lato:lato:1.1.0'
    implementation 'com.google.accompanist:accompanist-permissions:0.20.0'
    
    // مكتبات الأمان والتشفير
    implementation 'com.scottyab.rootbeer:rootbeer-lib:0.1.0'
    implementation 'com.scottyab.rootbeer:rootbeer:0.1.0'
    
    // مكتبات الاختبار
    testImplementation 'junit:junit:4.13.2'
    testImplementation 'org.jetbrains.kotlinx:kotlinx-coroutines-test:1.5.2'
    testImplementation 'androidx.arch.core:core-testing:2.1.0'
    testImplementation 'org.mockito:mockito-core:4.0.0'
    testImplementation 'org.robolectric:robolectric:4.7.3'
    testImplementation 'com.google.truth:truth:1.1.3'
    
    androidTestImplementation 'androidx.test.ext:junit:1.1.3'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.4.0'
    androidTestImplementation 'androidx.test:runner:1.4.0'
    androidTestImplementation 'androidx.test:rules:1.4.0'
    androidTestImplementation 'androidx.test.uiautomator:uiautomator:2.2.0'
    androidTestImplementation 'androidx.test:core:1.4.0'
}

// إعدادات الأخطاء في Gradle
gradle.startParameter.excludedTaskNames += [
    ':app:transformClassesWithDesugarForDebugAndroidTest',
    ':app:transformClassesWithDesugarForReleaseAndroidTest'
]

// إعدادات ProGuard لتحسين حجم التطبيق
proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'