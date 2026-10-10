<?php
/**
 * Psyco Time X Pro - Subscription Upgrade & WhatsApp Payment Workflow
 * Lokasi: htdocs/training-management/api/upgrade.php
 * Menguruskan sistem permohonan upgrade pakej, pelanjutan tarikh luput automatik,
 * dan integrasi WhatsApp ke Admin (Coach Salipar Jipun).
 */

require_once __DIR__ . '/db.php';

header('Content-Type: application/json; charset=utf-8');

$input = file_get_contents('php://input');
$data = json_decode($input, true);

if (!$data) {
    echo json_encode(['status' => 'error', 'message' => 'Data tidak sah.']);
    exit;
}

$userEmail = trim($data['email'] ?? '');
$targetPlanKey = trim($data['targetPlan'] ?? '');
$paymentMethod = trim($data['paymentMethod'] ?? 'WhatsApp Admin');

if (empty($userEmail) || !isset($SUBSCRIPTION_PLANS[$targetPlanKey])) {
    echo json_encode([
        'status' => 'error',
        'message' => 'Sila pilih pakej langganan yang sah.'
    ]);
    exit;
}

$planDetails = $SUBSCRIPTION_PLANS[$targetPlanKey];
$users = readJsonFile($usersFile);
$userFound = false;
$updatedUser = null;
$adminWhatsApp = "601112345678"; // Nombor WhatsApp Admin rasmi (Coach Salipar Jipun)

foreach ($users as $idx => $u) {
    if (strtolower($u['email']) === strtolower($userEmail)) {
        $userFound = true;
        
        $now = time();
        $currentExpiry = strtotime($u['subscription_end'] ?? 'now');
        $base = ($currentExpiry > $now) ? $currentExpiry : $now;
        $newExpiryTime = $base + (30 * 86400);

        $users[$idx]['plan'] = $targetPlanKey;
        $users[$idx]['plan_name'] = $planDetails['name'];
        $users[$idx]['max_athletes'] = $planDetails['max_athletes'];
        $users[$idx]['max_sub_coaches'] = $planDetails['max_sub_coaches'] ?? 2;
        $users[$idx]['subscription_start'] = date('Y-m-d H:i:s', $now);
        $users[$idx]['subscription_end'] = date('Y-m-d H:i:s', $newExpiryTime);
        $users[$idx]['is_trial'] = false;
        $users[$idx]['status'] = 'active';
        $updatedUser = $users[$idx];
        break;
    }
}

if (!$userFound) {
    // Daftar user baru secara automatik jika belum wujud
    $now = time();
    $newExpiryTime = $now + (30 * 86400);
    $newUser = [
        'id' => 'USR-' . strtoupper(substr(md5($userEmail . time()), 0, 6)),
        'name' => $data['name'] ?? 'Jurulatih Baru',
        'email' => $userEmail,
        'password' => '123456',
        'role' => 'coach',
        'phone' => $data['phone'] ?? '',
        'club_name' => $data['clubName'] ?? 'Kelab Sukan',
        'plan' => $targetPlanKey,
        'plan_name' => $planDetails['name'],
        'max_athletes' => $planDetails['max_athletes'],
        'max_sub_coaches' => $planDetails['max_sub_coaches'] ?? 2,
        'registered_at' => date('Y-m-d H:i:s', $now),
        'subscription_start' => date('Y-m-d H:i:s', $now),
        'subscription_end' => date('Y-m-d H:i:s', $newExpiryTime),
        'status' => 'active',
        'is_trial' => false,
        'whatsapp_verified' => false
    ];
    $users[] = $newUser;
    $updatedUser = $newUser;
}

writeJsonFile($usersFile, $users);

// Simpan rekod resit pembayaran
$payments = readJsonFile($paymentsFile);
$paymentId = 'PAY-' . strtoupper(substr(md5(uniqid()), 0, 8));
$payments[] = [
    'id' => $paymentId,
    'userEmail' => $userEmail,
    'plan' => $targetPlanKey,
    'planName' => $planDetails['name'],
    'amount' => $planDetails['price'],
    'status' => 'PENDING_WHATSAPP_CONFIRMATION',
    'createdAt' => date('Y-m-d H:i:s'),
    'newExpiryDate' => $updatedUser['subscription_end']
];
writeJsonFile($paymentsFile, $payments);

// Formatkan teks WhatsApp untuk dihantar terus ke Coach Salipar Jipun
$waMessage = urlencode(
    "Salam Tuan Superadmin Psyco Time X Pro (Coach Salipar Jipun),\n\n" .
    "Saya ingin membuat bayaran/pengesahan Langganan APK Psyco Time X Pro:\n" .
    "• Email Akaun: {$userEmail}\n" .
    "• Nama Kelab: {$updatedUser['club_name']}\n" .
    "• Pakej Dipilih: {$planDetails['name']} (RM{$planDetails['price']}/Bulan)\n" .
    "• Had Kuota: {$planDetails['max_athletes']} Atlit + {$planDetails['max_sub_coaches']} Sub Coach\n" .
    "• No Rujukan Resit: {$paymentId}\n\n" .
    "Mohon semakan dan pengesahan aktifkan akses APK saya. Terima kasih!"
);

$whatsAppUrl = "https://api.whatsapp.com/send?phone=601112345678&text=" . $waMessage;

echo json_encode([
    'status' => 'success',
    'message' => "Permohonan langganan {$planDetails['name']} berjaya direkodkan!",
    'receiptId' => $paymentId,
    'amount' => $planDetails['price'],
    'max_athletes' => $planDetails['max_athletes'],
    'max_sub_coaches' => $planDetails['max_sub_coaches'],
    'new_expiry_date' => $updatedUser['subscription_end'],
    'whatsapp_url' => $whatsAppUrl
], JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE);
