<?php
/**
 * Psyco Time X Pro - Payment Gateway Callback & Webhook Handler
 * Lokasi: htdocs/training-management/api/payment-webhook.php
 *
 * Disediakan khas untuk integrasi Antigravity Payment Gateway:
 * - Malaysia (MYR): ToyyibPay Callback (POST/GET: status_id, billcode, order_id, msg)
 *   Kadar: RM30, RM40, RM60, RM80
 * - Antarabangsa (USD): Stripe / PayPal Webhook (JSON Payload)
 *   Kadar: USD $7, USD $10, USD $15, USD $20
 *
 * Apabila pembayaran disahkan berjaya (paid/status_id=1), fail ini akan
 * mengaktifkan akaun jurulatih di dalam data/users.json secara automatik
 * dan melanjutkan tarikh luput (subscription_end) mengikut tempoh pakej (30/90/365 hari).
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

$rawInput = file_get_contents('php://input');
$jsonBody = json_decode($rawInput, true);
$payload = is_array($jsonBody) ? array_merge($_REQUEST, $jsonBody) : $_REQUEST;

// 1. Kesan parameter daripada ToyyibPay atau Stripe/PayPal/Custom Gateway
$statusId   = isset($payload['status_id']) ? (string)$payload['status_id'] : (isset($payload['status']) ? (string)$payload['status'] : '');
$billCode   = trim($payload['billcode'] ?? ($payload['transaction_id'] ?? ''));
$orderId    = trim($payload['order_id'] ?? ($payload['reference_id'] ?? ''));
$email      = strtolower(trim($payload['email'] ?? ($payload['customer_email'] ?? '')));
$planKey    = trim($payload['plan'] ?? ($payload['targetPlan'] ?? 'plan_rm30'));
$days       = (int)($payload['days'] ?? 30);
$amountPaid = $payload['amount'] ?? '';
$currency   = strtoupper(trim($payload['currency'] ?? 'MYR'));

// Jika order_id mengandungi format PSYCO|email|planKey|days
if (strpos($orderId, '|') !== false) {
    $parts = explode('|', $orderId);
    if (empty($email) && !empty($parts[1])) $email = strtolower(trim($parts[1]));
    if (!empty($parts[2])) $planKey = trim($parts[2]);
    if (!empty($parts[3])) $days = (int)$parts[3];
}

// Normalize planKey
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
$normalizedPlan = $planAliases[$planKey] ?? 'plan_rm30';
if ($days <= 0) $days = 30;

// Semak sama ada transaksi berjaya:
// ToyyibPay: status_id == '1' (Success)
// Stripe/PayPal/Generic: status === 'paid' || status === 'success' || status === 'completed'
$isPaid = ($statusId === '1' || strtolower($statusId) === 'paid' || strtolower($statusId) === 'success' || strtolower($statusId) === 'completed');

// Rekod log pembayaran ke dalam data/payments.json
$payments = readJsonFile($paymentsFile);
$paymentRecord = [
    'id'             => 'PAY-' . strtoupper(substr(md5(uniqid('', true)), 0, 8)),
    'order_id'       => $orderId,
    'billcode'       => $billCode,
    'email'          => $email,
    'plan'           => $normalizedPlan,
    'days'           => $days,
    'currency'       => $currency,
    'amount'         => $amountPaid,
    'status'         => $isPaid ? 'paid' : ('failed_or_pending_' . $statusId),
    'gateway_Source' => !empty($payload['billcode']) ? 'ToyyibPay' : ($payload['gateway'] ?? 'Webhook'),
    'updated_at'     => date('Y-m-d H:i:s')
];
$payments[] = $paymentRecord;
writeJsonFile($paymentsFile, $payments);

if (!$isPaid) {
    echo json_encode([
        'status'  => 'pending_or_failed',
        'message' => 'Pembayaran belum disahkan berjaya. Sila hubungi Admin +6019-5326399 jika berlaku sebarang ralat.',
        'record'  => $paymentRecord
    ], JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE);
    exit;
}

if (empty($email)) {
    echo json_encode([
        'status'  => 'error',
        'message' => 'Parameter email pengguna tiada dalam callback pembayaran.'
    ]);
    exit;
}

// 2. Kemas kini status langganan pengguna di dalam data/users.json
$users = readJsonFile($usersFile);
$found = false;
$newExpFormatted = '';

foreach ($users as $idx => $u) {
    if (strtolower($u['email'] ?? '') === $email) {
        $found = true;
        $currentExp = strtotime($u['subscription_end'] ?? 'now');
        $baseTime = ($currentExp > time()) ? $currentExp : time();
        $newExp = $baseTime + ($days * 86400);
        $newExpFormatted = date('Y-m-d H:i:s', $newExp);

        $users[$idx]['status'] = 'active';
        $users[$idx]['is_trial'] = false;
        $users[$idx]['plan'] = $normalizedPlan;
        if (isset($SUBSCRIPTION_PLANS[$normalizedPlan])) {
            $users[$idx]['plan_name'] = $SUBSCRIPTION_PLANS[$normalizedPlan]['name'];
            $users[$idx]['max_athletes'] = $SUBSCRIPTION_PLANS[$normalizedPlan]['max_athletes'];
            $users[$idx]['max_sub_coaches'] = $SUBSCRIPTION_PLANS[$normalizedPlan]['max_sub_coaches'];
        }
        $users[$idx]['subscription_end'] = $newExpFormatted;
        $users[$idx]['last_payment_at'] = date('Y-m-d H:i:s');
        $users[$idx]['last_billcode'] = $billCode;
        break;
    }
}

// Jika pengguna membayar terus sebelum mendaftar, cipta rekod aktif automatik
if (!$found) {
    $now = time();
    $newExpFormatted = date('Y-m-d H:i:s', $now + ($days * 86400));
    $planMeta = $SUBSCRIPTION_PLANS[$normalizedPlan] ?? $SUBSCRIPTION_PLANS['plan_rm30'];
    $users[] = [
        'id'                 => 'USR-' . strtoupper(substr(md5($email . $now), 0, 6)),
        'name'               => $payload['name'] ?? ('Jurulatih (' . substr($email, 0, strpos($email, '@')) . ')'),
        'email'              => $email,
        'role'               => 'coach',
        'phone'              => $payload['phone'] ?? '',
        'country'            => $payload['country'] ?? ($currency === 'MYR' ? 'Malaysia' : 'International'),
        'currency'           => $currency,
        'club_name'          => $payload['club'] ?? 'Kelab Olahraga',
        'plan'               => $normalizedPlan,
        'plan_name'          => $planMeta['name'],
        'max_athletes'       => $planMeta['max_athletes'],
        'max_sub_coaches'    => $planMeta['max_sub_coaches'],
        'registered_at'      => date('Y-m-d H:i:s', $now),
        'subscription_start' => date('Y-m-d H:i:s', $now),
        'subscription_end'   => $newExpFormatted,
        'status'             => 'active',
        'is_trial'           => false,
        'last_payment_at'    => date('Y-m-d H:i:s', $now),
        'last_billcode'      => $billCode
    ];
}

writeJsonFile($usersFile, $users);

// Jika dipanggil menerusi redirect pelayar (GET return_url daripada ToyyibPay), halakan ke dashboard
if ($_SERVER['REQUEST_METHOD'] === 'GET' && !empty($_GET['redirect_dashboard'])) {
    header('Location: ../dashboard.html?email=' . urlencode($email) . '&payment=success');
    exit;
}

echo json_encode([
    'status'           => 'success',
    'message'          => "Pembayaran disahkan! Langganan {$email} telah diaktifkan sehingga {$newExpFormatted}.",
    'email'            => $email,
    'plan'             => $normalizedPlan,
    'subscription_end' => $newExpFormatted,
    'admin_whatsapp'   => '+6019-5326399'
], JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE);
