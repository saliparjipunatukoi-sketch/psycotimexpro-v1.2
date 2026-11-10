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
$rawPlanKey = trim($data['targetPlan'] ?? '');
$paymentMethod = trim($data['paymentMethod'] ?? 'WhatsApp Admin');
$currency = strtoupper(trim($data['currency'] ?? 'MYR'));
$days = (int)($data['days'] ?? 30);
if ($days <= 0) $days = 30;

$planAliases = [
    'basic'     => 'plan_rm30',
    'standard'  => 'plan_rm40',
    'pro'       => 'plan_rm60',
    'elite'     => 'plan_rm80',
    'plan_rm30' => 'plan_rm30',
    'plan_rm40' => 'plan_rm40',
    'plan_rm60' => 'plan_rm60',
    'plan_rm80' => 'plan_rm80'
];
$targetPlanKey = $planAliases[$rawPlanKey] ?? $rawPlanKey;

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
$adminWhatsApp = "60195326399"; // Nombor WhatsApp Admin rasmi (+6019-5326399)
$defaultDisplayPrice = ($currency === 'USD')
    ? ('USD $' . ($planDetails['price_usd'] ?? 7))
    : ('RM' . $planDetails['price']);
$displayPrice = trim($data['displayPrice'] ?? $defaultDisplayPrice);

foreach ($users as $idx => $u) {
    if (strtolower($u['email']) === strtolower($userEmail)) {
        $userFound = true;
        
        $now = time();
        $currentExpiry = strtotime($u['subscription_end'] ?? 'now');
        $base = ($currentExpiry > $now) ? $currentExpiry : $now;
        $newExpiryTime = $base + ($days * 86400);

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
    $newExpiryTime = $now + ($days * 86400);
    $newUser = [
        'id' => 'USR-' . strtoupper(substr(md5($userEmail . time()), 0, 6)),
        'name' => $data['name'] ?? 'Jurulatih Baru',
        'email' => $userEmail,
        'password' => '123456',
        'role' => 'coach',
        'phone' => $data['phone'] ?? '',
        'country' => $data['country'] ?? ($currency === 'USD' ? 'International' : 'Malaysia'),
        'currency' => $currency,
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

// Simpan rekod resit pembayaran & rujukan Order ID untuk Payment Gateway (ToyyibPay / Stripe / PayPal)
$payments = readJsonFile($paymentsFile);
$paymentId = 'PAY-' . strtoupper(substr(md5(uniqid()), 0, 8));
$gatewayOrderId = "PSYCO|{$userEmail}|{$targetPlanKey}|{$days}";
$payments[] = [
    'id' => $paymentId,
    'order_id' => $gatewayOrderId,
    'userEmail' => $userEmail,
    'plan' => $targetPlanKey,
    'planName' => $planDetails['name'],
    'currency' => $currency,
    'amount' => $displayPrice,
    'amount_myr' => $planDetails['price'],
    'amount_usd' => $planDetails['price_usd'] ?? 7,
    'status' => 'PENDING_CONFIRMATION',
    'createdAt' => date('Y-m-d H:i:s'),
    'newExpiryDate' => $updatedUser['subscription_end']
];
writeJsonFile($paymentsFile, $payments);

// Formatkan teks WhatsApp untuk dihantar terus ke Admin (+6019-5326399)
$waMessage = urlencode(
    "Salam Admin Psyco Time X Pro (+6019-5326399),\n\n" .
    "Saya ingin membuat bayaran/pengesahan Langganan APK Psyco Time X Pro:\n" .
    "• Email Akaun: {$userEmail}\n" .
    "• Nama Kelab: {$updatedUser['club_name']}\n" .
    "• Pakej Dipilih: {$planDetails['name']} ({$displayPrice}/Bulan)\n" .
    "• Had Kuota: {$planDetails['max_athletes']} Atlit + {$planDetails['max_sub_coaches']} Sub Coach\n" .
    "• No Rujukan Resit: {$paymentId}\n\n" .
    "Mohon semakan dan pengesahan aktifkan akses APK saya. Terima kasih!"
);

$whatsAppUrl = "https://wa.me/{$adminWhatsApp}?text=" . $waMessage;

echo json_encode([
    'status' => 'success',
    'message' => "Permohonan langganan {$planDetails['name']} berjaya direkodkan!",
    'receiptId' => $paymentId,
    'order_id' => $gatewayOrderId,
    'currency' => $currency,
    'display_price' => $displayPrice,
    'amount_myr' => $planDetails['price'],
    'amount_usd' => $planDetails['price_usd'] ?? 7,
    'max_athletes' => $planDetails['max_athletes'],
    'max_sub_coaches' => $planDetails['max_sub_coaches'],
    'new_expiry_date' => $updatedUser['subscription_end'],
    'webhook_endpoint' => 'api/payment-webhook.php',
    'whatsapp_url' => $whatsAppUrl
], JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE);
