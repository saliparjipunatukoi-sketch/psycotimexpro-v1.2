package com.example.service

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.StadiumBorder
import com.example.ui.theme.StadiumSurfaceVariant
import com.example.ui.theme.TextSecondary

enum class AppLanguage(val code: String, val shortLabel: String, val displayName: String, val flag: String) {
    ENGLISH("en", "EN", "English", "🇬🇧"),
    JAPANESE("ja", "JP", "日本語", "🇯🇵"),
    CHINESE("zh", "ZH", "中文", "🇨🇳"),
    MALAY("ms", "BM", "Melayu", "🇲🇾")
}

data class CountryDialSpec(
    val countryName: String,
    val flag: String,
    val dialCode: String,
    val isMalaysia: Boolean = countryName.equals("Malaysia", ignoreCase = true)
)

val COUNTRY_DIAL_SPECS = listOf(
    CountryDialSpec("Malaysia", "🇲🇾", "+60", true),
    CountryDialSpec("Indonesia", "🇮🇩", "+62", false),
    CountryDialSpec("Singapore", "🇸🇬", "+65", false),
    CountryDialSpec("Japan", "🇯🇵", "+81", false),
    CountryDialSpec("Sri Lanka", "🇱🇰", "+94", false),
    CountryDialSpec("China", "🇨🇳", "+86", false),
    CountryDialSpec("Thailand", "🇹🇭", "+66", false),
    CountryDialSpec("Brunei", "🇧🇳", "+673", false),
    CountryDialSpec("Philippines", "🇵🇭", "+63", false),
    CountryDialSpec("India", "🇮🇳", "+91", false),
    CountryDialSpec("United States", "🇺🇸", "+1", false),
    CountryDialSpec("United Kingdom", "🇬🇧", "+44", false),
    CountryDialSpec("Australia", "🇦🇺", "+61", false),
    CountryDialSpec("Other", "🌐", "+", false)
)

data class SubscriptionPlanSpec(
    val id: String,
    val rmPrice: Int,
    val usdPrice: Int,
    val athletesLimit: Int,
    val subCoachLimit: Int
) {
    fun formattedPrice(country: String): String {
        val isMy = country.trim().equals("Malaysia", ignoreCase = true) || country.contains("+60")
        return if (isMy) "RM $rmPrice" else "USD $$usdPrice"
    }
}

object SubscriptionPricing {
    const val ADMIN_WHATSAPP_NUMBER = "60195326399"
    const val ADMIN_WHATSAPP_DISPLAY = "+6019-5326399"

    val plans = listOf(
        SubscriptionPlanSpec("plan_rm30", rmPrice = 30, usdPrice = 7, athletesLimit = 30, subCoachLimit = 2),
        SubscriptionPlanSpec("plan_rm40", rmPrice = 40, usdPrice = 10, athletesLimit = 50, subCoachLimit = 2),
        SubscriptionPlanSpec("plan_rm60", rmPrice = 60, usdPrice = 15, athletesLimit = 80, subCoachLimit = 3),
        SubscriptionPlanSpec("plan_rm80", rmPrice = 80, usdPrice = 20, athletesLimit = 100, subCoachLimit = 5)
    )

    fun isMalaysia(country: String): Boolean {
        return country.trim().equals("Malaysia", ignoreCase = true)
    }

    fun formatPrice(rmAmount: Int, country: String): String {
        if (isMalaysia(country)) return "RM $rmAmount"
        val usd = when (rmAmount) {
            30 -> 7
            40 -> 10
            60 -> 15
            80 -> 20
            else -> (rmAmount / 4).coerceAtLeast(5)
        }
        return "USD $$usd"
    }
}

/**
 * Custom Composable Hook (`useLanguage` equivalent) to manage localization state
 * and dynamically translate navigation labels, hero section, and system bar text.
 */
@Stable
class LanguageHookState(
    val currentLanguage: AppLanguage,
    val onLanguageChange: (AppLanguage) -> Unit
) {
    fun t(key: String): String = LanguageStrings.get(key, currentLanguage)
}

val LocalLanguageHook = compositionLocalOf {
    LanguageHookState(AppLanguage.MALAY) {}
}

@Composable
fun rememberLanguageHook(
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit
): LanguageHookState {
    return remember(currentLanguage, onLanguageChange) {
        LanguageHookState(currentLanguage, onLanguageChange)
    }
}

/**
 * Reusable Language Toggle Component for English, Japanese, Chinese, and Malay.
 */
@Composable
fun LanguageToggleBar(
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        color = StadiumSurfaceVariant,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, StadiumBorder),
        modifier = modifier.testTag("language_toggle_bar")
    ) {
        Row(
            modifier = Modifier.padding(if (compact) 2.dp else 4.dp),
            horizontalArrangement = Arrangement.spacedBy(if (compact) 2.dp else 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Prioritize English, Japanese, Chinese, and Malay
            val orderedLanguages = listOf(
                AppLanguage.MALAY,
                AppLanguage.ENGLISH,
                AppLanguage.JAPANESE,
                AppLanguage.CHINESE
            )
            orderedLanguages.forEach { lang ->
                val isSelected = currentLanguage == lang
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) ElectricCyan else Color.Transparent,
                    modifier = Modifier
                        .clickable { onLanguageChange(lang) }
                        .testTag("lang_btn_${lang.code}")
                ) {
                    Text(
                        text = if (compact) "${lang.flag} ${lang.shortLabel}" else "${lang.flag} ${lang.displayName}",
                        fontSize = if (compact) 9.sp else 11.sp,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                        color = if (isSelected) Color.Black else TextSecondary,
                        modifier = Modifier.padding(
                            horizontal = if (compact) 6.dp else 10.dp,
                            vertical = if (compact) 3.dp else 6.dp
                        )
                    )
                }
            }
        }
    }
}

object LanguageStrings {

    fun get(key: String, lang: AppLanguage): String {
        return translations[key]?.get(lang) ?: translations[key]?.get(AppLanguage.ENGLISH) ?: key
    }

    private val translations = mapOf(
        // System Bar Text
        "sys_superadmin" to mapOf(
            AppLanguage.MALAY to "Superadmin",
            AppLanguage.ENGLISH to "Superadmin",
            AppLanguage.JAPANESE to "最高管理者",
            AppLanguage.CHINESE to "超级管理员"
        ),
        "sys_coach" to mapOf(
            AppLanguage.MALAY to "Jurulatih",
            AppLanguage.ENGLISH to "Coach",
            AppLanguage.JAPANESE to "コーチ",
            AppLanguage.CHINESE to "教练"
        ),
        "sys_status_online" to mapOf(
            AppLanguage.MALAY to "SISTEM AKTIF",
            AppLanguage.ENGLISH to "SYSTEM ONLINE",
            AppLanguage.JAPANESE to "システム稼働中",
            AppLanguage.CHINESE to "系统在线"
        ),
        "sys_bt_locked" to mapOf(
            AppLanguage.MALAY to "BLE-M3 TERKUNCI",
            AppLanguage.ENGLISH to "BLE-M3 LOCKED",
            AppLanguage.JAPANESE to "BLE-M3 ロック",
            AppLanguage.CHINESE to "BLE-M3 已锁定"
        ),

        // Navigation Labels
        "tab_dashboard" to mapOf(
            AppLanguage.MALAY to "Utama",
            AppLanguage.ENGLISH to "Home",
            AppLanguage.JAPANESE to "ホーム",
            AppLanguage.CHINESE to "首页"
        ),
        "tab_timing" to mapOf(
            AppLanguage.MALAY to "Timing ET",
            AppLanguage.ENGLISH to "ET Timing",
            AppLanguage.JAPANESE to "電子計時",
            AppLanguage.CHINESE to "电子计时"
        ),
        "tab_sand_jump" to mapOf(
            AppLanguage.MALAY to "Lompat AI",
            AppLanguage.ENGLISH to "Jump AI",
            AppLanguage.JAPANESE to "走幅跳AI",
            AppLanguage.CHINESE to "跳远AI"
        ),
        "tab_replay" to mapOf(
            AppLanguage.MALAY to "Replay",
            AppLanguage.ENGLISH to "Replay",
            AppLanguage.JAPANESE to "リプレイ",
            AppLanguage.CHINESE to "回放"
        ),
        "tab_athletes" to mapOf(
            AppLanguage.MALAY to "Atlit & PB",
            AppLanguage.ENGLISH to "Athletes",
            AppLanguage.JAPANESE to "選手・PB",
            AppLanguage.CHINESE to "运动员"
        ),
        "tab_fees" to mapOf(
            AppLanguage.MALAY to "Yuran",
            AppLanguage.ENGLISH to "Fees",
            AppLanguage.JAPANESE to "会費",
            AppLanguage.CHINESE to "费用"
        ),
        "tab_sports_science" to mapOf(
            AppLanguage.MALAY to "Sains AI",
            AppLanguage.ENGLISH to "Science AI",
            AppLanguage.JAPANESE to "科学AI",
            AppLanguage.CHINESE to "运动科学"
        ),
        "tab_settings" to mapOf(
            AppLanguage.MALAY to "Tetapan",
            AppLanguage.ENGLISH to "Settings",
            AppLanguage.JAPANESE to "設定",
            AppLanguage.CHINESE to "设置"
        ),

        // Hero Section (Dashboard & Landing)
        "hero_badge" to mapOf(
            AppLanguage.MALAY to "SISTEM MASA ELEKTRONIK & AI BIOMEKANIK",
            AppLanguage.ENGLISH to "ELECTRONIC TIMING & AI BIOMECHANICS",
            AppLanguage.JAPANESE to "電子計時・AIバイオメカニクスシステム",
            AppLanguage.CHINESE to "电子计时与AI生物力学分析系统"
        ),
        "hero_role_superadmin" to mapOf(
            AppLanguage.MALAY to "SUPERADMIN",
            AppLanguage.ENGLISH to "SUPERADMIN",
            AppLanguage.JAPANESE to "最高管理者",
            AppLanguage.CHINESE to "超级管理员"
        ),
        "hero_role_coach" to mapOf(
            AppLanguage.MALAY to "JURULATIH UTAMA",
            AppLanguage.ENGLISH to "HEAD COACH",
            AppLanguage.JAPANESE to "ヘッドコーチ",
            AppLanguage.CHINESE to "主教练"
        ),
        "hero_stat_athletes" to mapOf(
            AppLanguage.MALAY to "Atlit Berdaftar",
            AppLanguage.ENGLISH to "Registered Athletes",
            AppLanguage.JAPANESE to "登録選手数",
            AppLanguage.CHINESE to "注册运动员"
        ),
        "hero_stat_races" to mapOf(
            AppLanguage.MALAY to "Jumlah Race ET",
            AppLanguage.ENGLISH to "Total ET Races",
            AppLanguage.JAPANESE to "総計時レース数",
            AppLanguage.CHINESE to "电子计时总场次"
        ),
        "hero_quick_actions" to mapOf(
            AppLanguage.MALAY to "Tindakan Pantas",
            AppLanguage.ENGLISH to "Quick Actions",
            AppLanguage.JAPANESE to "クイックアクション",
            AppLanguage.CHINESE to "快捷操作"
        ),
        "hero_btn_start_et" to mapOf(
            AppLanguage.MALAY to "Mula Timing ET",
            AppLanguage.ENGLISH to "Start ET Timing",
            AppLanguage.JAPANESE to "電子計時を開始",
            AppLanguage.CHINESE to "开始电子计时"
        ),
        "hero_btn_add_athlete" to mapOf(
            AppLanguage.MALAY to "Daftar Atlit",
            AppLanguage.ENGLISH to "Add Athlete",
            AppLanguage.JAPANESE to "選手を登録",
            AppLanguage.CHINESE to "添加运动员"
        ),
        "hero_btn_website" to mapOf(
            AppLanguage.MALAY to "Website Rasmi",
            AppLanguage.ENGLISH to "Official Website",
            AppLanguage.JAPANESE to "公式ウェブサイト",
            AppLanguage.CHINESE to "官方网站"
        ),
        "hero_btn_sync" to mapOf(
            AppLanguage.MALAY to "Sync Portal Web",
            AppLanguage.ENGLISH to "Web Cloud Sync",
            AppLanguage.JAPANESE to "クラウド同期",
            AppLanguage.CHINESE to "云端同步"
        ),
        "hero_squad_title" to mapOf(
            AppLanguage.MALAY to "Senarai Pelatih & Profil Atlit",
            AppLanguage.ENGLISH to "Athlete Squad & PB Profiles",
            AppLanguage.JAPANESE to "選手一覧・自己ベスト(PB)",
            AppLanguage.CHINESE to "运动员名单与个人最佳(PB)"
        ),
        "hero_squad_subtitle" to mapOf(
            AppLanguage.MALAY to "Termasuk gambar, acara dan Personal Best (PB)",
            AppLanguage.ENGLISH to "Includes photo, event specialization and Personal Best (PB)",
            AppLanguage.JAPANESE to "写真、専門種目、自己ベスト記録を含む",
            AppLanguage.CHINESE to "包含照片、专项与个人最好成绩(PB)"
        ),
        "hero_manage_athletes" to mapOf(
            AppLanguage.MALAY to "Urus Atlit",
            AppLanguage.ENGLISH to "Manage",
            AppLanguage.JAPANESE to "選手管理",
            AppLanguage.CHINESE to "管理运动员"
        ),

        // Auth & Onboarding
        "auth_title" to mapOf(
            AppLanguage.MALAY to "Log Masuk & Pendaftaran Jurulatih (Wajib Lengkap)",
            AppLanguage.ENGLISH to "Coach Login & Mandatory Registration",
            AppLanguage.JAPANESE to "コーチログイン・新規登録（必須項目）",
            AppLanguage.CHINESE to "教练登录与必填注册信息"
        ),
        "auth_subtitle" to mapOf(
            AppLanguage.MALAY to "Sistem Pengiraan Masa Elektronik & Analisis Biomekanik Pintar",
            AppLanguage.ENGLISH to "Electronic Timing & Smart Biomechanics Analysis System",
            AppLanguage.JAPANESE to "電子計時システムとAIバイオメカニクス分析",
            AppLanguage.CHINESE to "电子计时与智能生物力学分析系统"
        ),
        "field_fullname" to mapOf(
            AppLanguage.MALAY to "Nama Penuh Jurulatih (Wajib) *",
            AppLanguage.ENGLISH to "Coach Full Name (Required) *",
            AppLanguage.JAPANESE to "コーチ氏名（必須） *",
            AppLanguage.CHINESE to "教练全名（必填） *"
        ),
        "field_country" to mapOf(
            AppLanguage.MALAY to "Negara & Kod Panggilan *",
            AppLanguage.ENGLISH to "Country & Dial Code *",
            AppLanguage.JAPANESE to "国・地域（国番号） *",
            AppLanguage.CHINESE to "国家与区号 *"
        ),
        "field_phone" to mapOf(
            AppLanguage.MALAY to "Nombor Telefon WhatsApp (Wajib) *",
            AppLanguage.ENGLISH to "WhatsApp Contact Number (Required) *",
            AppLanguage.JAPANESE to "WhatsApp電話番号（必須） *",
            AppLanguage.CHINESE to "WhatsApp 手机号码（必填） *"
        ),
        "field_club" to mapOf(
            AppLanguage.MALAY to "Nama Kelab / Sekolah / Pasukan *",
            AppLanguage.ENGLISH to "Club / School / Team Name *",
            AppLanguage.JAPANESE to "所属クラブ・学校・チーム名 *",
            AppLanguage.CHINESE to "俱乐部 / 学校 / 队伍名称 *"
        ),
        "field_cert" to mapOf(
            AppLanguage.MALAY to "Sijil Kejurulatihan / Tahap Kelayakan",
            AppLanguage.ENGLISH to "Coaching Certification / Level",
            AppLanguage.JAPANESE to "指導資格・ライセンス",
            AppLanguage.CHINESE to "教练资质与证书等级"
        ),
        "field_spec" to mapOf(
            AppLanguage.MALAY to "Acara Pengkhususan",
            AppLanguage.ENGLISH to "Specialization Event",
            AppLanguage.JAPANESE to "専門種目",
            AppLanguage.CHINESE to "主项 / 专项"
        ),
        "btn_google_signin" to mapOf(
            AppLanguage.MALAY to "Daftar & Log Masuk dengan Google",
            AppLanguage.ENGLISH to "Register & Sign In with Google",
            AppLanguage.JAPANESE to "登録してGoogleでサインイン",
            AppLanguage.CHINESE to "注册并使用 Google 登录"
        ),
        "free_trial_banner" to mapOf(
            AppLanguage.MALAY to "Percubaan Percuma 7 Hari Diaktifkan Secara Automatik!",
            AppLanguage.ENGLISH to "Auto 7-Day Free Trial Activated upon Registration!",
            AppLanguage.JAPANESE to "登録時に7日間の無料トライアルが自動適用されます！",
            AppLanguage.CHINESE to "注册即享7天全功能免费试用！"
        ),
        "err_fullname_phone_required" to mapOf(
            AppLanguage.MALAY to "Sila isi Nama Penuh dan Nombor Telefon mengikut kod negara terlebih dahulu.",
            AppLanguage.ENGLISH to "Please enter your Full Name and Contact Number with country code first.",
            AppLanguage.JAPANESE to "氏名と国番号付きの電話番号を入力してください。",
            AppLanguage.CHINESE to "请先填写您的全名以及带国家区号的联系电话。"
        ),

        // Paywall, Pricing & Subscription
        "trial_active" to mapOf(
            AppLanguage.MALAY to "PERCUBAAN PERCUMA AKTIF",
            AppLanguage.ENGLISH to "FREE TRIAL ACTIVE",
            AppLanguage.JAPANESE to "無料トライアル有効中",
            AppLanguage.CHINESE to "免费试用进行中"
        ),
        "trial_expired" to mapOf(
            AppLanguage.MALAY to "TEMPOH PERCUBAAN TAMAT",
            AppLanguage.ENGLISH to "TRIAL EXPIRED",
            AppLanguage.JAPANESE to "トライアル期間終了",
            AppLanguage.CHINESE to "试用期已结束"
        ),
        "subscribe_now" to mapOf(
            AppLanguage.MALAY to "Langgan Sekarang",
            AppLanguage.ENGLISH to "Subscribe Now",
            AppLanguage.JAPANESE to "今すぐサブスク登録",
            AppLanguage.CHINESE to "立即订阅"
        ),
        "pricing_header" to mapOf(
            AppLanguage.MALAY to "Pakej Langganan Bulanan (Diuruskan oleh Admin)",
            AppLanguage.ENGLISH to "Monthly Subscription Plans (Managed by Admin)",
            AppLanguage.JAPANESE to "月額サブスクリプションプラン（管理者承認制）",
            AppLanguage.CHINESE to "月度订阅套餐（由管理员统一管理）"
        ),
        "pay_online_my" to mapOf(
            AppLanguage.MALAY to "Bayar Dalam Talian (ToyyibPay / FPX Malaysia)",
            AppLanguage.ENGLISH to "Pay Online (ToyyibPay / FPX Malaysia)",
            AppLanguage.JAPANESE to "オンライン決済 (ToyyibPay / FPX)",
            AppLanguage.CHINESE to "在线支付 (ToyyibPay / FPX 马来西亚)"
        ),
        "pay_international" to mapOf(
            AppLanguage.MALAY to "Pengguna Antarabangsa (PayPal / Stripe / WhatsApp)",
            AppLanguage.ENGLISH to "International Users (PayPal / Stripe / WhatsApp)",
            AppLanguage.JAPANESE to "海外ユーザー決済 (PayPal / Stripe / WhatsApp)",
            AppLanguage.CHINESE to "国际用户支付 (PayPal / Stripe / WhatsApp)"
        ),
        "contact_admin_whatsapp" to mapOf(
            AppLanguage.MALAY to "Hubungi Admin (+6019-5326399) — Bantuan Langganan & Ralat",
            AppLanguage.ENGLISH to "Contact Admin (+6019-5326399) — Subscription & Error Help",
            AppLanguage.JAPANESE to "管理者に連絡 (+6019-5326399) — サブスク・エラー対応",
            AppLanguage.CHINESE to "联系管理员 (+6019-5326399) — 订阅与故障协助"
        ),
        "contact_admin_desc" to mapOf(
            AppLanguage.MALAY to "Langganan diatur oleh Admin. Jika terdapat sebarang ralat, kekeliruan atau masalah pengaktifan, tekan butang di bawah untuk terus ke WhatsApp Admin (+6019-5326399).",
            AppLanguage.ENGLISH to "Subscriptions are managed by Admin. If you encounter any error, confusion, or activation issue, tap below to message Admin directly on WhatsApp (+6019-5326399).",
            AppLanguage.JAPANESE to "サブスクリプションは管理者が設定します。エラーや不明点がある場合は、下のボタンからWhatsApp (+6019-5326399) で管理者に直接ご連絡ください。",
            AppLanguage.CHINESE to "订阅由管理员统一安排。如有任何错误、疑问或激活问题，请点击下方按钮直接通过 WhatsApp (+6019-5326399) 联系管理员。"
        ),

        // Lock screen
        "system_locked" to mapOf(
            AppLanguage.MALAY to "SISTEM DIKUNCI PENTADBIR",
            AppLanguage.ENGLISH to "SYSTEM LOCKED BY ADMIN",
            AppLanguage.JAPANESE to "管理者によりシステム停止中",
            AppLanguage.CHINESE to "系统已被管理员锁定"
        ),
        "pending_approval" to mapOf(
            AppLanguage.MALAY to "MENUNGGU KELULUSAN ADMIN",
            AppLanguage.ENGLISH to "PENDING ADMIN APPROVAL",
            AppLanguage.JAPANESE to "管理者承認待ち",
            AppLanguage.CHINESE to "等待管理员批准"
        )
    )
}
