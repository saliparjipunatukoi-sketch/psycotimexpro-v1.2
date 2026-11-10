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
$allFees = readJsonFile($feesFile);
$customPlanFeatures = readJsonFile($planFeaturesFile);
$systemControl = readJsonFile($dataDir . '/system_control.json');

// Filter mengikut email jika bukan superadmin
$isSuperAdmin = (strtolower($email) === 'saliparjipun.atukoi@gmail.com');

$filteredAthletes = $allAthletes;
$filteredRaces = $allRaces;
$filteredFees = $allFees;

if (!empty($email) && !$isSuperAdmin) {
    $filteredAthletes = array_values(array_filter($allAthletes, function($a) use ($email) {
        return strtolower($a['coachEmail'] ?? '') === strtolower($email);
    }));
    $filteredRaces = array_values(array_filter($allRaces, function($r) use ($email) {
        return strtolower($r['coachEmail'] ?? '') === strtolower($email);
    }));
    $filteredFees = array_values(array_filter($allFees, function($f) use ($email) {
        return strtolower($f['coachEmail'] ?? '') === strtolower($email);
    }));
}

// Merge custom plan features into $SUBSCRIPTION_PLANS if overridden by Admin
foreach ($customPlanFeatures as $pKey => $feats) {
    if (isset($SUBSCRIPTION_PLANS[$pKey]) && is_array($feats)) {
        $SUBSCRIPTION_PLANS[$pKey]['features'] = $feats;
    }
}

echo json_encode([
    'status' => 'success',
    'totalAthletes' => count($filteredAthletes),
    'totalRaces' => count($filteredRaces),
    'totalFees' => count($filteredFees),
    'totalUsers' => count($allUsers),
    'athletes' => $filteredAthletes,
    'races' => $filteredRaces,
    'fees' => $filteredFees,
    'users' => $isSuperAdmin ? $allUsers : [],
    'payments' => $isSuperAdmin ? $allPayments : [],
    'plans' => $SUBSCRIPTION_PLANS,
    'planFeatures' => $customPlanFeatures,
    'systemControl' => $systemControl,
    'videoStoragePolicy' => 'LOCAL_PHONE_MEMORY_ONLY',
    'serverTime' => date('Y-m-d H:i:s')
], JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE);
