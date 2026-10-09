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

// Semak tarikh luput langganan
$subExpiryTime = strtotime($currentUser['subscription_end']);
$isExpired = (time() > $subExpiryTime);
$daysLeft = ceil(($subExpiryTime - time()) / 86400);

// 2. Simpan atau kemaskini rekod Atlit dari APK
$existingAthletes = readJsonFile($athletesFile);
$incomingAthletes = $data['athletes'] ?? [];
$athletesSyncedCount = 0;

$currentAthleteCount = count(array_filter($existingAthletes, function($a) use ($coachEmail) {
    return strtolower($a['coachEmail'] ?? '') === strtolower($coachEmail);
}));

$maxAllowedAthletes = $currentUser['max_athletes'] ?? 25;

foreach ($incomingAthletes as $inAth) {
    $athId = $inAth['id'] ?? ('ATH-' . uniqid());
    $found = false;
    
    foreach ($existingAthletes as $k => $exist) {
        if ($exist['id'] == $athId || ($exist['name'] === $inAth['name'] && strtolower($exist['coachEmail']) === strtolower($coachEmail))) {
            // Update
            $existingAthletes[$k] = array_merge($exist, $inAth, [
                'coachEmail' => $coachEmail,
                'syncedAt' => date('Y-m-d H:i:s')
            ]);
            $found = true;
            $athletesSyncedCount++;
            break;
        }
    }
    
    if (!$found) {
        // Semak kuota had atlit
        if ($currentAthleteCount < $maxAllowedAthletes) {
            $inAth['coachEmail'] = $coachEmail;
            $inAth['syncedAt'] = date('Y-m-d H:i:s');
            $existingAthletes[] = $inAth;
            $currentAthleteCount++;
            $athletesSyncedCount++;
        }
    }
}
writeJsonFile($athletesFile, $existingAthletes);

// 3. Simpan atau kemaskini rekod Race dari APK
$existingRaces = readJsonFile($racesFile);
$incomingRaces = $data['raceRecords'] ?? [];
$racesSyncedCount = 0;

foreach ($incomingRaces as $inRace) {
    $raceId = $inRace['id'] ?? ('RACE-' . uniqid());
    $found = false;
    foreach ($existingRaces as $k => $exist) {
        if ($exist['id'] == $raceId) {
            $existingRaces[$k] = array_merge($exist, $inRace, [
                'coachEmail' => $coachEmail,
                'updatedAt' => date('Y-m-d H:i:s')
            ]);
            $found = true;
            $racesSyncedCount++;
            break;
        }
    }
    if (!$found) {
        $inRace['coachEmail'] = $coachEmail;
        $inRace['createdAt'] = date('Y-m-d H:i:s');
        $existingRaces[] = $inRace;
        $racesSyncedCount++;
    }
}
writeJsonFile($racesFile, $existingRaces);

// Return response to APK
echo json_encode([
    'status' => 'success',
    'message' => 'Penyelarasan web berjaya!',
    'serverTimestamp' => date('d M Y, H:i:s'),
    'user' => [
        'id' => $currentUser['id'],
        'email' => $currentUser['email'],
        'club_name' => $currentUser['club_name'],
        'plan' => $currentUser['plan'],
        'plan_name' => $currentUser['plan_name'],
        'max_athletes' => $currentUser['max_athletes'],
        'subscription_end' => $currentUser['subscription_end'],
        'days_remaining' => $daysLeft > 0 ? $daysLeft : 0,
        'is_expired' => $isExpired
    ],
    'synced' => [
        'athletes_count' => $athletesSyncedCount,
        'races_count' => $racesSyncedCount
    ]
], JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE);
