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

// Pakej Langganan Rasmi Psyco Time X Pro
$SUBSCRIPTION_PLANS = [
    'trial' => [
        'name' => 'Percubaan Percuma (7 Hari)',
        'price' => 0,
        'max_athletes' => 10,
        'duration_days' => 7
    ],
    'basic' => [
        'name' => 'Pakej Asas (Basic)',
        'price' => 30,
        'max_athletes' => 25,
        'duration_days' => 30
    ],
    'pro' => [
        'name' => 'Pakej Pro',
        'price' => 40,
        'max_athletes' => 35,
        'duration_days' => 30
    ],
    'elite' => [
        'name' => 'Pakej Elite',
        'price' => 50,
        'max_athletes' => 50,
        'duration_days' => 30
    ],
    'unlimited' => [
        'name' => 'Pakej Sekolah / Majlis Sukan',
        'price' => 80,
        'max_athletes' => 120,
        'duration_days' => 30
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
        return null; // Return null if MySQL offline/unconfigured
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

// Inisialisasi User Default jika fail kosong
if (!file_exists($usersFile) || count(readJsonFile($usersFile)) === 0) {
    $now = time();
    $initialUsers = [
        [
            'id' => 'USR-001',
            'name' => 'Coach Salipar Jipun',
            'email' => 'saliparjipun.atukoi@gmail.com',
            'password' => 'admin123',
            'role' => 'superadmin',
            'phone' => '601112345678',
            'club_name' => 'Psyco Track & Field Elite Malaysia',
            'plan' => 'unlimited',
            'plan_name' => 'Superadmin Unlimited',
            'max_athletes' => 9999,
            'registered_at' => date('Y-m-d H:i:s', $now),
            'subscription_start' => date('Y-m-d H:i:s', $now),
            'subscription_end' => date('Y-m-d H:i:s', $now + (365 * 86400)),
            'status' => 'active',
            'is_trial' => false,
            'whatsapp_verified' => true
        ],
        [
            'id' => 'USR-002',
            'name' => 'Jurulatih Percubaan (Demo Club)',
            'email' => 'coach.demo@gmail.com',
            'password' => 'demo123',
            'role' => 'coach',
            'phone' => '60123456789',
            'club_name' => 'Kelab Olahraga Gemilang',
            'plan' => 'trial',
            'plan_name' => 'Percubaan Percuma 7 Hari',
            'max_athletes' => 10,
            'registered_at' => date('Y-m-d H:i:s', $now),
            'subscription_start' => date('Y-m-d H:i:s', $now),
            'subscription_end' => date('Y-m-d H:i:s', $now + (7 * 86400)),
            'status' => 'active',
            'is_trial' => true,
            'whatsapp_verified' => false
        ]
    ];
    writeJsonFile($usersFile, $initialUsers);
}

// Inisialisasi Atlit Demo jika kosong
if (!file_exists($athletesFile) || count(readJsonFile($athletesFile)) === 0) {
    $initialAthletes = [
        [
            'id' => 'ATH-001',
            'name' => 'Muhammad Azeem Fahmi',
            'category' => 'Track',
            'specificEvent' => '100m Sprint',
            'pbValue' => '10.09',
            'pbUnit' => 'Saat (s)',
            'gender' => 'Lelaki',
            'dateOfBirth' => '2004-04-29',
            'clubName' => 'Psyco Track & Field Elite Malaysia',
            'coachEmail' => 'saliparjipun.atukoi@gmail.com',
            'registrationFee' => 50,
            'feePaidStatus' => true,
            'photoUri' => '',
            'notes' => 'Sasaran Kelayakan Olimpik & Kejohanan Asia',
            'syncedAt' => date('Y-m-d H:i:s')
        ],
        [
            'id' => 'ATH-002',
            'name' => 'Shereen Samson Vallabouy',
            'category' => 'Track',
            'specificEvent' => '400m',
            'pbValue' => '51.80',
            'pbUnit' => 'Saat (s)',
            'gender' => 'Perempuan',
            'dateOfBirth' => '1998-07-10',
            'clubName' => 'Psyco Track & Field Elite Malaysia',
            'coachEmail' => 'saliparjipun.atukoi@gmail.com',
            'registrationFee' => 50,
            'feePaidStatus' => true,
            'photoUri' => '',
            'notes' => 'Pemegang Rekod Kebangsaan 400m',
            'syncedAt' => date('Y-m-d H:i:s')
        ],
        [
            'id' => 'ATH-003',
            'name' => 'Andre Anura Anuar',
            'category' => 'Field',
            'specificEvent' => 'Lompat Jauh',
            'pbValue' => '8.02',
            'pbUnit' => 'Meter (Jarak - Lompat Jauh/Kijang)',
            'gender' => 'Lelaki',
            'dateOfBirth' => '1999-06-12',
            'clubName' => 'Psyco Track & Field Elite Malaysia',
            'coachEmail' => 'saliparjipun.atukoi@gmail.com',
            'registrationFee' => 50,
            'feePaidStatus' => true,
            'photoUri' => '',
            'notes' => 'Sasaran Emas Sukan SEA',
            'syncedAt' => date('Y-m-d H:i:s')
        ]
    ];
    writeJsonFile($athletesFile, $initialAthletes);
}

// Inisialisasi Rekod Race Demo jika kosong
if (!file_exists($racesFile) || count(readJsonFile($racesFile)) === 0) {
    $initialRaces = [
        [
            'id' => 'RACE-101',
            'title' => 'Race 1 - Saringan 100m',
            'eventName' => '100m Sprint',
            'athleteName' => 'Muhammad Azeem Fahmi',
            'recordedTimeMillis' => 10180,
            'formattedTime' => '0:00:10:180',
            'windReading' => '+1.2 m/s',
            'lane' => 4,
            'coachEmail' => 'saliparjipun.atukoi@gmail.com',
            'cam1VideoUri' => 'videos/CAM1_Race1_Demo.mp4',
            'cam2VideoUri' => 'videos/CAM2_Race1_Torso.mp4',
            'photoFinishUri' => 'photofinish/PhotoFinish_Race1.png',
            'cadenceSpM' => 268,
            'groundContactTimeMs' => 96,
            'torsoLeanAngleDeg' => 15.2,
            'notes' => 'Finisher Torso Lean tepat pada garisan penamat Cam 2.',
            'createdAt' => date('Y-m-d H:i:s')
        ]
    ];
    writeJsonFile($racesFile, $initialRaces);
}
