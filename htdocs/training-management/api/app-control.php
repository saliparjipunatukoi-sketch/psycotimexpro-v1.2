<?php
/**
 * Psyco Time X Pro - Master App Control & Remote Kill-Switch API Endpoint
 * Lokasi: htdocs/training-management/api/app-control.php
 * 
 * Membenarkan Coach Salipar Jipun mengawal aplikasi Android dari jauh:
 * 1. Global Kill-Switch (Hentikan / Kunci semua aplikasi serta-merta)
 * 2. Kelulusan Pengguna (Pengguna baru tidak boleh guna selagi belum diluluskan)
 * 3. Sekat / Tangguh Pengguna (Block / Suspend unauthorized users)
 * 4. Kawalan Langganan & Tarikh Luput
 */

require_once __DIR__ . '/db.php';

header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

$controlFile = $dataDir . '/system_control.json';

// Inisialisasi fail system_control jika belum wujud
function getSystemControl($filePath) {
    if (!file_exists($filePath)) {
        $defaultConfig = [
            'kill_switch_active' => false,
            'kill_switch_message' => 'Sistem Psyco Time X Pro telah dikunci atau dihentikan sementara oleh Pentadbir Rasmi (Coach Salipar Jipun). Sila hubungi +6019-5326399 untuk pengaktifan lesen.',
            'require_admin_approval' => false, // Pendaftaran baru automatik mendapat 7 Hari Percubaan Percuma (Auto Free 7 Days Trial)
            'default_trial_days' => 7,
            'broadcast_message' => '',
            'superadmin_email' => 'saliparjipun.atukoi@gmail.com',
            'admin_whatsapp' => '+6019-5326399',
            'updated_at' => date('Y-m-d H:i:s')
        ];
        file_put_contents($filePath, json_encode($defaultConfig, JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE));
        return $defaultConfig;
    }
    $content = file_get_contents($filePath);
    $data = json_decode($content, true);
    return is_array($data) ? $data : [];
}

$controlConfig = getSystemControl($controlFile);
$superadminEmail = 'saliparjipun.atukoi@gmail.com';

// -------------------------------------------------------------
// GET REQUEST: Disemak oleh APK Android & Web pada setiap pembukaan / pendaftaran
// -------------------------------------------------------------
if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    $email = strtolower(trim($_GET['email'] ?? ''));
    $isSuper = ($email === strtolower($superadminEmail));

    // Jika emel Superadmin, sentiasa diberi akses master tanpa sebarang sekatan
    if ($isSuper) {
        echo json_encode([
            'status' => 'success',
            'isSuperadmin' => true,
            'killSwitchActive' => $controlConfig['kill_switch_active'] ?? false,
            'isApproved' => true,
            'userStatus' => 'active',
            'plan' => 'superadmin',
            'planName' => 'Superadmin Unlimited',
            'subscriptionEnd' => '2099-12-31 23:59:59',
            'adminWhatsapp' => '+6019-5326399',
            'message' => 'Akses Master Superadmin Dibenarkan.'
        ], JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE);
        exit;
    }

    // 1. Semak Global Kill-Switch untuk pengguna biasa
    if (!empty($controlConfig['kill_switch_active'])) {
        echo json_encode([
            'status' => 'locked',
            'isSuperadmin' => false,
            'killSwitchActive' => true,
            'isApproved' => false,
            'userStatus' => 'system_locked',
            'adminWhatsapp' => '+6019-5326399',
            'message' => $controlConfig['kill_switch_message'] ?? 'Aplikasi dikunci oleh Pentadbir (+6019-5326399).'
        ], JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE);
        exit;
    }

    // 2. Semak status individu pengguna dalam users.json
    $users = readJsonFile($usersFile);
    $currentUser = null;
    $userIndex = -1;

    foreach ($users as $idx => $u) {
        if (strtolower($u['email'] ?? '') === $email) {
            $currentUser = $u;
            $userIndex = $idx;
            break;
        }
    }

    // Jika pengguna sedia ada menghantar maklumat profil terkini (nama penuh, negara, telefon, kelab, sijil), kemas kini
    if ($currentUser && $userIndex >= 0) {
        $profileUpdated = false;
        if (!empty($_GET['name'])) { $users[$userIndex]['name'] = trim($_GET['name']); $profileUpdated = true; }
        if (!empty($_GET['phone'])) { $users[$userIndex]['phone'] = trim($_GET['phone']); $profileUpdated = true; }
        if (!empty($_GET['club'])) { $users[$userIndex]['club_name'] = trim($_GET['club']); $profileUpdated = true; }
        if (!empty($_GET['country'])) {
            $c = trim($_GET['country']);
            $users[$userIndex]['country'] = $c;
            $users[$userIndex]['currency'] = (strtolower($c) === 'malaysia') ? 'MYR' : 'USD';
            $profileUpdated = true;
        }
        if (!empty($_GET['cert'])) { $users[$userIndex]['coach_certification'] = trim($_GET['cert']); $profileUpdated = true; }
        if ($profileUpdated) {
            writeJsonFile($usersFile, $users);
            $currentUser = $users[$userIndex];
        }
    }

    // Jika pengguna belum didaftarkan langsung di pelayan (Pendaftaran Kali Pertama -> Auto Free 7 Days Trial)
    if (!$currentUser) {
        if (!empty($email)) {
            $now = time();
            $trialDays = (int)($controlConfig['default_trial_days'] ?? 7);
            if ($trialDays <= 0) $trialDays = 7;
            $requireApproval = !empty($controlConfig['require_admin_approval']);
            $countryVal = trim($_GET['country'] ?? 'Malaysia');
            $currencyVal = (strtolower($countryVal) === 'malaysia') ? 'MYR' : 'USD';

            $newUser = [
                'id' => 'USR-' . strtoupper(substr(md5($email . $now), 0, 6)),
                'name' => !empty($_GET['name']) ? trim($_GET['name']) : ('Jurulatih (' . substr($email, 0, strpos($email, '@')) . ')'),
                'email' => $email,
                'role' => 'coach',
                'country' => $countryVal,
                'currency' => $currencyVal,
                'phone' => trim($_GET['phone'] ?? ''),
                'club_name' => !empty($_GET['club']) ? trim($_GET['club']) : 'Kelab Sukan',
                'coach_certification' => trim($_GET['cert'] ?? ''),
                'plan' => 'trial',
                'plan_name' => "Percubaan Percuma ({$trialDays} Hari)",
                'max_athletes' => 15,
                'max_sub_coaches' => 1,
                'registered_at' => date('Y-m-d H:i:s', $now),
                'subscription_start' => date('Y-m-d H:i:s', $now),
                'subscription_end' => date('Y-m-d H:i:s', $now + ($trialDays * 86400)),
                'status' => $requireApproval ? 'pending' : 'active',
                'is_trial' => true,
                'whatsapp_verified' => false
            ];
            $users[] = $newUser;
            writeJsonFile($usersFile, $users);
            $currentUser = $newUser;
        } else {
            echo json_encode([
                'status' => 'error',
                'isApproved' => false,
                'userStatus' => 'unauthenticated',
                'message' => 'Parameter emel diperlukan untuk semakan kawalan aplikasi.'
            ]);
            exit;
        }
    }

    // Semak status kelulusan pengguna
    $userStatus = strtolower($currentUser['status'] ?? 'active');
    $subEnd = strtotime($currentUser['subscription_end'] ?? 'now');
    $isExpired = (time() > $subEnd);

    if ($userStatus === 'suspended' || $userStatus === 'blocked') {
        echo json_encode([
            'status' => 'blocked',
            'isApproved' => false,
            'userStatus' => 'blocked',
            'killSwitchActive' => false,
            'adminWhatsapp' => '+6019-5326399',
            'message' => 'Akaun anda telah DISEKAT oleh Pentadbir Utama (Coach Salipar Jipun). Sila hubungi Admin WhatsApp +6019-5326399 untuk mengaktifkan peranti anda.'
        ], JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE);
        exit;
    }

    if ($userStatus === 'pending') {
        echo json_encode([
            'status' => 'pending_approval',
            'isApproved' => false,
            'userStatus' => 'pending',
            'killSwitchActive' => false,
            'registeredAt' => $currentUser['registered_at'] ?? date('Y-m-d H:i:s'),
            'adminWhatsapp' => '+6019-5326399',
            'message' => 'Akaun anda (' . $email . ') sedang MENUNGGU KELULUSAN daripada Admin (+6019-5326399). Akses fungsi masa dan kamera belum diaktifkan.'
        ], JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE);
        exit;
    }

    if ($isExpired) {
        echo json_encode([
            'status' => 'expired',
            'isApproved' => false,
            'userStatus' => 'expired',
            'killSwitchActive' => false,
            'subscriptionEnd' => $currentUser['subscription_end'] ?? '-',
            'adminWhatsapp' => '+6019-5326399',
            'message' => 'Tempoh Percubaan 7 Hari / Langganan akaun anda telah TAMAT. Sila pilih pakej langganan di laman web atau hubungi Admin WhatsApp (+6019-5326399).'
        ], JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE);
        exit;
    }

    // Pengguna yang aktif dan diluluskan (termasuk dalam tempoh 7 Hari Percubaan Percuma)
    echo json_encode([
        'status' => 'active',
        'isApproved' => true,
        'userStatus' => 'active',
        'killSwitchActive' => false,
        'plan' => $currentUser['plan'] ?? 'trial',
        'planName' => $currentUser['plan_name'] ?? 'Percubaan Percuma (7 Hari)',
        'country' => $currentUser['country'] ?? 'Malaysia',
        'currency' => $currentUser['currency'] ?? 'MYR',
        'maxAthletes' => $currentUser['max_athletes'] ?? 30,
        'subscriptionEnd' => $currentUser['subscription_end'] ?? '-',
        'broadcastMessage' => $controlConfig['broadcast_message'] ?? '',
        'adminWhatsapp' => '+6019-5326399',
        'message' => 'Akaun disahkan aktif oleh pelayan.'
    ], JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE);
    exit;
}

// -------------------------------------------------------------
// POST REQUEST: Arahan daripada Coach Salipar Jipun (Admin Page atau APK)
// -------------------------------------------------------------
$input = file_get_contents('php://input');
$data = json_decode($input, true);

if (!$data) {
    echo json_encode(['status' => 'error', 'message' => 'Payload JSON tidak sah.']);
    exit;
}

$adminEmail = strtolower(trim($data['adminEmail'] ?? ''));
if ($adminEmail !== strtolower($superadminEmail)) {
    echo json_encode([
        'status' => 'error',
        'message' => 'Akses Ditolak: Hanya Superadmin (saliparjipun.atukoi@gmail.com) dibenarkan mengawal aplikasi.'
    ]);
    exit;
}

$action = $data['action'] ?? '';

// 1. Arahan: Toggle Kill-Switch (Hentikan / Buka Kunci Aplikasi Global)
if ($action === 'toggle_kill_switch') {
    $active = (bool)($data['active'] ?? false);
    $msg = trim($data['message'] ?? '');
    if (empty($msg)) {
        $msg = $active ? 
            'Sistem Psyco Time X Pro telah DIKUNCI OLEH PENTADBIR UTAMA. Sila hubungi Admin WhatsApp (+6019-5326399).' : 
            'Sistem Electronic Timing beroperasi seperti biasa.';
    }
    $controlConfig['kill_switch_active'] = $active;
    $controlConfig['kill_switch_message'] = $msg;
    $controlConfig['updated_at'] = date('Y-m-d H:i:s');
    file_put_contents($controlFile, json_encode($controlConfig, JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE));

    echo json_encode([
        'status' => 'success',
        'killSwitchActive' => $active,
        'message' => $active ? 'MASTER KILL-SWITCH DIAKTIFKAN: Semua aplikasi kini dikunci serta-merta!' : 'MASTER KILL-SWITCH DIMATIKAN: Aplikasi dibuka semula.'
    ], JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE);
    exit;
}

// 2. Arahan: Luluskan Pengguna (Approve User)
if ($action === 'approve_user') {
    $targetEmail = strtolower(trim($data['targetEmail'] ?? ''));
    $days = (int)($data['days'] ?? 30);
    $plan = trim($data['plan'] ?? 'plan_rm30');
    $users = readJsonFile($usersFile);
    $found = false;

    foreach ($users as $idx => $u) {
        if (strtolower($u['email'] ?? '') === $targetEmail) {
            $found = true;
            $users[$idx]['status'] = 'active';
            $users[$idx]['plan'] = $plan;
            if (isset($SUBSCRIPTION_PLANS[$plan])) {
                $users[$idx]['plan_name'] = $SUBSCRIPTION_PLANS[$plan]['name'];
                $users[$idx]['max_athletes'] = $SUBSCRIPTION_PLANS[$plan]['max_athletes'];
                $users[$idx]['max_sub_coaches'] = $SUBSCRIPTION_PLANS[$plan]['max_sub_coaches'];
            }
            $now = time();
            $users[$idx]['subscription_start'] = date('Y-m-d H:i:s', $now);
            $users[$idx]['subscription_end'] = date('Y-m-d H:i:s', $now + ($days * 86400));
            $users[$idx]['approved_at'] = date('Y-m-d H:i:s', $now);
            break;
        }
    }

    if ($found) {
        writeJsonFile($usersFile, $users);
        echo json_encode([
            'status' => 'success',
            'message' => "Pengguna {$targetEmail} BERJAYA DILULUSKAN dan diaktifkan selama {$days} hari!"
        ], JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE);
    } else {
        echo json_encode(['status' => 'error', 'message' => "Pengguna {$targetEmail} tidak ditemui."]);
    }
    exit;
}

// 3. Arahan: Sekat Pengguna (Block / Suspend User)
if ($action === 'block_user') {
    $targetEmail = strtolower(trim($data['targetEmail'] ?? ''));
    if ($targetEmail === strtolower($superadminEmail)) {
        echo json_encode(['status' => 'error', 'message' => 'Anda tidak boleh menyekat akaun Superadmin anda sendiri.']);
        exit;
    }
    $users = readJsonFile($usersFile);
    $found = false;

    foreach ($users as $idx => $u) {
        if (strtolower($u['email'] ?? '') === $targetEmail) {
            $found = true;
            $users[$idx]['status'] = 'blocked';
            break;
        }
    }

    if ($found) {
        writeJsonFile($usersFile, $users);
        echo json_encode([
            'status' => 'success',
            'message' => "Pengguna {$targetEmail} BERJAYA DISEKAT. Aplikasi mereka kini disekat daripada berfungsi!"
        ], JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE);
    } else {
        echo json_encode(['status' => 'error', 'message' => "Pengguna {$targetEmail} tidak ditemui."]);
    }
    exit;
}

// 4. Arahan: Senarai Semua Pengguna & Status Untuk Paparan Admin
if ($action === 'get_system_status') {
    $users = readJsonFile($usersFile);
    echo json_encode([
        'status' => 'success',
        'killSwitchActive' => $controlConfig['kill_switch_active'] ?? false,
        'killSwitchMessage' => $controlConfig['kill_switch_message'] ?? '',
        'broadcastMessage' => $controlConfig['broadcast_message'] ?? '',
        'totalUsers' => count($users),
        'users' => $users
    ], JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE);
    exit;
}

echo json_encode(['status' => 'error', 'message' => "Tindakan '{$action}' tidak sah."]);
