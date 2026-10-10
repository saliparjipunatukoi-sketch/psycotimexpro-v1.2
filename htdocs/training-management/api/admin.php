<?php
/**
 * Psyco Time X Pro - Superadmin Quick Action API Endpoint
 * Membenarkan Superadmin (Coach Salipar Jipun) mengedit profil user,
 * mengubah tarikh luput (subscription_end), kuota atlit, kuota sub coach,
 * pakej langganan, dan status aktif terus dari portal web.
 */

require_once __DIR__ . '/db.php';

header('Content-Type: application/json; charset=utf-8');

$input = file_get_contents('php://input');
$data = json_decode($input, true);

if (!$data) {
    echo json_encode(['status' => 'error', 'message' => 'Data tidak sah.']);
    exit;
}

$adminEmail = trim($data['adminEmail'] ?? '');
if (strtolower($adminEmail) !== 'saliparjipun.atukoi@gmail.com') {
    echo json_encode(['status' => 'error', 'message' => 'Akses ditolak: Hanya Superadmin (Coach Salipar Jipun) dibenarkan membuat perubahan ini.']);
    exit;
}

$action = $data['action'] ?? '';
$users = readJsonFile($usersFile);

if ($action === 'update_user') {
    $targetEmail = trim($data['targetEmail'] ?? '');
    $found = false;

    foreach ($users as $idx => $u) {
        if (strtolower($u['email']) === strtolower($targetEmail)) {
            $found = true;
            if (isset($data['name'])) $users[$idx]['name'] = trim($data['name']);
            if (isset($data['club_name'])) $users[$idx]['club_name'] = trim($data['club_name']);
            if (isset($data['phone'])) $users[$idx]['phone'] = trim($data['phone']);
            if (isset($data['plan'])) {
                $p = trim($data['plan']);
                $users[$idx]['plan'] = $p;
                if (isset($SUBSCRIPTION_PLANS[$p])) {
                    $users[$idx]['plan_name'] = $SUBSCRIPTION_PLANS[$p]['name'];
                    if (!isset($data['max_athletes'])) {
                        $users[$idx]['max_athletes'] = $SUBSCRIPTION_PLANS[$p]['max_athletes'];
                    }
                    if (!isset($data['max_sub_coaches'])) {
                        $users[$idx]['max_sub_coaches'] = $SUBSCRIPTION_PLANS[$p]['max_sub_coaches'];
                    }
                }
            }
            if (isset($data['max_athletes'])) $users[$idx]['max_athletes'] = (int)$data['max_athletes'];
            if (isset($data['max_sub_coaches'])) $users[$idx]['max_sub_coaches'] = (int)$data['max_sub_coaches'];
            if (isset($data['subscription_end'])) $users[$idx]['subscription_end'] = trim($data['subscription_end']);
            if (isset($data['status'])) $users[$idx]['status'] = trim($data['status']);
            break;
        }
    }

    if ($found) {
        writeJsonFile($usersFile, $users);
        echo json_encode(['status' => 'success', 'message' => "Akaun {$targetEmail} berjaya dikemaskini oleh Admin!"]);
    } else {
        echo json_encode(['status' => 'error', 'message' => 'Pengguna tidak ditemui.']);
    }
    exit;
}

if ($action === 'extend_subscription') {
    $targetEmail = trim($data['targetEmail'] ?? '');
    $days = (int)($data['days'] ?? 30);
    $found = false;

    foreach ($users as $idx => $u) {
        if (strtolower($u['email']) === strtolower($targetEmail)) {
            $found = true;
            $currentExp = strtotime($users[$idx]['subscription_end'] ?? 'now');
            $base = ($currentExp > time()) ? $currentExp : time();
            $newExp = $base + ($days * 86400);
            $users[$idx]['subscription_end'] = date('Y-m-d H:i:s', $newExp);
            $users[$idx]['status'] = 'active';
            break;
        }
    }

    if ($found) {
        writeJsonFile($usersFile, $users);
        echo json_encode([
            'status' => 'success', 
            'message' => "Langganan {$targetEmail} berjaya dilanjutkan sebanyak {$days} hari sehingga " . date('d M Y', $newExp) . "!"
        ]);
    } else {
        echo json_encode(['status' => 'error', 'message' => 'Pengguna tidak ditemui.']);
    }
    exit;
}

if ($action === 'delete_user') {
    $targetEmail = trim($data['targetEmail'] ?? '');
    if (strtolower($targetEmail) === 'saliparjipun.atukoi@gmail.com') {
        echo json_encode(['status' => 'error', 'message' => 'Akaun Superadmin tidak boleh dipadam.']);
        exit;
    }
    $users = array_values(array_filter($users, function($u) use ($targetEmail) {
        return strtolower($u['email']) !== strtolower($targetEmail);
    }));
    writeJsonFile($usersFile, $users);
    echo json_encode(['status' => 'success', 'message' => "Pengguna {$targetEmail} berjaya dipadam."]);
    exit;
}

echo json_encode(['status' => 'error', 'message' => 'Tindakan tidak sah.']);
