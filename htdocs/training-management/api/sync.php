<?php
/**
 * Psyco Time X Pro - Training Management Sync API Endpoint
 * Lokasi: htdocs/training-management/api/sync.php
 * Menerima payload sync automatik daripada APK Android
 * (Senarai Atlit, PB, Keputusan Race, Data Biomekanik, Status Langganan)
 */

require_once __DIR__ . '/db.php';

header('Content-Type: application/json; charset=utf-8');

$input = file_get_contents('php://input');
$data = json_decode($input, true);

if (!$data) {
    echo json_encode([
        'status' => 'error',
        'message' => 'Format payload JSON tidak sah atau kosong.'
    ]);
    exit;
}

$coachEmail = trim($data['coachEmail'] ?? '');
if (empty($coachEmail)) {
    echo json_encode([
        'status' => 'error',
        'message' => 'Parameter coachEmail diperlukan untuk penyelarasan akaun.'
    ]);
    exit;
}

// 1. Semak status langganan User
$users = readJsonFile($usersFile);
$currentUser = null;
$userIndex = -1;

foreach ($users as $idx => $u) {
    if (strtolower($u['email']) === strtolower($coachEmail)) {
        $currentUser = $u;
        $userIndex = $idx;
        break;
    }
}

// Jika belum wujud, daftarkan secara automatik sebagai Free Trial 7 Hari
if (!$currentUser) {
    $now = time();
    $currentUser = [
        'id' => 'USR-' . strtoupper(substr(md5($coachEmail . time()), 0, 6)),
        'name' => $data['clubName'] ?? 'Jurulatih Baru',
        'email' => $coachEmail,
        'password' => '123456',
        'role' => 'coach',
        'phone' => $data['phone'] ?? '',
        'club_name' => $data['clubName'] ?? 'Kelab Sukan',
        'plan' => 'trial',
        'plan_name' => 'Percubaan Percuma (7 Hari)',
        'max_athletes' => 10,
        'max_sub_coaches' => 1,
        'registered_at' => date('Y-m-d H:i:s', $now),
        'subscription_start' => date('Y-m-d H:i:s', $now),
        'subscription_end' => date('Y-m-d H:i:s', $now + (7 * 86400)),
        'status' => 'active',
        'is_trial' => true,
        'whatsapp_verified' => false
    ];
    $users[] = $currentUser;
    writeJsonFile($usersFile, $users);
}

// 2. Semak Tarikh Luput Akaun
$expiryTime = strtotime($currentUser['subscription_end'] ?? 'now');
$isExpired = (time() > $expiryTime) && ($currentUser['role'] !== 'superadmin');

// 3. Simpan dan gabungkan rekod atlit yang disinkronkan dari APK
$incomingAthletes = $data['athletes'] ?? [];
$allAthletes = readJsonFile($athletesFile);

// Ambil rekod atlit sedia ada dari coach lain
$otherAthletes = array_values(array_filter($allAthletes, function($a) use ($coachEmail) {
    return strtolower($a['coachEmail'] ?? '') !== strtolower($coachEmail);
}));

// Tambah/Kemas kini rekod atlit bagi coach ini
$syncedAthletesCount = 0;
foreach ($incomingAthletes as $ia) {
    $ia['coachEmail'] = $coachEmail;
    $ia['clubName'] = $currentUser['club_name'] ?? ($data['clubName'] ?? '');
    $ia['syncedAt'] = date('Y-m-d H:i:s');
    $otherAthletes[] = $ia;
    $syncedAthletesCount++;
}
writeJsonFile($athletesFile, $otherAthletes);

// 4. Simpan dan gabungkan rekod perlumbaan (races)
$incomingRaces = $data['raceRecords'] ?? [];
$allRaces = readJsonFile($racesFile);

$otherRaces = array_values(array_filter($allRaces, function($r) use ($coachEmail) {
    return strtolower($r['coachEmail'] ?? '') !== strtolower($coachEmail);
}));

$syncedRacesCount = 0;
foreach ($incomingRaces as $ir) {
    $ir['coachEmail'] = $coachEmail;
    $ir['syncedAt'] = date('Y-m-d H:i:s');
    $otherRaces[] = $ir;
    $syncedRacesCount++;
}
writeJsonFile($racesFile, $otherRaces);

// 5. Kembalikan respons rasmi ke APK
echo json_encode([
    'status' => 'success',
    'message' => 'Penyegerakan ke Portal Web Training Management berjaya!',
    'isExpired' => $isExpired,
    'currentPlan' => $currentUser['plan'] ?? 'trial',
    'planName' => $currentUser['plan_name'] ?? 'Percubaan Percuma',
    'maxAthletes' => $currentUser['max_athletes'] ?? 10,
    'maxSubCoaches' => $currentUser['max_sub_coaches'] ?? 1,
    'subscriptionEnd' => $currentUser['subscription_end'] ?? '-',
    'syncedAthletes' => $syncedAthletesCount,
    'syncedRaces' => $syncedRacesCount,
    'serverTimestamp' => date('Y-m-d H:i:s')
], JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE);
