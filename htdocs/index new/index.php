<?php
/**
 * PSYCOTIMEXPRO - TAB 1: CORPORATE BUSINESS HOMEPAGE (RESTORED & UPGRADED)
 * Replaces the unwanted standalone ET System login screen on the root domain
 * and restores the complete corporate identity, logo, services, shop, ET training,
 * sports blog highlights, and sports management modules.
 */

require_once __DIR__ . '/config/database.php';

$ACTIVE_TAB = 'home';
$SEO_TITLE = 'PsycotimexPro — Sistem ET Video Finish, Sports Management, ET Training & Kedai Sukan';
$SEO_DESCRIPTION = 'Laman web korporat rasmi PsycotimexPro (www.psycotimexpro.my): Sewaan Sistem Electronic Timing (ET Video Finish) daripada perisian bangunan sendiri bermula RM450/hari, Kedai Peralatan Balapan & Padang, Portal ET Training APK & Sistem Pengurusan Kejohanan Sukan.';
$SEO_CANONICAL = SITE_URL . '/';

$slides = PsycoDB::getSlides();
$products = PsycoDB::getShopProducts(false);
$featuredProducts = array_slice($products, 0, 4);
$services = PsycoDB::getServices();
$articles = PsycoDB::getBlogArticles(['status' => 'published']);
$latestArticles = array_slice($articles, 0, 3);
$etSchedules = array_slice(PsycoDB::getEtSchedules(), -6);
$championships = PsycoDB::getChampionships();
$sysSettings = PsycoDB::getSettings();
$apkDownloadUrl = !empty($sysSettings['apk_download_url']) ? $sysSettings['apk_download_url'] : 'https://drive.google.com/drive/folders/1gp_qP2D5G446xX5EgsMuWvp6ziEQxTMT?usp=sharing';
$apkDriveUrl    = !empty($sysSettings['apk_drive_url']) ? $sysSettings['apk_drive_url'] : $apkDownloadUrl;
$apkGithubUrl   = !empty($sysSettings['apk_github_url']) ? $sysSettings['apk_github_url'] : '';
$businessHubAds = PsycoDB::readJson('business_hub_ads.json', []);
if (empty($businessHubAds)) {
    $businessHubAds = [
        [
            'id'           => 'bhub-nexa-2026',
            'partner_name' => 'NEXA SPORT VENTURES',
            'badge'        => 'RAKAN STRATEGIK UTAMA • JULAI 2026',
            'title'        => 'NEXA AKAN MULA BEROPERASI PADA JULAI 2026',
            'tagline'      => 'System Management • Event Organising • Apparel • Electronic Timing • Media & Publisiti (@NexaSportVentures)',
            'image'        => '/assets/images/nexa-banner.png',
            'logo'         => '/assets/images/nexa-logo.jpg',
            'target_url'   => '/contact/index.php',
            'cta_text'     => 'Hubungi Rakan Strategik NEXA',
        ]
    ];
    PsycoDB::writeJson('business_hub_ads.json', $businessHubAds);
}

require_once __DIR__ . '/includes/header.php';
?>

<main class="flex-1 space-y-16 pb-20">

  <!-- ==================== 1. CORPORATE HERO SECTION ==================== -->
  <section class="relative pt-6 px-4 sm:px-6 lg:px-8 max-w-7xl mx-auto">
    <div class="relative rounded-3xl psyco-hero-grid border border-[#C0C0C0]/25 p-6 sm:p-10 lg:p-12 overflow-hidden shadow-2xl">
      <div class="relative z-10 grid grid-cols-1 lg:grid-cols-12 gap-8 items-center">

        <!-- Left Column: Brand Crest, Tagline & Primary CTAs -->
        <div class="lg:col-span-7 space-y-5 text-center lg:text-left">
          <div class="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-[#D71920]/20 border border-[#D71920]/60 text-red-300 text-xs font-black tracking-wider uppercase">
            <span class="w-2 h-2 rounded-full bg-[#D71920] animate-ping"></span>
            <span>LAMAN KORPORAT RASMI • WWW.PSYCOTIMEXPRO.MY</span>
          </div>

          <div class="py-1 max-w-xl mx-auto lg:mx-0">
            <?php echo render_psyco_logo('full'); ?>
          </div>

          <h1 class="text-2xl sm:text-3xl lg:text-4xl font-black font-orbitron text-white leading-tight">
            Teknologi Masa Elektronik, <span class="text-[#D71920]">Pengurusan Kejohanan</span> & Ekosistem Sukan Digital
          </h1>

          <p class="text-sm sm:text-base text-[#E5E5E5] leading-relaxed max-w-2xl mx-auto lg:mx-0">
            Pusat sewaan sistem pengiraan masa elektronik (<strong class="text-white">Sistem ET Video Finish</strong> daripada perisian yang dibangunkan sendiri), kedai peralatan balapan & padang, aplikasi latihan pecutan <strong class="text-white">ET Training APK</strong>, portal berita sukan, serta sistem penganjuran kejohanan sukan bersepadu.
          </p>

          <!-- Primary CTA Buttons -->
          <div class="pt-2 flex flex-wrap items-center justify-center lg:justify-start gap-3">
            <a href="/services/index.php" class="px-6 py-3.5 rounded-xl psyco-btn-red font-bold text-xs sm:text-sm flex items-center gap-2">
              <i class="fa-solid fa-stopwatch"></i>
              <span>Sewa ET System (Bermula RM450)</span>
            </a>
            <a href="/kedai-produk/index.php" class="px-5 py-3.5 rounded-xl psyco-btn-silver font-bold text-xs sm:text-sm flex items-center gap-2">
              <i class="fa-solid fa-bag-shopping text-[#D71920]"></i>
              <span>Kedai Produk Sukan</span>
            </a>
            <a href="/sports-management/index.php" class="px-5 py-3.5 rounded-xl psyco-btn-silver font-bold text-xs sm:text-sm flex items-center gap-2">
              <i class="fa-solid fa-trophy text-[#D71920]"></i>
              <span>Pengurusan Sukan</span>
            </a>
            <a href="<?php echo e(whatsapp_url('Halo Admin NIA (+6016-5861530), saya ingin mendapatkan sebut harga pakej & perkhidmatan PsycotimexPro.')); ?>"
               target="_blank" rel="noopener noreferrer"
               class="px-5 py-3.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-bold text-xs sm:text-sm flex items-center gap-2 transition">
              <i class="fa-brands fa-whatsapp text-base"></i>
              <span>WhatsApp Admin NIA</span>
            </a>
          </div>
        </div>

        <!-- Right Column: Emblem Crest & Live Performance Metrics -->
        <div class="lg:col-span-5 flex flex-col items-center justify-center space-y-5">
          <div class="relative p-4 rounded-full bg-gradient-to-b from-[#232329] via-[#121215] to-[#0B0B0D] border border-[#C0C0C0]/40 shadow-[0_0_50px_rgba(215,25,32,0.28)]">
            <?php echo render_psyco_logo('icon', 'w-36 h-36 sm:w-44 sm:h-44'); ?>
          </div>

          <div class="grid grid-cols-2 gap-3 w-full max-w-md">
            <div class="p-3.5 rounded-2xl bg-[#171719]/90 border border-[#C0C0C0]/25 text-center">
              <span class="block font-orbitron text-xl font-black text-[#D71920]">1/1000s</span>
              <span class="text-[11px] text-[#C0C0C0] uppercase tracking-wider font-bold">Kejituan ET Video Finish</span>
            </div>
            <div class="p-3.5 rounded-2xl bg-[#171719]/90 border border-[#C0C0C0]/25 text-center">
              <span class="block font-orbitron text-xl font-black text-white">In-House OS</span>
              <span class="text-[11px] text-[#C0C0C0] uppercase tracking-wider font-bold">Perisian ET Sendiri</span>
            </div>
            <div class="p-3.5 rounded-2xl bg-[#171719]/90 border border-[#C0C0C0]/25 text-center">
              <span class="block font-orbitron text-xl font-black text-white">RM 450/hari</span>
              <span class="text-[11px] text-[#C0C0C0] uppercase tracking-wider font-bold">Pakej Time Trial ET</span>
            </div>
            <div class="p-3.5 rounded-2xl bg-[#171719]/90 border border-[#C0C0C0]/25 text-center">
              <span class="block font-orbitron text-xl font-black text-emerald-400">7 Modul</span>
              <span class="text-[11px] text-[#C0C0C0] uppercase tracking-wider font-bold">Platform Bersepadu</span>
            </div>
          </div>
        </div>

      </div>
    </div>
  </section>

  <!-- ==================== 2. QUICK LINKS TO ALL 7 MAIN WEBSITE SECTIONS ==================== -->
  <section class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
    <div class="flex flex-col sm:flex-row sm:items-end justify-between gap-3 mb-6">
      <div>
        <span class="text-xs font-black tracking-widest uppercase text-[#D71920]">AKSES PANTAS 7 BAHAGIAN UTAMA</span>
        <h2 class="text-xl sm:text-2xl font-black font-orbitron text-white">Ekosistem Bersepadu PsycotimexPro</h2>
      </div>
      <p class="text-xs text-[#C0C0C0]">Pilih modul perniagaan atau portal teknikal di bawah:</p>
    </div>

    <div class="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-7 gap-3">
      <?php
      $sevenCards = [
          ['tab' => '01', 'title' => 'HOME', 'sub' => 'Info Korporat', 'url' => '/index.php', 'icon' => 'fa-house-chimney'],
          ['tab' => '02', 'title' => 'KEDAI PRODUK', 'sub' => 'Alatan Olahraga', 'url' => '/kedai-produk/index.php', 'icon' => 'fa-bag-shopping'],
          ['tab' => '03', 'title' => 'OUR SERVICES', 'sub' => 'Sewaan ET & Sukan', 'url' => '/services/index.php', 'icon' => 'fa-stopwatch'],
          ['tab' => '04', 'title' => 'CONTACT', 'sub' => 'Pertanyaan Rasmi', 'url' => '/contact/index.php', 'icon' => 'fa-headset'],
          ['tab' => '05', 'title' => 'ET TRAINING', 'sub' => 'APK & Rekod Latihan', 'url' => '/training-management/index.php', 'icon' => 'fa-person-running'],
          ['tab' => '06', 'title' => 'SPORTS BLOG', 'sub' => 'Portal Berita Sukan', 'url' => '/sports-blog/index.php', 'icon' => 'fa-newspaper'],
          ['tab' => '07', 'title' => 'SPORTS MGMT', 'sub' => 'Kejohanan & Urusetia', 'url' => '/sports-management/index.php', 'icon' => 'fa-trophy']
      ];
      foreach ($sevenCards as $sc):
      ?>
        <a href="<?php echo e($sc['url']); ?>" class="psyco-card psyco-card-interactive rounded-2xl p-4 flex flex-col justify-between group">
          <div class="flex items-center justify-between mb-3">
            <span class="text-[10px] font-mono font-bold text-[#C0C0C0]">TAB <?php echo e($sc['tab']); ?></span>
            <span class="w-8 h-8 rounded-lg bg-[#D71920]/15 border border-[#D71920]/40 flex items-center justify-center text-[#D71920] group-hover:bg-[#D71920] group-hover:text-white transition">
              <i class="fa-solid <?php echo e($sc['icon']); ?> text-xs"></i>
            </span>
          </div>
          <div>
            <div class="font-orbitron font-bold text-xs text-white group-hover:text-[#D71920] transition"><?php echo e($sc['title']); ?></div>
            <div class="text-[11px] text-[#C0C0C0] mt-0.5"><?php echo e($sc['sub']); ?></div>
          </div>
        </a>
      <?php endforeach; ?>
    </div>
  </section>

  <!-- ==================== 3. INTERACTIVE HERO PROMOTIONAL CAROUSEL (RUANGAN IKLAN & SOROTAN HOME) ==================== -->
  <section class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
    <div class="flex flex-wrap items-center justify-between gap-3 mb-4">
      <div>
        <h2 class="text-xl sm:text-2xl font-black font-orbitron text-white flex items-center gap-2">
          <i class="fa-solid fa-fire text-[#D71920]"></i>
          <span>SOROTAN, IKLAN &amp; PROMOSI TERKINI</span>
        </h2>
        <p class="text-xs sm:text-sm text-[#C0C0C0]">Pakej sewaan timing, kelengkapan sukan, berita semasa dan pengumuman kejohanan.</p>
      </div>
      <?php if (PsycoAuth::hasRole(['superadmin', 'admin', 'editor'])): ?>
        <div class="flex flex-wrap items-center gap-2">
          <button type="button" onclick="openHomeSlideModal()" class="px-3.5 py-2 rounded-xl psyco-btn-red text-xs font-bold flex items-center gap-1.5 shadow">
            <i class="fa-solid fa-plus"></i>
            <span>Tambah Iklan / Slide Home</span>
          </button>
          <a href="/admin/index.php?module=website" class="px-3.5 py-2 rounded-xl psyco-btn-silver text-xs font-bold flex items-center gap-1.5">
            <i class="fa-solid fa-sliders text-[#D71920]"></i>
            <span>Urus Semua di Dashboard</span>
          </a>
        </div>
      <?php endif; ?>
    </div>

    <div id="psyco-home-carousel" class="relative rounded-3xl overflow-hidden border border-[#C0C0C0]/25 bg-[#171719] shadow-2xl h-[420px] sm:h-[460px]">
      <?php foreach ($slides as $idx => $slide):
          $slImg    = $slide['imageUrl'] ?? ($slide['image'] ?? 'https://images.unsplash.com/photo-1461896836934-ffe607ba8211?q=80&w=1600&auto=format&fit=crop');
          $slTarget = $slide['targetUrl'] ?? ($slide['cta_link'] ?? '/services/index.php');
          $slCta    = $slide['ctaText'] ?? ($slide['cta_text'] ?? 'Terokai Sekarang');
          $slDesc   = $slide['description'] ?? ($slide['subtitle'] ?? '');
      ?>
        <div class="psyco-carousel-slide absolute inset-0 transition-opacity duration-700 <?php echo $idx === 0 ? 'opacity-100 z-10' : 'opacity-0 z-0 pointer-events-none'; ?>" data-slide-index="<?php echo $idx; ?>">
          <div class="absolute inset-0 bg-cover bg-center" style="background-image: url('<?php echo e($slImg); ?>');">
            <div class="absolute inset-0 bg-gradient-to-r from-[#0B0B0D] via-[#0B0B0D]/85 to-[#0B0B0D]/45"></div>
          </div>
          <div class="relative h-full max-w-4xl mx-auto px-6 sm:px-12 flex flex-col justify-center space-y-4">
            <div class="flex flex-wrap items-center justify-between gap-2">
              <div class="flex flex-wrap items-center gap-2">
                <span class="px-3 py-1 rounded-full text-[11px] font-black tracking-widest uppercase bg-[#D71920] text-white">
                  <?php echo e($slide['badge'] ?? 'INFO RASMI'); ?>
                </span>
                <?php if (!empty($slide['highlightTag'])): ?>
                  <span class="px-3 py-1 rounded-full text-[11px] font-bold bg-[#171719]/90 text-[#E5E5E5] border border-[#C0C0C0]/35">
                    <?php echo e($slide['highlightTag']); ?>
                  </span>
                <?php endif; ?>
              </div>

              <?php if (PsycoAuth::hasRole(['superadmin', 'admin', 'editor'])): ?>
                <div class="flex items-center gap-2">
                  <button type="button"
                          onclick='openEditHomeSlideModal(<?php echo json_encode([
                              "id" => $slide["id"] ?? ("slide-" . $idx),
                              "badge" => $slide["badge"] ?? "",
                              "highlightTag" => $slide["highlightTag"] ?? "",
                              "title" => $slide["title"] ?? "",
                              "subtitle" => $slide["subtitle"] ?? "",
                              "description" => $slDesc,
                              "imageUrl" => $slImg,
                              "ctaText" => $slCta,
                              "targetUrl" => $slTarget
                          ], JSON_HEX_APOS | JSON_HEX_QUOT); ?>)'
                          class="px-3 py-1.5 rounded-xl bg-amber-500/90 hover:bg-amber-500 text-black font-extrabold text-[11px] flex items-center gap-1.5 shadow-lg">
                    <i class="fa-solid fa-pen-to-square"></i>
                    <span>Edit Gambar &amp; Detail Iklan Ini</span>
                  </button>
                  <button type="button"
                          onclick="deleteHomeSlideDirect('<?php echo e($slide['id'] ?? ''); ?>')"
                          class="px-2.5 py-1.5 rounded-xl bg-red-600/90 hover:bg-red-600 text-white font-bold text-[11px] shadow-lg"
                          title="Padam Slide Ini">
                    <i class="fa-solid fa-trash"></i>
                  </button>
                </div>
              <?php endif; ?>
            </div>

            <h3 class="text-2xl sm:text-4xl font-black font-orbitron text-white leading-tight">
              <?php echo e($slide['title'] ?? ''); ?>
            </h3>
            <p class="text-sm sm:text-lg font-bold text-[#D71920]">
              <?php echo e($slide['subtitle'] ?? ''); ?>
            </p>
            <p class="text-xs sm:text-sm text-[#E5E5E5] max-w-2xl leading-relaxed">
              <?php echo e($slDesc); ?>
            </p>
            <div class="pt-2 flex flex-wrap items-center gap-3">
              <a href="<?php echo e($slTarget); ?>" class="px-6 py-3 rounded-xl psyco-btn-red font-bold text-xs sm:text-sm flex items-center gap-2">
                <span><?php echo e($slCta); ?></span>
                <i class="fa-solid fa-arrow-right"></i>
              </a>
              <a href="<?php echo e(whatsapp_url('Halo Admin NIA, saya berminat dengan promosi: ' . ($slide['title'] ?? ''))); ?>"
                 target="_blank" rel="noopener noreferrer"
                 class="px-5 py-3 rounded-xl psyco-btn-silver font-bold text-xs sm:text-sm flex items-center gap-2">
                <i class="fa-brands fa-whatsapp text-emerald-400"></i>
                <span>Tanya Admin NIA</span>
              </a>
            </div>
          </div>
        </div>
      <?php endforeach; ?>

      <!-- Carousel Controls -->
      <button type="button" onclick="psycoMoveSlide(-1)" aria-label="Slide Sebelumnya" class="absolute left-4 top-1/2 -translate-y-1/2 z-20 w-10 h-10 rounded-full bg-black/70 hover:bg-[#D71920] text-white border border-[#C0C0C0]/30 flex items-center justify-center transition">
        <i class="fa-solid fa-chevron-left"></i>
      </button>
      <button type="button" onclick="psycoMoveSlide(1)" aria-label="Slide Seterusnya" class="absolute right-4 top-1/2 -translate-y-1/2 z-20 w-10 h-10 rounded-full bg-black/70 hover:bg-[#D71920] text-white border border-[#C0C0C0]/30 flex items-center justify-center transition">
        <i class="fa-solid fa-chevron-right"></i>
      </button>
    </div>
  </section>

  <!-- ==================== 4. BUSINESS SERVICES & 4 CORE PILLARS ==================== -->
  <section class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 psyco-defer-section">
    <div class="text-center max-w-3xl mx-auto space-y-2 mb-10">
      <span class="text-xs font-black tracking-widest uppercase text-[#D71920]">PERKHIDMATAN KORPORAT & TEKNIKAL</span>
      <h2 class="text-2xl sm:text-3xl font-black font-orbitron text-white">Solusi Lengkap Sukan, Timing & Merchandise</h2>
      <p class="text-xs sm:text-sm text-[#C0C0C0]">
        Segala keperluan penganjuran kejohanan olahraga, peralatan kepegawaian, tempahan custom pakaian atlet sehingga sistem latihan digital bersepadu.
      </p>
    </div>

    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
      <!-- Pillar 1: ET Rental -->
      <div class="psyco-card psyco-card-interactive rounded-2xl p-6 flex flex-col justify-between">
        <div class="space-y-4">
          <div class="w-12 h-12 rounded-xl bg-[#D71920]/20 border border-[#D71920]/60 flex items-center justify-center text-[#D71920]">
            <i class="fa-solid fa-stopwatch text-xl"></i>
          </div>
          <span class="inline-block px-2.5 py-0.5 rounded-full psyco-badge-red text-[10px] font-black uppercase">Bermula RM450/hari</span>
          <h3 class="text-lg font-bold font-orbitron text-white">ET Rental System</h3>
          <p class="text-xs text-[#C0C0C0] leading-relaxed">
            Pakej ET Video Finish untuk Time Trial, Pakej Sukan Sekolah & MSSD fleksibel, dan Pakej Professional menggunakan perisian ET Video Finish yang dibangunkan sendiri.
          </p>
          <ul class="text-xs text-[#E5E5E5] space-y-1.5 pt-1">
            <li class="flex items-center gap-2"><i class="fa-solid fa-circle-check text-[#D71920]"></i><span>ET Video Finish & Pistol Pelepas</span></li>
            <li class="flex items-center gap-2"><i class="fa-solid fa-circle-check text-[#D71920]"></i><span>Keputusan terus ke meja teknikal</span></li>
            <li class="flex items-center gap-2"><i class="fa-solid fa-circle-check text-[#D71920]"></i><span>Perisian ET Bangunan Sendiri</span></li>
          </ul>
        </div>
        <div class="pt-6">
          <a href="/services/index.php#et-packages" class="w-full py-2.5 rounded-xl psyco-btn-red font-bold text-xs flex items-center justify-center gap-2">
            <span>Lihat Pakej & Tempah</span>
            <i class="fa-solid fa-arrow-right"></i>
          </a>
        </div>
      </div>

      <!-- Pillar 2: Kedai Produk & Peralatan Olahraga -->
      <div class="psyco-card psyco-card-interactive rounded-2xl p-6 flex flex-col justify-between">
        <div class="space-y-4">
          <div class="w-12 h-12 rounded-xl bg-[#171719] border border-[#C0C0C0]/40 flex items-center justify-center text-[#D71920]">
            <i class="fa-solid fa-bag-shopping text-xl"></i>
          </div>
          <span class="inline-block px-2.5 py-0.5 rounded-full psyco-badge-silver text-[10px] font-black uppercase">Katalog Rasmi Sukan</span>
          <h3 class="text-lg font-bold font-orbitron text-white">Kedai Produk Sukan</h3>
          <p class="text-xs text-[#C0C0C0] leading-relaxed">
            Pembekalan peralatan rasmi balapan & padang: Starting Block Aluminium, Sensor Pelepas Elektronik, Stopwatch Seiko, Kasut Spikes Karbon & alatan kepegawaian.
          </p>
          <ul class="text-xs text-[#E5E5E5] space-y-1.5 pt-1">
            <li class="flex items-center gap-2"><i class="fa-solid fa-circle-check text-[#D71920]"></i><span>Peralatan kejohanan berkualiti tinggi</span></li>
            <li class="flex items-center gap-2"><i class="fa-solid fa-circle-check text-[#D71920]"></i><span>Sokongan pesanan sekolah & LO</span></li>
            <li class="flex items-center gap-2"><i class="fa-solid fa-circle-check text-[#D71920]"></i><span>Penghantaran seluruh Malaysia</span></li>
          </ul>
        </div>
        <div class="pt-6">
          <a href="/kedai-produk/index.php" class="w-full py-2.5 rounded-xl psyco-btn-silver font-bold text-xs flex items-center justify-center gap-2">
            <span>Buka Kedai Produk</span>
            <i class="fa-solid fa-arrow-right text-[#D71920]"></i>
          </a>
        </div>
      </div>

      <!-- Pillar 3: Sports Management -->
      <div class="psyco-card psyco-card-interactive rounded-2xl p-6 flex flex-col justify-between">
        <div class="space-y-4">
          <div class="w-12 h-12 rounded-xl bg-[#D71920]/20 border border-[#D71920]/60 flex items-center justify-center text-[#D71920]">
            <i class="fa-solid fa-trophy text-xl"></i>
          </div>
          <span class="inline-block px-2.5 py-0.5 rounded-full psyco-badge-red text-[10px] font-black uppercase">Urusetia & Kepegawaian</span>
          <h3 class="text-lg font-bold font-orbitron text-white">Pengurusan Sukan</h3>
          <p class="text-xs text-[#C0C0C0] leading-relaxed">
            Sistem pendaftaran atlet & kelab, susunan saringan & lorong (heats), penjanaan Call Sheet rasmi, Kad Balapan/Padang, keputusan & E-Sijil.
          </p>
          <ul class="text-xs text-[#E5E5E5] space-y-1.5 pt-1">
            <li class="flex items-center gap-2"><i class="fa-solid fa-circle-check text-[#D71920]"></i><span>Pendaftaran atlet, kelab & kontinjen</span></li>
            <li class="flex items-center gap-2"><i class="fa-solid fa-circle-check text-[#D71920]"></i><span>Cetak Call Sheet & Kad Hakim</span></li>
            <li class="flex items-center gap-2"><i class="fa-solid fa-circle-check text-[#D71920]"></i><span>Keputusan live & jadual pingat</span></li>
          </ul>
        </div>
        <div class="pt-6">
          <a href="/sports-management/index.php" class="w-full py-2.5 rounded-xl psyco-btn-silver font-bold text-xs flex items-center justify-center gap-2">
            <span>Urus Kejohanan Sukan</span>
            <i class="fa-solid fa-arrow-right text-[#D71920]"></i>
          </a>
        </div>
      </div>

      <!-- Pillar 4: ET Training Portal -->
      <div class="psyco-card psyco-card-interactive rounded-2xl p-6 flex flex-col justify-between relative overflow-hidden">
        <div class="absolute top-0 right-0 px-3 py-1 bg-[#D71920] text-white font-black text-[9px] uppercase tracking-wider rounded-bl-xl">
          MODUL LATIHAN
        </div>
        <div class="space-y-4">
          <div class="w-12 h-12 rounded-xl bg-[#171719] border border-[#C0C0C0]/40 flex items-center justify-center text-[#D71920]">
            <i class="fa-solid fa-person-running text-xl"></i>
          </div>
          <span class="inline-block px-2.5 py-0.5 rounded-full psyco-badge-silver text-[10px] font-black uppercase">APK & Laporan Peribadi</span>
          <h3 class="text-lg font-bold font-orbitron text-white">ET Training System</h3>
          <p class="text-xs text-[#C0C0C0] leading-relaxed">
            Portal khusus untuk pemantauan sesi latihan kelajuan atlet, log jurulatih, analisis biomekanik pecutan dan muat turun laporan peribadi atlet.
          </p>
          <ul class="text-xs text-[#E5E5E5] space-y-1.5 pt-1">
            <li class="flex items-center gap-2"><i class="fa-solid fa-circle-check text-[#D71920]"></i><span>Muat turun aplikasi Android (.APK)</span></li>
            <li class="flex items-center gap-2"><i class="fa-solid fa-circle-check text-[#D71920]"></i><span>Dashboard peribadi atlet & jurulatih</span></li>
            <li class="flex items-center gap-2"><i class="fa-solid fa-circle-check text-[#D71920]"></i><span>Muat turun laporan prestasi sulit</span></li>
          </ul>
        </div>
        <div class="pt-6">
          <a href="/training-management/index.php" class="w-full py-2.5 rounded-xl psyco-btn-red font-bold text-xs flex items-center justify-center gap-2">
            <span>Buka Portal ET Training</span>
            <i class="fa-solid fa-arrow-right"></i>
          </a>
        </div>
      </div>
    </div>
  </section>

  <!-- ==================== 5. FEATURED PRODUCTS SHOWCASE (KEDAI PRODUK) ==================== -->
  <section class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 psyco-defer-section">
    <div class="rounded-3xl bg-[#121215] border border-[#C0C0C0]/20 p-6 sm:p-10 space-y-8">
      <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-zinc-800 pb-6">
        <div>
          <span class="text-xs font-black tracking-widest uppercase text-[#D71920]">PERALATAN TEKNIKAL & BALAPAN</span>
          <h2 class="text-2xl font-black font-orbitron text-white">Kedai Produk PsycotimexPro</h2>
          <p class="text-xs sm:text-sm text-[#C0C0C0]">Peralatan electronic timing, starting block aluminium, kasut spikes, baton dan aksesori kepegawaian rasmi.</p>
        </div>
        <a href="/kedai-produk/index.php" class="px-5 py-2.5 rounded-xl psyco-btn-silver font-bold text-xs flex items-center gap-2 self-start sm:self-auto">
          <span>Lihat Semua Produk (<?php echo count($products); ?> Item)</span>
          <i class="fa-solid fa-arrow-right text-[#D71920]"></i>
        </a>
      </div>

      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
        <?php foreach ($featuredProducts as $prod):
            $img = $prod['images'][0] ?? '/assets/images/logo.png';
        ?>
          <div class="psyco-card psyco-card-interactive rounded-2xl overflow-hidden flex flex-col justify-between group">
            <div>
              <div class="h-44 overflow-hidden relative bg-[#0B0B0D]">
                <img src="<?php echo e($img); ?>" alt="<?php echo e($prod['name']); ?>" loading="lazy" class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300">
                <span class="absolute top-2.5 left-2.5 px-2.5 py-0.5 rounded-full bg-black/80 text-[10px] font-bold text-[#E5E5E5] border border-[#C0C0C0]/30">
                  <?php echo e($prod['category']); ?>
                </span>
              </div>
              <div class="p-4 space-y-2">
                <h3 class="text-xs sm:text-sm font-bold text-white line-clamp-2 group-hover:text-[#D71920] transition">
                  <?php echo e($prod['name']); ?>
                </h3>
                <p class="text-[11px] text-[#C0C0C0] line-clamp-2">
                  <?php echo e($prod['description']); ?>
                </p>
              </div>
            </div>
            <div class="p-4 pt-0 flex items-center justify-between gap-2 border-t border-zinc-800/80 mt-2 pt-3">
              <div>
                <span class="font-orbitron font-black text-sm text-[#D71920] block"><?php echo format_rm($prod['price']); ?></span>
                <?php if (!empty($prod['original_price']) && $prod['original_price'] > $prod['price']): ?>
                  <span class="text-[10px] text-zinc-500 line-through"><?php echo format_rm($prod['original_price']); ?></span>
                <?php endif; ?>
              </div>
              <button type="button"
                      onclick='PsycoApp.addToCart(<?php echo json_encode([
                          "id" => $prod["id"],
                          "name" => $prod["name"],
                          "price" => $prod["price"],
                          "image" => $img,
                          "category" => $prod["category"]
                      ], JSON_HEX_APOS | JSON_HEX_QUOT); ?>)'
                      class="px-3 py-2 rounded-xl psyco-btn-red text-[11px] font-bold flex items-center gap-1.5">
                <i class="fa-solid fa-cart-plus"></i>
                <span>Beli</span>
              </button>
            </div>
          </div>
        <?php endforeach; ?>
      </div>
    </div>
  </section>

  <!-- ==================== 6. SPORTS ACTIVITIES, ET SCHEDULE & CHAMPIONSHIPS ==================== -->
  <section class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 psyco-defer-section">
    <div class="grid grid-cols-1 lg:grid-cols-12 gap-8">

      <!-- Left 7 Cols: Recent & Upcoming Official ET Rental Schedules -->
      <div class="lg:col-span-7 psyco-card rounded-3xl p-6 sm:p-8 space-y-5">
        <div class="flex flex-wrap items-center justify-between gap-3 border-b border-zinc-800 pb-4">
          <div>
            <span class="text-[11px] font-black uppercase tracking-widest text-[#D71920]">REKOD OPERASI & KALENDAR RASMI</span>
            <h3 class="text-xl font-black font-orbitron text-white">Jadual Kejohanan & Sewaan ET 2026</h3>
          </div>
          <a href="/et-schedule/" class="px-3.5 py-2 rounded-xl psyco-btn-silver text-xs font-bold flex items-center gap-1.5">
            <i class="fa-solid fa-calendar-days text-[#D71920]"></i>
            <span>Lihat Semua 17+ Jadual</span>
          </a>
        </div>

        <div class="overflow-x-auto">
          <table class="w-full text-left text-xs">
            <thead class="text-[#C0C0C0] uppercase border-b border-zinc-800">
              <tr>
                <th class="py-2.5 pr-3">Kejohanan / Penganjur</th>
                <th class="py-2.5 px-3">Lokasi Stadium</th>
                <th class="py-2.5 px-3">Tarikh</th>
                <th class="py-2.5 pl-3 text-right">Status</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-zinc-800/70">
              <?php foreach ($etSchedules as $sch): ?>
                <tr class="hover:bg-zinc-900/50">
                  <td class="py-3 pr-3">
                    <div class="font-bold text-white"><?php echo e($sch['title']); ?></div>
                    <div class="text-[11px] text-[#C0C0C0]"><?php echo e($sch['package']); ?></div>
                  </td>
                  <td class="py-3 px-3 text-[#E5E5E5]"><?php echo e($sch['venue']); ?></td>
                  <td class="py-3 px-3 font-mono text-[11px] text-[#C0C0C0]"><?php echo e($sch['startDate']); ?></td>
                  <td class="py-3 pl-3 text-right">
                    <span class="px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-950/80 text-emerald-300 border border-emerald-700/60">
                      <?php echo e($sch['status']); ?>
                    </span>
                  </td>
                </tr>
              <?php endforeach; ?>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Right 5 Cols: Active Championships & Quick Registration -->
      <div class="lg:col-span-5 psyco-card rounded-3xl p-6 sm:p-8 flex flex-col justify-between space-y-5">
        <div class="space-y-4">
          <div class="flex items-center justify-between border-b border-zinc-800 pb-4">
            <div>
              <span class="text-[11px] font-black uppercase tracking-widest text-[#D71920]">PORTAL KEJOHANAN SUKAN</span>
              <h3 class="text-xl font-black font-orbitron text-white">Pendaftaran Kejohanan</h3>
            </div>
            <a href="/sports-management/index.php" class="text-xs font-bold text-[#D71920] hover:underline">Buka Portal &rarr;</a>
          </div>

          <div class="space-y-3">
            <?php foreach ($championships as $ch): ?>
              <div class="p-4 rounded-2xl bg-[#0B0B0D] border border-zinc-800 hover:border-[#D71920]/50 transition space-y-2">
                <div class="flex items-center justify-between gap-2">
                  <span class="text-[10px] font-mono font-bold text-[#C0C0C0]"><?php echo e($ch['championship_code']); ?></span>
                  <span class="px-2 py-0.5 rounded-full text-[10px] font-black <?php echo ($ch['status'] === 'OPEN') ? 'bg-[#D71920] text-white' : 'bg-zinc-800 text-[#C0C0C0]'; ?>">
                    <?php echo e($ch['status']); ?>
                  </span>
                </div>
                <div class="font-bold text-xs sm:text-sm text-white"><?php echo e($ch['name']); ?></div>
                <div class="text-[11px] text-[#C0C0C0] flex flex-wrap gap-3">
                  <span><i class="fa-solid fa-location-dot text-[#D71920] mr-1"></i><?php echo e($ch['venue']); ?></span>
                  <span><i class="fa-solid fa-calendar text-[#D71920] mr-1"></i><?php echo e($ch['date_start']); ?></span>
                </div>
              </div>
            <?php endforeach; ?>
          </div>
        </div>

        <div class="grid grid-cols-2 gap-3 pt-2">
          <a href="/sports-management/daftar.html" class="py-3 px-4 rounded-xl psyco-btn-red text-xs font-bold text-center flex items-center justify-center gap-1.5">
            <i class="fa-solid fa-user-plus"></i>
            <span>Daftar Atlet / Kelab</span>
          </a>
          <a href="/sports-management/result.html" class="py-3 px-4 rounded-xl psyco-btn-silver text-xs font-bold text-center flex items-center justify-center gap-1.5">
            <i class="fa-solid fa-ranking-star text-[#D71920]"></i>
            <span>Keputusan Rasmi</span>
          </a>
        </div>
      </div>

    </div>
  </section>

  <!-- ==================== 7. LATEST SPORTS BLOG & NEWS HIGHLIGHTS ==================== -->
  <section class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 psyco-defer-section">
    <div class="flex flex-col sm:flex-row sm:items-end justify-between gap-4 mb-6">
      <div>
        <span class="text-xs font-black tracking-widest uppercase text-[#D71920]">PORTAL BERITA & LIPUTAN SUKAN</span>
        <h2 class="text-2xl sm:text-3xl font-black font-orbitron text-white">Berita & Artikel Sukan Terkini</h2>
      </div>
      <a href="/sports-blog/index.php" class="px-5 py-2.5 rounded-xl psyco-btn-silver text-xs font-bold flex items-center gap-2 self-start sm:self-auto">
        <span>Buka Portal Sports Blog</span>
        <i class="fa-solid fa-arrow-right text-[#D71920]"></i>
      </a>
    </div>

    <?php if (empty($latestArticles)): ?>
      <div class="rounded-3xl border border-zinc-800 bg-[#121215] p-8 sm:p-10 text-center space-y-3">
        <div class="text-2xl text-[#D71920]"><i class="fa-regular fa-newspaper"></i></div>
        <h3 class="font-orbitron text-base font-bold text-white">Liputan Berita Sukan Akan Datang</h3>
        <p class="text-xs text-[#C0C0C0] max-w-lg mx-auto">Berita kejohanan olahraga, keputusan rasmi dan artikel sains sukan akan diterbitkan di ruangan ini.</p>
        <?php if (PsycoAuth::canManageBlog()): ?>
          <div class="pt-2">
            <a href="/sports-blog/index.php" class="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl psyco-btn-red text-xs font-bold">
              <i class="fa-solid fa-pen-nib"></i>
              <span>Tulis Berita Pertama (Admin / Author)</span>
            </a>
          </div>
        <?php endif; ?>
      </div>
    <?php else: ?>
      <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
        <?php foreach ($latestArticles as $art):
            $artCat = $art['category_name'] ?? ($art['category'] ?? 'Sports News');
            $artRead = (int)($art['reading_time_min'] ?? ($art['reading_time'] ?? 4));
        ?>
          <article class="psyco-card psyco-card-interactive rounded-2xl overflow-hidden flex flex-col justify-between group">
            <div>
              <a href="/sports-blog/article.php?slug=<?php echo urlencode($art['slug'] ?? ''); ?>" class="block h-48 overflow-hidden relative bg-[#0B0B0D]">
                <img src="<?php echo e($art['featured_image'] ?? '/assets/images/logo.png'); ?>" alt="<?php echo e($art['image_alt'] ?? ($art['title'] ?? '')); ?>" loading="lazy" class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300">
                <span class="absolute top-3 left-3 px-2.5 py-1 rounded-full bg-[#D71920] text-white text-[10px] font-black uppercase tracking-wider">
                  <?php echo e($artCat); ?>
                </span>
              </a>
              <div class="p-5 space-y-2.5">
                <div class="flex items-center gap-3 text-[11px] text-[#C0C0C0]">
                  <span><i class="fa-regular fa-calendar mr-1 text-[#D71920]"></i><?php echo e(substr($art['published_at'] ?? '', 0, 10)); ?></span>
                  <span><i class="fa-regular fa-user mr-1 text-[#D71920]"></i><?php echo e($art['author_name'] ?? 'Meja Sukan'); ?></span>
                </div>
                <h3 class="text-sm sm:text-base font-bold text-white line-clamp-2 group-hover:text-[#D71920] transition">
                  <a href="/sports-blog/article.php?slug=<?php echo urlencode($art['slug'] ?? ''); ?>"><?php echo e($art['title'] ?? ''); ?></a>
                </h3>
                <p class="text-xs text-[#C0C0C0] line-clamp-3 leading-relaxed">
                  <?php echo e($art['excerpt'] ?? ''); ?>
                </p>
              </div>
            </div>
            <div class="px-5 pb-5 pt-2 flex items-center justify-between text-xs border-t border-zinc-800/70">
              <a href="/sports-blog/article.php?slug=<?php echo urlencode($art['slug'] ?? ''); ?>" class="font-bold text-[#D71920] hover:underline flex items-center gap-1.5">
                <span>Baca Artikel Penuh</span>
                <i class="fa-solid fa-arrow-right text-[10px]"></i>
              </a>
              <span class="text-[11px] text-zinc-500"><?php echo $artRead; ?> min bacaan</span>
            </div>
          </article>
        <?php endforeach; ?>
      </div>
    <?php endif; ?>
  </section>

  <!-- ==================== 8. APK & ET TRAINING COMPARISON SECTION ==================== -->
  <section class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 psyco-defer-section">
    <div class="rounded-3xl bg-gradient-to-b from-[#171719] to-[#0B0B0D] border border-[#D71920]/40 p-6 sm:p-10 shadow-2xl space-y-6">
      <div class="flex flex-col lg:flex-row lg:items-center justify-between gap-6 border-b border-zinc-800 pb-6">
        <div class="space-y-2">
          <span class="inline-flex items-center gap-2 px-3 py-1 rounded-full psyco-badge-red text-xs font-black uppercase">
            <i class="fa-brands fa-android"></i> APLIKASI ANDROID (APK) & WEB PORTAL LIVE
          </span>
          <h2 class="text-2xl sm:text-3xl font-black font-orbitron text-white">
            Sistem Latihan Pintar: PsycotimeX ET Training Management
          </h2>
          <p class="text-xs sm:text-sm text-[#E5E5E5] max-w-2xl">
            Pantau masa larian dengan Kamera Electronic Timing (ET), analisis hayunan biomekanik pecutan, rekod latihan individu dan muat turun laporan prestasi peribadi atlet secara selamat.
          </p>
        </div>

        <div class="flex flex-wrap items-center gap-3 shrink-0">
          <a href="<?= e($apkDriveUrl) ?>" target="_blank" rel="noopener noreferrer" class="px-5 py-3 rounded-xl psyco-btn-red font-bold text-xs flex items-center gap-2 shadow-lg hover:shadow-red-900/30 transition">
            <i class="fa-brands fa-google-drive text-base"></i>
            <span>Muat Turun APK (Google Drive)</span>
          </a>
          <?php if (!empty($apkGithubUrl)): ?>
          <a href="<?= e($apkGithubUrl) ?>" target="_blank" rel="noopener noreferrer" class="px-5 py-3 rounded-xl bg-zinc-800 hover:bg-zinc-700 text-white border border-zinc-700 font-bold text-xs flex items-center gap-2 transition">
            <i class="fa-brands fa-github text-base"></i>
            <span>Muat Turun APK (GitHub)</span>
          </a>
          <?php endif; ?>
          <a href="/training-management/index.php" class="px-5 py-3 rounded-xl psyco-btn-silver font-bold text-xs flex items-center gap-2">
            <i class="fa-solid fa-user-lock text-[#D71920]"></i>
            <span>Log Masuk & Laporan Peribadi</span>
          </a>
        </div>
      </div>

      <div class="overflow-x-auto rounded-2xl border border-zinc-800 bg-[#0B0B0D]">
        <table class="w-full text-left text-xs">
          <thead class="bg-[#171719] text-[#C0C0C0] uppercase font-bold border-b border-zinc-800">
            <tr>
              <th class="px-5 py-3.5">Modul & Keupayaan Sistem</th>
              <th class="px-5 py-3.5 text-center text-[#D71920]">Aplikasi Android (.APK)</th>
              <th class="px-5 py-3.5 text-center text-[#E5E5E5]">Portal Web Rasmi</th>
              <th class="px-5 py-3.5">Kelebihan Untuk Jurulatih & Atlet</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-zinc-800/80 text-[#E5E5E5]">
            <tr>
              <td class="px-5 py-3.5 font-bold text-white">Kamera Electronic Timing (Start Gun & Torso Gate)</td>
              <td class="px-5 py-3.5 text-center font-bold text-emerald-400">Tersedia (Kamera Telefon)</td>
              <td class="px-5 py-3.5 text-center text-[#C0C0C0]">Penyegerakan Awan</td>
              <td class="px-5 py-3.5 text-[11px] text-[#C0C0C0]">Merakam pelepasan dan garisan penamat dengan sensor visual telefon</td>
            </tr>
            <tr>
              <td class="px-5 py-3.5 font-bold text-white">Analisis Biomekanik Pergerakan Pecutan</td>
              <td class="px-5 py-3.5 text-center font-bold text-emerald-400">Tersedia (Cadence & GCT)</td>
              <td class="px-5 py-3.5 text-center font-bold text-emerald-400">Tersedia</td>
              <td class="px-5 py-3.5 text-[11px] text-[#C0C0C0]">Membaca Cadence (SPM), Ground Contact Time (ms) dan Torso Lean Angle</td>
            </tr>
            <tr>
              <td class="px-5 py-3.5 font-bold text-white">Muat Turun Laporan Prestasi Peribadi (Sulit)</td>
              <td class="px-5 py-3.5 text-center font-bold text-emerald-400">Akses Akaun Sendiri</td>
              <td class="px-5 py-3.5 text-center font-bold text-emerald-400">Cetakan & Muat Turun</td>
              <td class="px-5 py-3.5 text-[11px] text-[#C0C0C0]">Setiap pengguna hanya boleh mengakses rekod dan laporan peribadi masing-masing</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </section>

  <!-- ==================== 9. RAKAN STRATEGIK NEXA & PSYCOTIMEXPRO BUSINESS HUB (SLIDE CAROUSEL IKLAN BISNES) ==================== -->
  <section id="business-hub" class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 psyco-defer-section">
    <div class="rounded-3xl bg-gradient-to-br from-[#171719] via-[#121215] to-[#0B0B0D] border border-amber-500/35 p-6 sm:p-10 shadow-2xl space-y-6">
      <div class="flex flex-col lg:flex-row lg:items-center justify-between gap-4 border-b border-zinc-800 pb-5">
        <div class="flex flex-col sm:flex-row sm:items-center gap-4">
          <div class="h-16 w-16 shrink-0 rounded-2xl bg-white p-1.5 border border-amber-400/60 shadow-lg flex items-center justify-center">
            <img src="/assets/images/nexa-logo.jpg" alt="NEXA Sport Ventures" class="w-full h-full object-contain">
          </div>
          <div class="space-y-1">
            <div class="flex flex-wrap items-center gap-2">
              <span class="px-3 py-1 rounded-full bg-amber-500/20 border border-amber-400/50 text-amber-300 font-orbitron text-[10px] font-black uppercase tracking-wider">
                🤝 RAKAN STRATEGIK RASMI &amp; BUSINESS HUB
              </span>
              <span class="px-2.5 py-0.5 rounded-full bg-[#D71920]/20 border border-[#D71920]/50 text-red-300 text-[10px] font-bold uppercase">
                Powered by Psyco Time X Pro Sdn. Bhd.
              </span>
            </div>
            <h2 class="text-xl sm:text-2xl lg:text-3xl font-black font-orbitron text-white">
              PsycotimexPro Business Hub &amp; Rakan Strategik <span class="text-amber-400">NEXA Sport Ventures</span>
            </h2>
            <p class="text-xs sm:text-sm text-[#C0C0C0] max-w-3xl">
              Ruangan promosi perniagaan, perkongsian strategik sukan, pengurusan acara, pakaian sukan (Apparel), Electronic Timing serta media &amp; publisiti bersepadu.
            </p>
          </div>
        </div>

        <div class="flex flex-wrap items-center gap-2 shrink-0">
          <?php if (PsycoAuth::hasRole(['superadmin', 'admin', 'editor'])): ?>
            <button type="button" onclick="openBusinessHubModal()" class="px-4 py-2.5 rounded-xl psyco-btn-red text-xs font-bold flex items-center gap-1.5 shadow-lg">
              <i class="fa-solid fa-plus"></i>
              <span>Tambah Iklan Business Hub</span>
            </button>
          <?php endif; ?>
          <a href="<?php echo e(whatsapp_url('Halo Admin Psyco Time X Pro Sdn. Bhd., saya berminat untuk meletakkan iklan / kolaborasi di Ruangan PsycotimexPro Business Hub.')); ?>"
             target="_blank" rel="noopener noreferrer"
             class="px-4 py-2.5 rounded-xl psyco-btn-silver text-xs font-bold flex items-center gap-1.5">
            <i class="fa-solid fa-bullhorn text-amber-400"></i>
            <span>Promosi Bisnes Anda Di Sini</span>
          </a>
        </div>
      </div>

      <!-- Business Hub Banner Slide Carousel -->
      <div id="psyco-bhub-carousel" class="relative rounded-2xl overflow-hidden border border-white/15 bg-[#0B0B0D] shadow-2xl">
        <?php foreach ($businessHubAds as $bIdx => $ad): ?>
          <div class="psyco-bhub-slide transition-all duration-700 <?php echo $bIdx === 0 ? 'block opacity-100' : 'hidden opacity-0'; ?>" data-bhub-index="<?php echo $bIdx; ?>">
            <!-- Full Banner Artwork -->
            <div class="relative bg-[#0B0B0D]">
              <img src="<?php echo e($ad['image'] ?? '/assets/images/nexa-banner.png'); ?>"
                   alt="<?php echo e($ad['title'] ?? 'NEXA Sport Ventures'); ?>"
                   class="w-full max-h-[440px] object-contain mx-auto bg-black">

              <?php if (PsycoAuth::hasRole(['superadmin', 'admin', 'editor'])): ?>
                <div class="absolute top-3 right-3 z-20 flex items-center gap-2">
                  <button type="button"
                          onclick='openEditBusinessHubModal(<?php echo json_encode($ad, JSON_HEX_APOS | JSON_HEX_QUOT); ?>)'
                          class="px-3 py-1.5 rounded-xl bg-amber-500 hover:bg-amber-400 text-black font-extrabold text-[11px] flex items-center gap-1.5 shadow-lg">
                    <i class="fa-solid fa-pen-to-square"></i>
                    <span>Edit Iklan</span>
                  </button>
                  <?php if (count($businessHubAds) > 1): ?>
                    <button type="button"
                            onclick="deleteBusinessHubAd('<?php echo e($ad['id'] ?? ''); ?>')"
                            class="px-2.5 py-1.5 rounded-xl bg-red-600 hover:bg-red-500 text-white font-bold text-[11px] shadow-lg"
                            title="Padam Iklan Ini">
                      <i class="fa-solid fa-trash"></i>
                    </button>
                  <?php endif; ?>
                </div>
              <?php endif; ?>
            </div>

            <!-- Bottom Caption & Action Bar -->
            <div class="p-4 sm:p-6 bg-[#141418] border-t border-white/10 flex flex-col md:flex-row md:items-center justify-between gap-4">
              <div class="flex items-start sm:items-center gap-3.5">
                <img src="<?php echo e($ad['logo'] ?? '/assets/images/nexa-logo.jpg'); ?>" alt="Partner Logo" class="h-12 w-12 shrink-0 rounded-xl bg-white p-1 object-contain border border-amber-400/40">
                <div class="space-y-1">
                  <div class="flex flex-wrap items-center gap-2">
                    <span class="px-2.5 py-0.5 rounded bg-amber-500/20 border border-amber-400/40 text-amber-300 font-orbitron text-[10px] font-black uppercase">
                      <?php echo e($ad['badge'] ?? 'RAKAN STRATEGIK & BISNES HUB'); ?>
                    </span>
                    <span class="text-xs font-bold text-white"><?php echo e($ad['partner_name'] ?? 'NEXA SPORT VENTURES'); ?></span>
                  </div>
                  <h3 class="font-orbitron text-sm sm:text-lg font-black text-white"><?php echo e($ad['title'] ?? ''); ?></h3>
                  <p class="text-xs text-[#C0C0C0]"><?php echo e($ad['tagline'] ?? ''); ?></p>
                </div>
              </div>

              <div class="flex flex-wrap items-center gap-2.5 shrink-0">
                <a href="<?php echo e($ad['target_url'] ?? '/contact/index.php'); ?>" class="px-5 py-2.5 rounded-xl psyco-btn-red text-xs font-bold flex items-center gap-2">
                  <span><?php echo e($ad['cta_text'] ?? 'Info Lanjut'); ?></span>
                  <i class="fa-solid fa-arrow-right"></i>
                </a>
                <div class="flex items-center gap-1.5">
                  <button type="button" onclick="psycoMoveBhubSlide(-1)" class="w-9 h-9 rounded-xl bg-[#0B0B0D] hover:bg-[#D71920] text-white border border-white/15 flex items-center justify-center transition" title="Iklan Sebelumnya">
                    <i class="fa-solid fa-chevron-left text-xs"></i>
                  </button>
                  <span class="px-2.5 py-1 rounded-lg bg-[#0B0B0D] border border-white/10 font-mono text-[11px] font-bold text-[#E5E5E5]">
                    <?php echo ($bIdx + 1) . ' / ' . count($businessHubAds); ?>
                  </span>
                  <button type="button" onclick="psycoMoveBhubSlide(1)" class="w-9 h-9 rounded-xl bg-[#0B0B0D] hover:bg-[#D71920] text-white border border-white/15 flex items-center justify-center transition" title="Iklan Seterusnya">
                    <i class="fa-solid fa-chevron-right text-xs"></i>
                  </button>
                </div>
              </div>
            </div>
          </div>
        <?php endforeach; ?>
      </div>
    </div>
  </section>

</main>

<?php if (PsycoAuth::hasRole(['superadmin', 'admin', 'editor'])): ?>
<!-- DIALOG: ADMIN QUICK EDIT / ADD HOME SLIDE & AD BANNER -->
<dialog id="home-slide-edit-dialog" closedby="any" class="w-full max-w-2xl rounded-3xl border border-white/15 bg-[#171719] p-0 text-white shadow-2xl backdrop:bg-black/85">
  <div class="flex items-center justify-between border-b border-white/10 px-6 py-4">
    <h3 class="font-orbitron text-base font-bold text-white" id="hs-modal-title">Edit Gambar &amp; Detail Iklan / Slide Home</h3>
    <button type="button" onclick="document.getElementById('home-slide-edit-dialog').close()" class="flex h-8 w-8 items-center justify-center rounded-lg bg-[#0B0B0D] text-[#C0C0C0] hover:text-white">&times;</button>
  </div>
  <form onsubmit="handleSaveHomeSlideDirect(event)" class="p-6 space-y-4 text-xs max-h-[82vh] overflow-y-auto">
    <input type="hidden" id="hs-id" value="">

    <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
      <div>
        <label class="mb-1 block font-bold text-[#E5E5E5]">Label Lencana (Badge) *</label>
        <input type="text" id="hs-badge" required value="IN-HOUSE ET VIDEO FINISH" class="w-full rounded-xl border border-white/15 bg-[#0B0B0D] px-3.5 py-2.5 text-white">
      </div>
      <div>
        <label class="mb-1 block font-bold text-[#E5E5E5]">Tag Sorotan (Highlight Tag)</label>
        <input type="text" id="hs-tag" value="Promosi & Info Terkini" class="w-full rounded-xl border border-white/15 bg-[#0B0B0D] px-3.5 py-2.5 text-white">
      </div>
    </div>

    <div>
      <label class="mb-1 block font-bold text-[#E5E5E5]">Tajuk Utama Iklan / Slide *</label>
      <input type="text" id="hs-title" required placeholder="Tajuk utama iklan atau berita..." class="w-full rounded-xl border border-white/15 bg-[#0B0B0D] px-3.5 py-2.5 font-bold text-white">
    </div>

    <div>
      <label class="mb-1 block font-bold text-[#E5E5E5]">Sub-Tajuk (Teks Merah) *</label>
      <input type="text" id="hs-subtitle" required placeholder="Sub-tajuk promosi..." class="w-full rounded-xl border border-white/15 bg-[#0B0B0D] px-3.5 py-2.5 text-white">
    </div>

    <div>
      <label class="mb-1 block font-bold text-[#E5E5E5]">Penerangan / Info Detail *</label>
      <textarea id="hs-desc" rows="3" required placeholder="Masukkan butiran lanjut iklan atau info terkini..." class="w-full rounded-xl border border-white/15 bg-[#0B0B0D] px-3.5 py-2.5 text-white"></textarea>
    </div>

    <div class="rounded-2xl border border-white/10 bg-[#0B0B0D] p-3.5 space-y-2.5">
      <div class="flex flex-wrap items-center justify-between gap-2">
        <label class="font-bold text-[#E5E5E5]">Gambar Iklan / Latar Slide *</label>
        <label class="cursor-pointer rounded-lg bg-[#D71920]/20 border border-[#D71920]/50 px-3 py-1.5 text-[11px] font-bold text-[#D71920] hover:bg-[#D71920] hover:text-white transition">
          📸 Muat Naik Gambar Baharu (Watermark Automatik)
          <input type="file" accept="image/*,.png,.jpg,.jpeg,.webp,.gif,.bmp,.svg,.avif,.jfif,.heic,.img" onchange="uploadHomeSlideImgDirect(this.files)" class="hidden">
        </label>
      </div>
      <input type="text" id="hs-image" required placeholder="URL Gambar..." class="w-full rounded-xl border border-white/15 bg-[#171719] px-3.5 py-2 text-white font-mono text-[11px]">
    </div>

    <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
      <div>
        <label class="mb-1 block font-bold text-[#E5E5E5]">Teks Butang Pautan (CTA) *</label>
        <input type="text" id="hs-cta" required value="Lihat Pakej & Info" class="w-full rounded-xl border border-white/15 bg-[#0B0B0D] px-3.5 py-2.5 text-white">
      </div>
      <div>
        <label class="mb-1 block font-bold text-[#E5E5E5]">Pautan URL Apabila Diklik *</label>
        <input type="text" id="hs-url" required value="/services/index.php" class="w-full rounded-xl border border-white/15 bg-[#0B0B0D] px-3.5 py-2.5 font-mono text-white">
      </div>
    </div>

    <div class="flex justify-end gap-3 pt-2">
      <button type="button" onclick="document.getElementById('home-slide-edit-dialog').close()" class="psyco-btn psyco-btn-secondary px-5 py-2.5 text-xs">Batal</button>
      <button type="submit" class="psyco-btn psyco-btn-primary px-6 py-2.5 text-xs">💾 Simpan Perubahan Iklan Home</button>
    </div>
  </form>
</dialog>

<script>
function openHomeSlideModal() {
  const dlg = document.getElementById('home-slide-edit-dialog');
  if (!dlg) return;
  document.getElementById('hs-modal-title').textContent = '➕ Tambah Iklan / Slide Baharu di Home';
  document.getElementById('hs-id').value = '';
  document.getElementById('hs-badge').value = 'PROMOSI & INFO RASMI';
  document.getElementById('hs-tag').value = 'Terkini';
  document.getElementById('hs-title').value = '';
  document.getElementById('hs-subtitle').value = '';
  document.getElementById('hs-desc').value = '';
  document.getElementById('hs-image').value = 'https://images.unsplash.com/photo-1461896836934-ffe607ba8211?q=80&w=1600&auto=format&fit=crop';
  document.getElementById('hs-cta').value = 'Terokai Sekarang';
  document.getElementById('hs-url').value = '/services/index.php';
  dlg.showModal();
}

function openEditHomeSlideModal(sl) {
  const dlg = document.getElementById('home-slide-edit-dialog');
  if (!dlg) return;
  document.getElementById('hs-modal-title').textContent = '✏️ Edit Gambar, Detail & Pautan Iklan Home';
  document.getElementById('hs-id').value = sl.id || '';
  document.getElementById('hs-badge').value = sl.badge || '';
  document.getElementById('hs-tag').value = sl.highlightTag || '';
  document.getElementById('hs-title').value = sl.title || '';
  document.getElementById('hs-subtitle').value = sl.subtitle || '';
  document.getElementById('hs-desc').value = sl.description || '';
  document.getElementById('hs-image').value = sl.imageUrl || '';
  document.getElementById('hs-cta').value = sl.ctaText || 'Terokai Sekarang';
  document.getElementById('hs-url').value = sl.targetUrl || '/services/index.php';
  dlg.showModal();
}

async function uploadHomeSlideImgDirect(files) {
  if (!files || !files.length) return;
  const formData = new FormData();
  formData.append('client_watermarked', '1');
  let wmDataUrl = '';
  if (window.PsycoApp && typeof PsycoApp.watermarkImageFile === 'function') {
    const wm = await PsycoApp.watermarkImageFile(files[0], 1400);
    if (wm && wm.file) formData.append('product_images[]', wm.file);
    else formData.append('product_images[]', files[0]);
    if (wm && wm.dataUrl) wmDataUrl = wm.dataUrl;
  } else {
    formData.append('product_images[]', files[0]);
  }
  try {
    const res = await fetch('/kedai-produk/upload.php', { method: 'POST', body: formData });
    const data = await res.json();
    if (data.status === 'success' && Array.isArray(data.images) && data.images.length > 0) {
      document.getElementById('hs-image').value = data.images[0];
      alert('Gambar (dengan Watermark PSYCO TIME X PRO) berjaya dimuat naik! Sila klik Simpan Perubahan.');
    } else if (wmDataUrl) {
      document.getElementById('hs-image').value = wmDataUrl;
      alert('Gambar (dengan Watermark PSYCO TIME X PRO) sedia! Sila klik Simpan Perubahan.');
    } else {
      alert(data.message || 'Gagal memuat naik gambar.');
    }
  } catch (e) {
    if (wmDataUrl) {
      document.getElementById('hs-image').value = wmDataUrl;
      alert('Gambar (dengan Watermark PSYCO TIME X PRO) sedia! Sila klik Simpan Perubahan.');
    } else {
      alert('Ralat semasa memuat naik gambar.');
    }
  }
}

async function handleSaveHomeSlideDirect(e) {
  e.preventDefault();
  const res = await fetch('/admin/api.php?action=save_hero_slide', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      action: 'save_hero_slide',
      id: document.getElementById('hs-id').value,
      badge: document.getElementById('hs-badge').value,
      highlightTag: document.getElementById('hs-tag').value,
      title: document.getElementById('hs-title').value,
      subtitle: document.getElementById('hs-subtitle').value,
      description: document.getElementById('hs-desc').value,
      imageUrl: document.getElementById('hs-image').value,
      ctaText: document.getElementById('hs-cta').value,
      targetUrl: document.getElementById('hs-url').value
    })
  });
  const data = await res.json();
  if (data.success) {
    window.location.reload();
  } else {
    alert(data.error || 'Gagal menyimpan perubahan slide.');
  }
}

async function deleteHomeSlideDirect(id) {
  if (!id) return;
  if (!confirm('Padam slide / iklan ini daripada halaman Home?')) return;
  const res = await fetch('/admin/api.php?action=delete_hero_slide', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ action: 'delete_hero_slide', id })
  });
  const data = await res.json();
  if (data.success) window.location.reload();
}

function openBusinessHubModal() {
  const dlg = document.getElementById('bhub-ad-dialog');
  if (!dlg) return;
  document.getElementById('bh-modal-title').textContent = '➕ Tambah Iklan Baharu ke PsycotimexPro Business Hub';
  document.getElementById('bh-id').value = '';
  document.getElementById('bh-partner').value = '';
  document.getElementById('bh-badge').value = 'RAKAN STRATEGIK & BISNES HUB';
  document.getElementById('bh-title').value = '';
  document.getElementById('bh-tagline').value = '';
  document.getElementById('bh-image').value = '/assets/images/nexa-banner.png';
  document.getElementById('bh-logo').value = '/assets/images/nexa-logo.jpg';
  document.getElementById('bh-cta').value = 'Hubungi Rakan Strategik';
  document.getElementById('bh-url').value = '/contact/index.php';
  dlg.showModal();
}

function openEditBusinessHubModal(ad) {
  const dlg = document.getElementById('bhub-ad-dialog');
  if (!dlg) return;
  document.getElementById('bh-modal-title').textContent = '✏️ Edit Iklan PsycotimexPro Business Hub';
  document.getElementById('bh-id').value = ad.id || '';
  document.getElementById('bh-partner').value = ad.partner_name || '';
  document.getElementById('bh-badge').value = ad.badge || '';
  document.getElementById('bh-title').value = ad.title || '';
  document.getElementById('bh-tagline').value = ad.tagline || '';
  document.getElementById('bh-image').value = ad.image || '/assets/images/nexa-banner.png';
  document.getElementById('bh-logo').value = ad.logo || '/assets/images/nexa-logo.jpg';
  document.getElementById('bh-cta').value = ad.cta_text || 'Hubungi Rakan Strategik';
  document.getElementById('bh-url').value = ad.target_url || '/contact/index.php';
  dlg.showModal();
}

async function uploadBusinessHubImg(files, targetInputId) {
  if (!files || !files.length) return;
  const formData = new FormData();
  formData.append('client_watermarked', '1');
  let wmDataUrl = '';
  if (window.PsycoApp && typeof PsycoApp.watermarkImageFile === 'function') {
    const wm = await PsycoApp.watermarkImageFile(files[0], 1400);
    if (wm && wm.file) formData.append('product_images[]', wm.file);
    else formData.append('product_images[]', files[0]);
    if (wm && wm.dataUrl) wmDataUrl = wm.dataUrl;
  } else {
    formData.append('product_images[]', files[0]);
  }
  try {
    const res = await fetch('/kedai-produk/upload.php', { method: 'POST', body: formData });
    const data = await res.json();
    if (data.status === 'success' && Array.isArray(data.images) && data.images.length > 0) {
      document.getElementById(targetInputId).value = data.images[0];
      alert('Gambar iklan berjaya dimuat naik!');
    } else if (wmDataUrl) {
      document.getElementById(targetInputId).value = wmDataUrl;
      alert('Gambar iklan sedia digunakan!');
    }
  } catch (e) {
    if (wmDataUrl) document.getElementById(targetInputId).value = wmDataUrl;
  }
}

async function handleSaveBusinessHubAd(e) {
  e.preventDefault();
  const res = await fetch('/admin/api.php?action=save_business_hub_ad', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      action: 'save_business_hub_ad',
      id: document.getElementById('bh-id').value,
      partner_name: document.getElementById('bh-partner').value,
      badge: document.getElementById('bh-badge').value,
      title: document.getElementById('bh-title').value,
      tagline: document.getElementById('bh-tagline').value,
      image: document.getElementById('bh-image').value,
      logo: document.getElementById('bh-logo').value,
      cta_text: document.getElementById('bh-cta').value,
      target_url: document.getElementById('bh-url').value
    })
  });
  const data = await res.json();
  if (data.success) window.location.reload();
  else alert(data.error || 'Gagal menyimpan iklan Business Hub.');
}

async function deleteBusinessHubAd(id) {
  if (!id || !confirm('Padam iklan ini daripada Business Hub Carousel?')) return;
  const res = await fetch('/admin/api.php?action=delete_business_hub_ad', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ action: 'delete_business_hub_ad', id })
  });
  const data = await res.json();
  if (data.success) window.location.reload();
}
</script>

<!-- DIALOG: ADMIN ADD / EDIT BUSINESS HUB AD -->
<dialog id="bhub-ad-dialog" closedby="any" class="w-full max-w-2xl rounded-3xl border border-amber-500/35 bg-[#171719] p-0 text-white shadow-2xl backdrop:bg-black/85">
  <div class="flex items-center justify-between border-b border-white/10 px-6 py-4">
    <h3 class="font-orbitron text-base font-bold text-white" id="bh-modal-title">Urus Iklan PsycotimexPro Business Hub</h3>
    <button type="button" onclick="document.getElementById('bhub-ad-dialog').close()" class="flex h-8 w-8 items-center justify-center rounded-lg bg-[#0B0B0D] text-[#C0C0C0] hover:text-white">&times;</button>
  </div>
  <form onsubmit="handleSaveBusinessHubAd(event)" class="p-6 space-y-4 text-xs max-h-[82vh] overflow-y-auto">
    <input type="hidden" id="bh-id" value="">
    <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
      <div>
        <label class="mb-1 block font-bold text-[#E5E5E5]">Nama Syarikat / Rakan Strategik *</label>
        <input type="text" id="bh-partner" required placeholder="Contoh: NEXA SPORT VENTURES" class="w-full rounded-xl border border-white/15 bg-[#0B0B0D] px-3.5 py-2.5 text-white">
      </div>
      <div>
        <label class="mb-1 block font-bold text-[#E5E5E5]">Label Lencana (Badge) *</label>
        <input type="text" id="bh-badge" required value="RAKAN STRATEGIK & BISNES HUB" class="w-full rounded-xl border border-white/15 bg-[#0B0B0D] px-3.5 py-2.5 text-white">
      </div>
    </div>
    <div>
      <label class="mb-1 block font-bold text-[#E5E5E5]">Tajuk Promosi / Iklan *</label>
      <input type="text" id="bh-title" required placeholder="Contoh: NEXA AKAN MULA BEROPERASI PADA JULAI 2026" class="w-full rounded-xl border border-white/15 bg-[#0B0B0D] px-3.5 py-2.5 font-bold text-white">
    </div>
    <div>
      <label class="mb-1 block font-bold text-[#E5E5E5]">Tagline / Perkhidmatan Ditawarkan *</label>
      <input type="text" id="bh-tagline" required placeholder="Contoh: System Management • Event Organising • Apparel • Electronic Timing" class="w-full rounded-xl border border-white/15 bg-[#0B0B0D] px-3.5 py-2.5 text-white">
    </div>
    <div class="rounded-2xl border border-white/10 bg-[#0B0B0D] p-3.5 space-y-2">
      <div class="flex flex-wrap items-center justify-between gap-2">
        <label class="font-bold text-[#E5E5E5]">Gambar Banner Iklan *</label>
        <label class="cursor-pointer rounded-lg bg-[#D71920] px-3 py-1.5 text-[11px] font-bold text-white hover:bg-[#9F1118] transition">
          📸 Muat Naik Banner Iklan
          <input type="file" accept="image/*,.png,.jpg,.jpeg,.webp,.gif,.bmp,.svg,.img" onchange="uploadBusinessHubImg(this.files, 'bh-image')" class="hidden">
        </label>
      </div>
      <input type="text" id="bh-image" required value="/assets/images/nexa-banner.png" class="w-full rounded-xl border border-white/15 bg-[#171719] px-3 py-2 text-white font-mono text-[11px]">
    </div>
    <div class="rounded-2xl border border-white/10 bg-[#0B0B0D] p-3.5 space-y-2">
      <div class="flex flex-wrap items-center justify-between gap-2">
        <label class="font-bold text-[#E5E5E5]">Logo Rakan Strategik / Bisnes</label>
        <label class="cursor-pointer rounded-lg border border-white/20 bg-[#171719] px-3 py-1.5 text-[11px] font-bold text-white hover:border-[#D71920] transition">
          📸 Muat Naik Logo
          <input type="file" accept="image/*,.png,.jpg,.jpeg,.webp,.gif,.bmp,.svg,.img" onchange="uploadBusinessHubImg(this.files, 'bh-logo')" class="hidden">
        </label>
      </div>
      <input type="text" id="bh-logo" value="/assets/images/nexa-logo.jpg" class="w-full rounded-xl border border-white/15 bg-[#171719] px-3 py-2 text-white font-mono text-[11px]">
    </div>
    <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
      <div>
        <label class="mb-1 block font-bold text-[#E5E5E5]">Teks Butang (CTA)</label>
        <input type="text" id="bh-cta" value="Hubungi Rakan Strategik" class="w-full rounded-xl border border-white/15 bg-[#0B0B0D] px-3.5 py-2.5 text-white">
      </div>
      <div>
        <label class="mb-1 block font-bold text-[#E5E5E5]">Pautan URL</label>
        <input type="text" id="bh-url" value="/contact/index.php" class="w-full rounded-xl border border-white/15 bg-[#0B0B0D] px-3.5 py-2.5 font-mono text-white">
      </div>
    </div>
    <div class="flex justify-end gap-3 pt-2">
      <button type="button" onclick="document.getElementById('bhub-ad-dialog').close()" class="psyco-btn psyco-btn-secondary px-5 py-2.5 text-xs">Batal</button>
      <button type="submit" class="psyco-btn psyco-btn-primary px-6 py-2.5 text-xs">💾 Simpan Iklan Business Hub</button>
    </div>
  </form>
</dialog>
<?php endif; ?>

<script>
  (function() {
    let currentSlide = 0;
    const slides = document.querySelectorAll('.psyco-carousel-slide');
    if (slides.length) {
      window.psycoMoveSlide = function(delta) {
        slides[currentSlide].classList.remove('opacity-100', 'z-10');
        slides[currentSlide].classList.add('opacity-0', 'z-0', 'pointer-events-none');
        currentSlide = (currentSlide + delta + slides.length) % slides.length;
        slides[currentSlide].classList.remove('opacity-0', 'z-0', 'pointer-events-none');
        slides[currentSlide].classList.add('opacity-100', 'z-10');
      };
      setInterval(function() {
        window.psycoMoveSlide(1);
      }, 6000);
    }

    let currentBhub = 0;
    const bhubSlides = document.querySelectorAll('.psyco-bhub-slide');
    if (bhubSlides.length) {
      window.psycoMoveBhubSlide = function(delta) {
        bhubSlides[currentBhub].classList.remove('block', 'opacity-100');
        bhubSlides[currentBhub].classList.add('hidden', 'opacity-0');
        currentBhub = (currentBhub + delta + bhubSlides.length) % bhubSlides.length;
        bhubSlides[currentBhub].classList.remove('hidden', 'opacity-0');
        bhubSlides[currentBhub].classList.add('block', 'opacity-100');
      };
      if (bhubSlides.length > 1) {
        setInterval(function() {
          window.psycoMoveBhubSlide(1);
        }, 7500);
      }
    }
  })();
</script>

<?php require_once __DIR__ . '/includes/footer.php'; ?>
