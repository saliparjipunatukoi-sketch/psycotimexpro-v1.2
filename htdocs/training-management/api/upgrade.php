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

$adminWhatsApp = "601112345678"; // Nombor WhatsApp Admin rasmi

foreach ($users as $idx => $u) {
    if (strtolower($u['email']) === strtolower($userEmail)) {
        $userFound = true;
        
        // PENTING: Pengiraan Tarikh Luput Baru (Ganjar ke hadapan mengikut tarikh upgrade penuh)
        $now = time();
        $currentExpiry = strtotime($u['subscription_end'] ?? 'now');
        
        // Jika belum tamat tempoh, lanjutkan dari tarikh sekarang + 30 hari penuh
        $newExpiryTime = $now + (30 * 86400);

        $users[$idx]['plan'] = $targetPlanKey;
        $users[$idx]['plan_name'] = $planDetails['name'];
        $users[$idx]['max_athletes'] = $planDetails['max_athletes'];
        $users[$idx]['subscription_start'] = date('Y-m-d H:i:s', $now);
        $users[$idx]['subscription_end'] = date('Y-m-d H:i:s', $newExpiryTime);
        $users[$idx]['is_trial'] = false;
        $users[$idx]['status'] = 'active';

        $updatedUser = $users[$idx];
        break;
    }
}

if (!$userFound) {
    echo json_encode([
        'status' => 'error',
        'message' => 'Pengguna tidak ditemui. Sila daftar terlebih dahulu.'
    ]);
    exit;
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
    "• Kuota Atlit: {$planDetails['max_athletes']} Orang\n" .
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
    'new_expiry_date' => $updatedUser['subscription_end'],
    'whatsapp_url' => $whatsAppUrl
], JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE);
