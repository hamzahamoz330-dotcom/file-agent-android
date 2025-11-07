#!/bin/bash

# سكريبت بناء تطبيق Android - أداة معالجة الملفات
# أداة أتمتة لإنشاء APK بسرعة

# متغيرات التكوين
PROJECT_NAME="FileAgent"
PACKAGE_NAME="com.fileagent.mobile"
BUILD_TYPE="debug"  # أو "release"
ASSEMBLY_TYPE="apk"  # أو "aab"

# ألوان النصوص
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# دالة طباعة الرسائل الملونة
print_message() {
    local color=$1
    local message=$2
    echo -e "${color}${message}${NC}"
}

print_header() {
    echo -e "${BLUE}"
    echo "=================================================="
    echo "🚀 أداة بناء تطبيق Android - $PROJECT_NAME"
    echo "=================================================="
    echo -e "${NC}"
}

print_step() {
    print_message $BLUE "📋 الخطوة $1: $2"
}

print_success() {
    print_message $GREEN "✅ $1"
}

print_warning() {
    print_message $YELLOW "⚠️  $1"
}

print_error() {
    print_message $RED "❌ $1"
}

# التحقق من وجود Android Studio و Gradle
check_requirements() {
    print_step 1 "فحص متطلبات النظام"
    
    # التحقق من Java
    if ! command -v java &> /dev/null; then
        print_error "Java غير مثبت. يرجى تثبيت JDK 8 أو أحدث"
        exit 1
    fi
    
    local java_version=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
    print_success "Java متوفر: $java_version"
    
    # التحقق من Android SDK
    if [ -z "$ANDROID_HOME" ]; then
        print_warning "متغير ANDROID_HOME غير محدد. يرجى تحديد مسار Android SDK"
        if [ -f "/opt/android-sdk/platforms" ]; then
            export ANDROID_HOME="/opt/android-sdk"
            print_success "تم تحديد ANDROID_HOME إلى: $ANDROID_HOME"
        else
            print_error "لم يتم العثور على Android SDK"
            exit 1
        fi
    else
        print_success "Android SDK متوفر: $ANDROID_HOME"
    fi
    
    # التحقق من Gradle
    if [ ! -f "./gradlew" ]; then
        print_error "ملف gradlew غير موجود. تأكد من وجوده في مجلد المشروع"
        exit 1
    fi
    print_success "Gradle wrapper متوفر"
}

# إعداد متغيرات البيئة
setup_environment() {
    print_step 2 "إعداد متغيرات البيئة"
    
    # إضافة Android SDK إلى PATH
    export PATH=$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/tools:$ANDROID_HOME/tools/bin
    print_success "تم تحديث PATH مع Android SDK"
    
    # التحقق من أجهزة Android المتصلة
    if command -v adb &> /dev/null; then
        local connected_devices=$(adb devices | wc -l)
        if [ $connected_devices -gt 1 ]; then
            print_success "تم العثور على أجهزة Android متصلة"
            adb devices
        else
            print_warning "لا توجد أجهزة Android متصلة"
        fi
    fi
}

# تنظيف المشروع
clean_project() {
    print_step 3 "تنظيف المشروع"
    
    ./gradlew clean
    if [ $? -eq 0 ]; then
        print_success "تم تنظيف المشروع بنجاح"
    else
        print_error "فشل في تنظيف المشروع"
        exit 1
    fi
}

# فحص التبعيات
check_dependencies() {
    print_step 4 "فحص التبعيات"
    
    ./gradlew app:dependencies --configuration debugCompileClasspath
    if [ $? -eq 0 ]; then
        print_success "جميع التبعيات متوفرة"
    else
        print_warning "قد تكون هناك مشاكل في التبعيات"
    fi
}

# بناء التطبيق
build_app() {
    print_step 5 "بناء التطبيق"
    
    case $BUILD_TYPE in
        "debug")
            print_message $YELLOW "بناء نسخة debug..."
            ./gradlew assembleDebug
            ;;
        "release")
            print_message $YELLOW "بناء نسخة release..."
            ./gradlew assembleRelease
            ;;
        *)
            print_error "نوع بناء غير صحيح: $BUILD_TYPE"
            exit 1
            ;;
    esac
    
    if [ $? -eq 0 ]; then
        print_success "تم بناء التطبيق بنجاح"
    else
        print_error "فشل في بناء التطبيق"
        exit 1
    fi
}

# إنشاء Bundle (APK/AAB)
create_bundle() {
    print_step 6 "إنشاء Bundle"
    
    case $ASSEMBLY_TYPE in
        "apk")
            if [ -f "app/build/outputs/apk/$BUILD_TYPE/app-$BUILD_TYPE.apk" ]; then
                local apk_path="app/build/outputs/apk/$BUILD_TYPE/app-$BUILD_TYPE.apk"
                local apk_size=$(du -h "$apk_path" | cut -f1)
                print_success "APK جاهز: $apk_path ($apk_size)"
                echo "📁 مسار APK: $(pwd)/$apk_path"
            else
                print_error "ملف APK غير موجود"
                exit 1
            fi
            ;;
        "aab")
            print_message $YELLOW "إنشاء Android App Bundle..."
            ./gradlew bundleRelease
            if [ $? -eq 0 ]; then
                local aab_path="app/build/outputs/bundle/release/app-release.aab"
                local aab_size=$(du -h "$aab_path" | cut -f1)
                print_success "AAB جاهز: $aab_path ($aab_size)"
                echo "📁 مسار AAB: $(pwd)/$aab_path"
            else
                print_error "فشل في إنشاء AAB"
                exit 1
            fi
            ;;
        *)
            print_error "نوع bundle غير صحيح: $ASSEMBLY_TYPE"
            exit 1
            ;;
    esac
}

# تثبيت التطبيق على الجهاز المتصل
install_app() {
    if command -v adb &> /dev/null; then
        local devices=$(adb devices | grep -v "List" | grep -v "^$" | wc -l)
        if [ $devices -gt 0 ]; then
            print_step 7 "تثبيت التطبيق على الجهاز المتصل"
            
            if [ $BUILD_TYPE = "debug" ]; then
                local apk_path="app/build/outputs/apk/debug/app-debug.apk"
            else
                local apk_path="app/build/outputs/apk/release/app-release.apk"
            fi
            
            if [ -f "$apk_path" ]; then
                adb install -r "$apk_path"
                if [ $? -eq 0 ]; then
                    print_success "تم تثبيت التطبيق بنجاح"
                    adb shell am start -n com.fileagent.mobile/.MainActivity
                else
                    print_error "فشل في تثبيت التطبيق"
                fi
            else
                print_warning "ملف APK غير موجود للتثبيت"
            fi
        else
            print_warning "لا توجد أجهزة Android متصلة"
        fi
    else
        print_warning "ADB غير متوفر، يمكنك تثبيت التطبيق يدوياً"
    fi
}

# إنشاء تقرير الاختبار
generate_test_report() {
    print_step 8 "إنشاء تقرير الاختبار"
    
    ./gradlew testDebugUnitTest
    if [ -d "app/build/reports/tests" ]; then
        print_success "تم إنشاء تقرير الاختبار في: app/build/reports/tests/"
    fi
}

# إنشاء ملف README للتوزيع
create_distribution_readme() {
    print_step 9 "إنشاء ملف README للتوزيع"
    
    local build_timestamp=$(date +"%Y-%m-%d %H:%M:%S")
    local commit_hash=$(git rev-parse --short HEAD 2>/dev/null || echo "غير متوفر")
    
    cat > DISTRIBUTION_README.md << EOF
# توزيع تطبيق $PROJECT_NAME

## معلومات البناء
- **نوع البناء**: $BUILD_TYPE
- **التاريخ**: $build_timestamp
- **نوع الملف**: $ASSEMBLY_TYPE
- **الكمود المرجعي**: $commit_hash
- **جافا**: $java_version
- **Android SDK**: $ANDROID_HOME

## ملفات التوزيع
EOF

    if [ $ASSEMBLY_TYPE = "apk" ]; then
        local apk_path="app/build/outputs/apk/$BUILD_TYPE/app-$BUILD_TYPE.apk"
        if [ -f "$apk_path" ]; then
            local apk_size=$(du -h "$apk_path" | cut -f1)
            echo "- **APK**: $apk_path ($apk_size)" >> DISTRIBUTION_README.md
        fi
    elif [ $ASSEMBLY_TYPE = "aab" ]; then
        local aab_path="app/build/outputs/bundle/release/app-release.aab"
        if [ -f "$aab_path" ]; then
            local aab_size=$(du -h "$aab_path" | cut -f1)
            echo "- **AAB**: $aab_path ($aab_size)" >> DISTRIBUTION_README.md
        fi
    fi
    
    cat >> DISTRIBUTION_README.md << EOF

## متطلبات التثبيت
- Android 5.0 (API 21) أو أحدث
- مساحة فارغة: 50 ميجابايت على الأقل

## تعليمات التثبيت
1. فعّل "مصادر غير معروفة" في إعدادات الأمان
2. انقل ملف APK إلى الجهاز
3. اضغط على ملف APK واتبع التعليمات

## الميزات
- معالجة شاملة للملفات (45+ نوع)
- واجهة عربية كاملة
- معالجة محلية (لا يتم رفع البيانات)
- دعم الصور والفيديوهات والصوتيات والمستندات

## الدعم التقني
يُرجى التواصل مع فريق التطوير في حالة وجود مشاكل.

---
تم إنشاء هذا الملف تلقائياً في: $build_timestamp
EOF

    print_success "تم إنشاء ملف DISTRIBUTION_README.md"
}

# عرض معلومات المشروع
show_project_info() {
    print_step 10 "معلومات المشروع"
    
    echo -e "\n📱 معلومات التطبيق:"
    echo "  الاسم: $PROJECT_NAME"
    echo "  الحزمة: $PACKAGE_NAME"
    echo "  نوع البناء: $BUILD_TYPE"
    echo "  نوع الملف: $ASSEMBLY_TYPE"
    
    if [ -f "app/build.gradle" ]; then
        echo -e "\n⚙️ إعدادات البناء:"
        echo "  أقل إصدار Android: $(grep -o 'minSdk [0-9]*' app/build.gradle | cut -d' ' -f2)"
        echo "  المستهدف: $(grep -o 'targetSdk [0-9]*' app/build.gradle | cut -d' ' -f2)"
        echo "  إصدار التطبيق: $(grep -o 'versionName "[^"]*"' app/build.gradle | cut -d'"' -f2)"
    fi
    
    echo -e "\n📊 حجم ملفات التوزيع:"
    if [ $ASSEMBLY_TYPE = "apk" ]; then
        if [ $BUILD_TYPE = "debug" ]; then
            if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
                local size=$(du -h "app/build/outputs/apk/debug/app-debug.apk" | cut -f1)
                echo "  APK: $size"
            fi
        else
            if [ -f "app/build/outputs/apk/release/app-release.apk" ]; then
                local size=$(du -h "app/build/outputs/apk/release/app-release.apk" | cut -f1)
                echo "  APK: $size"
            fi
        fi
    elif [ $ASSEMBLY_TYPE = "aab" ]; then
        if [ -f "app/build/outputs/bundle/release/app-release.aab" ]; then
            local size=$(du -h "app/build/outputs/bundle/release/app-release.aab" | cut -f1)
            echo "  AAB: $size"
        fi
    fi
}

# دالة المساعدة
show_help() {
    echo "🚀 أداة بناء تطبيق Android - $PROJECT_NAME"
    echo ""
    echo "الاستخدام:"
    echo "  $0 [خيارات]"
    echo ""
    echo "الخيارات:"
    echo "  -t, --type TYPE      نوع البناء (debug/release) [افتراضي: debug]"
    echo "  -b, --bundle TYPE    نوع الملف (apk/aab) [افتراضي: apk]"
    echo "  -i, --install        تثبيت التطبيق على الجهاز المتصل"
    echo "  -c, --clean          تنظيف المشروع قبل البناء"
    echo "  -h, --help           عرض هذه المساعدة"
    echo "  -v, --verbose        عرض تفاصيل أكثر"
    echo ""
    echo "أمثلة:"
    echo "  $0                    # بناء نسخة debug كـ APK"
    echo "  $0 -t release -b aab  # بناء نسخة release كـ AAB"
    echo "  $0 -i                 # بناء وتثبيت على الجهاز"
    echo ""
}

# دالة رئيسية
main() {
    local build_type="debug"
    local bundle_type="apk"
    local should_install=false
    local should_clean=false
    local verbose=false
    
    # معالجة المعاملات
    while [[ $# -gt 0 ]]; do
        case $1 in
            -t|--type)
                build_type="$2"
                shift 2
                ;;
            -b|--bundle)
                bundle_type="$2"
                shift 2
                ;;
            -i|--install)
                should_install=true
                shift
                ;;
            -c|--clean)
                should_clean=true
                shift
                ;;
            -v|--verbose)
                verbose=true
                set -x
                shift
                ;;
            -h|--help)
                show_help
                exit 0
                ;;
            *)
                print_error "معامل غير معروف: $1"
                show_help
                exit 1
                ;;
        esac
    done
    
    # التحقق من صحة المعاملات
    if [[ ! "$build_type" =~ ^(debug|release)$ ]]; then
        print_error "نوع البناء غير صحيح: $build_type. يجب أن يكون 'debug' أو 'release'"
        exit 1
    fi
    
    if [[ ! "$bundle_type" =~ ^(apk|aab)$ ]]; then
        print_error "نوع الملف غير صحيح: $bundle_type. يجب أن يكون 'apk' أو 'aab'"
        exit 1
    fi
    
    # تحديث المتغيرات العامة
    BUILD_TYPE="$build_type"
    ASSEMBLY_TYPE="$bundle_type"
    
    # بدء عملية البناء
    print_header
    
    check_requirements
    setup_environment
    
    if [ "$should_clean" = true ]; then
        clean_project
    fi
    
    check_dependencies
    build_app
    create_bundle
    
    if [ "$should_install" = true ]; then
        install_app
    fi
    
    generate_test_report
    create_distribution_readme
    show_project_info
    
    print_success "🎉 تم الانتهاء من بناء التطبيق بنجاح!"
    
    echo -e "\n${GREEN}📋 ملخص العملية:${NC}"
    echo "  ✅ فحص المتطلبات"
    echo "  ✅ إعداد البيئة"
    if [ "$should_clean" = true ]; then
        echo "  ✅ تنظيف المشروع"
    fi
    echo "  ✅ فحص التبعيات"
    echo "  ✅ بناء التطبيق"
    echo "  ✅ إنشاء Bundle"
    if [ "$should_install" = true ]; then
        echo "  ✅ تثبيت التطبيق"
    fi
    echo "  ✅ تقرير الاختبار"
    echo "  ✅ ملف README"
    
    echo -e "\n${BLUE}🔗 الملفات المُنشأة:${NC}"
    if [ $ASSEMBLY_TYPE = "apk" ]; then
        if [ $BUILD_TYPE = "debug" ]; then
            echo "  📱 app/build/outputs/apk/debug/app-debug.apk"
        else
            echo "  📱 app/build/outputs/apk/release/app-release.apk"
        fi
    elif [ $ASSEMBLY_TYPE = "aab" ]; then
        echo "  📱 app/build/outputs/bundle/release/app-release.aab"
    fi
    echo "  📄 DISTRIBUTION_README.md"
    
    if [ "$verbose" = true ]; then
        echo -e "\n${YELLOW}🔧 تفاصيل إضافية:${NC}"
        echo "  📁 مجلد البناء: $(pwd)/app/build/"
        echo "  📄 ملف التكوين: $(pwd)/app/build.gradle"
        echo "  🔗 Android SDK: $ANDROID_HOME"
    fi
}

# تشغيل الدالة الرئيسية مع جميع المعاملات
main "$@"