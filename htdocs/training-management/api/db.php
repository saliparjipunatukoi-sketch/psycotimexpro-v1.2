<?php
/**
 * Psyco Time X Pro - Training Management Central Database & Subscription Helper
 * Lokasi: htdocs/training-management/api/db.php
 * Menyokong kedua-dua MySQL (PDO) dan Fallback JSON automatik tanpa ralat.
 */

require_once __DIR__ . '/config.php';

header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS, PUT, DELETE');
header('Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

$dataDir = __DIR__ . '/../data';
if (!file_exists($dataDir)) {
    mkdir($dataDir, 0777, true);
}

$uploadsDir = __DIR__ . '/uploads';
$videosDir = __DIR__ . '/videos';
$photosDir = __DIR__ . '/photofinish';
if (!file_exists($uploadsDir)) mkdir($uploadsDir, 0777, true);
if (!file_exists($videosDir)) mkdir($videosDir, 0777, true);
if (!file_exists($photosDir)) mkdir($photosDir, 0777, true);

// Initialize JSON database files if not exist
$usersFile = $dataDir . '/users.json';
$athletesFile = $dataDir . '/athletes.json';
$racesFile = $dataDir . '/races.json';
$paymentsFile = $dataDir . '/payments.json';
$feesFile = $dataDir . '/fees.json';
$planFeaturesFile = $dataDir . '/plan_features.json';

// Pakej Langganan Rasmi Psyco Time X Pro Mengikut Kiraan Terbaharu (Dikurangkan 10 Atlit Setiap Pakej)
// PELAN RM30 = 15 Orang Atlit (1 Bulan) + 1 Sub Coach / Urusetia
// PELAN RM40 = 30 Orang Atlit (1 Bulan) + 2 Sub Coach / Urusetia
// PELAN RM60 = 50 Orang Atlit (1 Bulan) + 3 Sub Coach / Urusetia
// PELAN RM80 = Unlimited Atlit (1 Bulan) + 5 Sub Coach / Urusetia
$SUBSCRIPTION_PLANS = [
    'trial' => [
        'name' => 'Percubaan Percuma (7 Hari)',
        'price' => 0,
        'price_usd' => 0,
        'max_athletes' => 10,
        'max_sub_coaches' => 1,
        'duration_days' => 7,
        'description' => '10 Orang Atlit (7 Hari) + 1 Sub Coach',
        'features' => [
            'Cam 1 + Cam 2 Torso Capture',
            'Auto-Sync Web Portal',
            'Kad Teknikal & PB Atlit',
            'AI Biomekanik Larian & Padang',
            'Rakaman Video Folder Phone'
        ]
    ],
    'plan_rm30' => [
        'name' => 'PELAN RM30 (RM 30 / USD $7)',
        'price' => 30,
        'price_usd' => 7,
        'max_athletes' => 15,
        'max_sub_coaches' => 1,
        'duration_days' => 30,
        'description' => '15 Orang Atlit (1 Bulan) + 1 Sub Coach / Urusetia — Sesuai untuk jurulatih sekolah dan kelab pembangunan bakat baharu.',
        'features' => [
            'Cam 1 + Cam 2 Torso Capture',
            'Auto-Sync Web Portal',
            'Kad Teknikal & PB Atlit',
            'AI Biomekanik Larian & Padang',
            'Rakaman Video Folder Phone'
        ]
    ],
    'plan_rm40' => [
        'name' => 'PELAN RM40 (RM 40 / USD $10)',
        'price' => 40,
        'price_usd' => 10,
        'max_athletes' => 30,
        'max_sub_coaches' => 2,
        'duration_days' => 30,
        'description' => '30 Orang Atlit (1 Bulan) + 2 Sub Coach / Urusetia — Pilihan terbaik untuk skuad olahraga sekolah menengah dan kelab daerah.',
        'features' => [
            'Cam 1 + Cam 2 Torso Capture',
            'AI Biomekanik Larian & Padang',
            'Rakaman Video Folder Phone',
            'Auto-Sync Web Portal',
            'Kad Teknikal & PB Atlit'
        ]
    ],
    'plan_rm60' => [
        'name' => 'PELAN RM60 (RM 60 / USD $15)',
        'price' => 60,
        'price_usd' => 15,
        'max_athletes' => 50,
        'max_sub_coaches' => 3,
        'duration_days' => 30,
        'description' => '50 Orang Atlit (1 Bulan) + 3 Sub Coach / Urusetia — Direka khas bagi kontinjen MSSD, IPT, atau akademi berbilang jurulatih.',
        'features' => [
            'Multi-User Akses Serentak',
            'Sokongan Audio Starter Gun',
            'Pangkalan Data Web Cloud',
            'Cam 1 + Cam 2 Torso Capture',
            'AI Biomekanik Larian & Padang',
            'Rakaman Video Folder Phone'
        ]
    ],
    'plan_rm80' => [
        'name' => 'PELAN RM80 (RM 80 / USD $20)',
        'price' => 80,
        'price_usd' => 20,
        'max_athletes' => 9999,
        'max_sub_coaches' => 5,
        'duration_days' => 30,
        'description' => 'Unlimited Atlit (1 Bulan) + 5 Sub Coach / Urusetia — Kapasiti terbesar untuk kejohanan terbuka, MSSN & program sukan negeri.',
        'features' => [
            'Eksport Penuh PDF & Excel',
            'Priority Support WhatsApp',
            'Penyegerakan Pantas Tanpa Had',
            'Multi-User Akses Serentak',
            'Sokongan Audio Starter Gun',
            'Pangkalan Data Web Cloud',
            'Cam 1 + Cam 2 Torso Capture',
            'AI Biomekanik Larian & Padang',
            'Rakaman Video Folder Phone'
        ]
    ]
];

// Helper PDO Connection
function getDbConnection() {
    static $pdo = null;
    if ($pdo !== null) return $pdo;

    try {
        $dsn = "mysql:host=" . DB_HOST . ";dbname=" . DB_NAME . ";port=" . DB_PORT . ";charset=" . DB_CHARSET;
        $options = [
            PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
            PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
            PDO::ATTR_EMULATE_PREPARES => false,
        ];
        $pdo = new PDO($dsn, DB_USER, DB_PASS, $options);
        return $pdo;
    } catch (Exception $e) {
        return null;
    }
}

function readJsonFile($filePath) {
    if (!file_exists($filePath)) {
        return [];
    }
    $content = file_get_contents($filePath);
    $data = json_decode($content, true);
    return is_array($data) ? $data : [];
}

function writeJsonFile($filePath, $data) {
    file_put_contents($filePath, json_encode($data, JSON_PRETTY_PRINT | JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE));
}

// Inisialisasi User Default (Superadmin) jika fail users kosong
if (!file_exists($usersFile) || count(readJsonFile($usersFile)) === 0) {
    $now = time();
    $initialUsers = [
        [
            'id' => 'USR-001',
            'name' => 'Coach Salipar Jipun',
            'email' => 'saliparjipun.atukoi@gmail.com',
            'password' => 'admin123',
            'role' => 'superadmin',
            'phone' => '60195326399',
            'country' => 'Malaysia',
            'currency' => 'MYR',
            'club_name' => 'Psyco Track & Field Elite Malaysia',
            'plan' => 'plan_rm80',
            'plan_name' => 'Superadmin Unlimited',
            'max_athletes' => 9999,
            'max_sub_coaches' => 99,
            'registered_at' => date('Y-m-d H:i:s', $now),
            'subscription_start' => date('Y-m-d H:i:s', $now),
            'subscription_end' => date('Y-m-d H:i:s', $now + (365 * 86400)),
            'status' => 'active',
            'is_trial' => false,
            'whatsapp_verified' => true
        ]
    ];
    writeJsonFile($usersFile, $initialUsers);
}

// Inisialisasi fail kosong untuk start fresh
if (!file_exists($athletesFile)) {
    writeJsonFile($athletesFile, []);
}

if (!file_exists($racesFile)) {
    writeJsonFile($racesFile, []);
}

if (!file_exists($paymentsFile)) {
    writeJsonFile($paymentsFile, []);
}
