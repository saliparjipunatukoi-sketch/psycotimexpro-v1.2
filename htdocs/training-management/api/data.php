<?php
/**
 * Psyco Time X Pro - Data Query & Public Reports API Endpoint
 * Lokasi: htdocs/training-management/api/data.php
 * Menyediakan maklumat atlit, rekod perlumbaan, dan senarai video untuk dipaparkan di web atau APK
 */

require_once __DIR__ . '/db.php';

header('Content-Type: application/json; charset=utf-8');

$action = $_GET['action'] ?? 'dashboard';
$email = trim($_GET['email'] ?? '');

$allAthletes = readJsonFile($athletesFile);
$allRaces = readJsonFile($racesFile);
$allUsers = readJsonFile($usersFile);
$allPayments = readJsonFile($paymentsFile);

// Filter mengikut email jika bukan superadmin
$isSuperAdmin = (strtolower($email) === 'saliparjipun.atukoi@gmail.com');

$filteredAthletes = $allAthletes;
$filteredRaces = $allRaces;

if (!empty($email) && !$isSuperAdmin) {
    $filteredAthletes = array_values(array_filter($allAthletes, function($a) use ($email) {
        return strtolower($a['coachEmail'] ?? '') === strtolower($email);
    }));
    $filteredRaces = array_values(array_filter($allRaces, function($r) use ($email) {
        return strtolower($r['coachEmail'] ?? '') === strtolower($email);
    }));
}

echo json_encode([
    'status' => 'success',
    'totalAthletes' => count($filteredAthletes),
    'totalRaces' => count($filteredRaces),
    'totalUsers' => count($allUsers),
    'athletes' => $filteredAthletes,
    'races' => $filteredRaces,
    'users' => $isSuperAdmin ? $allUsers : [],
    'payments' => $isSuperAdmin ? $allPayments : [],
    'plans' => $SUBSCRIPTION_PLANS,
    'serverTime' => date('Y-m-d H:i:s')
], JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE);
