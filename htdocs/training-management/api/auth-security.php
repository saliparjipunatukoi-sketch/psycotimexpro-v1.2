<?php
/**
 * Psyco Time X Pro - Enterprise Authentication, Forgot Password (Gmail Reset Link) & Anti-Hack Security Engine
 * Lokasi: htdocs/training-management/api/auth-security.php
 *
 * Ciri Keselamatan Utama:
 * 1. Bcrypt Password Hashing (PASSWORD_BCRYPT) — tiada kata laluan disimpan dalam teks biasa.
 * 2. Brute-Force Protection & IP Rate Limiting — sekatan automatik 15 minit selepas 5 cubaan gagal.
 * 3. Forgot Password via Gmail Link — menjana token kriptografi 64-aksara (sah 30 minit) & menghantar
 *    pautan reset kata laluan ke Gmail pengguna supaya mereka boleh mencipta kata laluan baharu.
 * 4. Input Sanitization & XSS/SQLi Firewall.
 */

require_once __DIR__ . '/db.php';

header('Content-Type: application/json; charset=utf-8');
header('X-Content-Type-Options: nosniff');
header('X-Frame-Options: SAMEORIGIN');
header('X-XSS-Protection: 1; mode=block');

$resetTokensFile = $dataDir . '/reset_tokens.json';
$securityLogsFile = $dataDir . '/security_logs.json';

if (!file_exists($resetTokensFile)) writeJsonFile($resetTokensFile, []);
if (!file_exists($securityLogsFile)) writeJsonFile($securityLogsFile, []);

function sanitizeClean($val) {
    if (!is_string($val)) return '';
    $val = strip_tags(trim($val));
    $val = str_replace(['<', '>', '"', "'", ';', '--'], '', $val);
    return $val;
}

function getClientIpAddress() {
    if (!empty($_SERVER['HTTP_CF_CONNECTING_IP'])) return $_SERVER['HTTP_CF_CONNECTING_IP'];
    if (!empty($_SERVER['HTTP_X_FORWARDED_FOR'])) {
        $ips = explode(',', $_SERVER['HTTP_X_FORWARDED_FOR']);
        return trim($ips[0]);
    }
    return $_SERVER['REMOTE_ADDR'] ?? '0.0.0.0';
}

function isBruteForceLocked($securityLogsFile, $email, $ip) {
    $logs = readJsonFile($securityLogsFile);
    $now = time();
    $windowSeconds = 15 * 60; // 15 minit
    $failedCount = 0;

    foreach ($logs as $entry) {
        if (($entry['type'] ?? '') === 'login_failed' && ($now - ($entry['timestamp'] ?? 0)) < $windowSeconds) {
            if (strtolower($entry['email'] ?? '') === strtolower($email) || ($entry['ip'] ?? '') === $ip) {
                $failedCount++;
            }
        }
    }
    return $failedCount >= 5;
}

function recordSecurityEvent($securityLogsFile, $type, $email, $details = '') {
    $logs = readJsonFile($securityLogsFile);
    // Kekalkan 300 rekod terkini sahaja
    if (count($logs) > 300) {
        $logs = array_slice($logs, -250);
    }
    $logs[] = [
        'type' => $type,
        'email' => strtolower($email),
        'ip' => getClientIpAddress(),
        'user_agent' => substr($_SERVER['HTTP_USER_AGENT'] ?? 'Unknown', 0, 120),
        'details' => $details,
        'timestamp' => time(),
        'datetime' => date('Y-m-d H:i:s')
    ];
    writeJsonFile($securityLogsFile, $logs);
}

function clearFailedLoginAttempts($securityLogsFile, $email) {
    $logs = readJsonFile($securityLogsFile);
    $filtered = array_values(array_filter($logs, function($e) use ($email) {
        return !(($e['type'] ?? '') === 'login_failed' && strtolower($e['email'] ?? '') === strtolower($email));
    }));
    writeJsonFile($securityLogsFile, $filtered);
}

$input = file_get_contents('php://input');
$data = json_decode($input, true);
if (!is_array($data)) {
    $data = $_POST;
}

$action = sanitizeClean($data['action'] ?? ($_GET['action'] ?? ''));
$clientIp = getClientIpAddress();

// =========================================================================
// 1. ACTION: LOGIN ATAU DAFTAR DENGAN PENGESAHAN KATA LALUAN BCRYPT
// =========================================================================
if ($action === 'login_or_register') {
    $email = strtolower(sanitizeClean($data['email'] ?? ''));
    $password = trim($data['password'] ?? '');
    $fullName = sanitizeClean($data['name'] ?? '');
    $country = sanitizeClean($data['country'] ?? 'Malaysia');
    $phone = sanitizeClean($data['phone'] ?? '');
    $club = sanitizeClean($data['club'] ?? '');
    $cert = sanitizeClean($data['cert'] ?? 'World Athletics Level 1');

    if (empty($email) || !filter_var($email, FILTER_VALIDATE_EMAIL)) {
        echo json_encode(['status' => 'error', 'message' => 'Alamat emel Gmail tidak sah.']);
        exit;
    }

    if (isBruteForceLocked($securityLogsFile, $email, $clientIp)) {
        recordSecurityEvent($securityLogsFile, 'lockout_triggered', $email, 'Melebihi 5 cubaan kata laluan salah');
        echo json_encode([
            'status' => 'locked',
            'message' => 'PERISAI KESELAMATAN: Akaun ini dikunci sementara selama 15 minit kerana terlalu banyak cubaan kata laluan yang salah. Sila klik "Forgot Password" untuk menghantar pautan reset kata laluan ke Gmail anda.'
        ]);
        exit;
    }

    $users = readJsonFile($usersFile);
    $foundIndex = -1;
    foreach ($users as $idx => $u) {
        if (strtolower($u['email'] ?? '') === $email) {
            $foundIndex = $idx;
            break;
        }
    }

    // Jika pengguna sudah wujud: Sahkan kata laluan
    if ($foundIndex >= 0) {
        $existingUser = $users[$foundIndex];
        $storedPass = $existingUser['password'] ?? '';

        $passwordValid = false;
        if (!empty($password)) {
            if (strpos($storedPass, '$2y$') === 0 || strpos($storedPass, '$2a$') === 0) {
                $passwordValid = password_verify($password, $storedPass);
            } else {
                // Akaun lama dengan plaintext password -> sahkan & naik taraf ke Bcrypt secara automatik!
                if ($storedPass === $password || empty($storedPass)) {
                    $passwordValid = true;
                    $users[$foundIndex]['password'] = password_hash($password, PASSWORD_BCRYPT);
                }
            }
        }

        if (!$passwordValid) {
            recordSecurityEvent($securityLogsFile, 'login_failed', $email, 'Kata laluan tidak sepadan');
            echo json_encode([
                'status' => 'error',
                'message' => 'Kata laluan yang dimasukkan tidak tepat. Sekiranya anda terlupa kata laluan, sila tekan butang "Forgot Password?" untuk menerima pautan kata laluan baharu di Gmail anda.'
            ]);
            exit;
        }

        clearFailedLoginAttempts($securityLogsFile, $email);
        if (!empty($fullName)) $users[$foundIndex]['name'] = $fullName;
        if (!empty($phone)) $users[$foundIndex]['phone'] = $phone;
        if (!empty($club)) $users[$foundIndex]['club_name'] = $club;
        if (!empty($country)) {
            $users[$foundIndex]['country'] = $country;
            $users[$foundIndex]['currency'] = (strtolower($country) === 'malaysia') ? 'MYR' : 'USD';
        }
        if (!empty($cert)) $users[$foundIndex]['coach_certification'] = $cert;
        $users[$foundIndex]['last_login_at'] = date('Y-m-d H:i:s');
        $users[$foundIndex]['last_login_ip'] = $clientIp;
        writeJsonFile($usersFile, $users);

        recordSecurityEvent($securityLogsFile, 'login_success', $email, 'Log masuk disahkan Bcrypt');
        $sessionToken = hash_hmac('sha256', $email . '|' . date('Y-m-d'), 'PSYCO_TIMEX_PRO_2026_SECRET');

        echo json_encode([
            'status' => 'success',
            'message' => 'Log masuk berjaya disahkan dengan enkripsi Bcrypt!',
            'sessionToken' => $sessionToken,
            'user' => [
                'email' => $email,
                'name' => $users[$foundIndex]['name'] ?? '',
                'club_name' => $users[$foundIndex]['club_name'] ?? '',
                'country' => $users[$foundIndex]['country'] ?? 'Malaysia',
                'currency' => $users[$foundIndex]['currency'] ?? 'MYR',
                'plan' => $users[$foundIndex]['plan'] ?? 'trial',
                'subscription_end' => $users[$foundIndex]['subscription_end'] ?? ''
            ]
        ]);
        exit;
    }

    // Jika pengguna baharu: Daftarkan dengan Bcrypt hash & 7 Hari Percubaan Percuma
    if (strlen($password) < 6) {
        echo json_encode([
            'status' => 'error',
            'message' => 'Kata laluan mestilah sekurang-kurangnya 6 aksara untuk keselamatan akaun anda.'
        ]);
        exit;
    }

    $now = time();
    $currencyVal = (strtolower($country) === 'malaysia') ? 'MYR' : 'USD';
    $newUser = [
        'id' => 'USR-' . strtoupper(substr(md5($email . $now), 0, 6)),
        'name' => !empty($fullName) ? $fullName : ('Coach (' . substr($email, 0, strpos($email, '@')) . ')'),
        'email' => $email,
        'password' => password_hash($password, PASSWORD_BCRYPT),
        'role' => ($email === 'saliparjipun.atukoi@gmail.com') ? 'superadmin' : 'coach',
        'country' => $country,
        'currency' => $currencyVal,
        'phone' => $phone,
        'club_name' => !empty($club) ? $club : 'Kelab Olahraga',
        'coach_certification' => $cert,
        'plan' => 'trial',
        'plan_name' => 'Percubaan Percuma (7 Hari)',
        'max_athletes' => 15,
        'max_sub_coaches' => 1,
        'registered_at' => date('Y-m-d H:i:s', $now),
        'subscription_start' => date('Y-m-d H:i:s', $now),
        'subscription_end' => date('Y-m-d H:i:s', $now + (7 * 86400)),
        'status' => 'active',
        'is_trial' => true,
        'whatsapp_verified' => false
    ];
    $users[] = $newUser;
    writeJsonFile($usersFile, $users);
    recordSecurityEvent($securityLogsFile, 'register_success', $email, 'Pendaftaran baharu dengan Bcrypt');

    $sessionToken = hash_hmac('sha256', $email . '|' . date('Y-m-d'), 'PSYCO_TIMEX_PRO_2026_SECRET');
    echo json_encode([
        'status' => 'success',
        'message' => 'Akaun berjaya didaftarkan dengan keselamatan Bcrypt & 7 Hari Percubaan Percuma!',
        'sessionToken' => $sessionToken,
        'user' => [
            'email' => $email,
            'name' => $newUser['name'],
            'club_name' => $newUser['club_name'],
            'country' => $newUser['country'],
            'currency' => $newUser['currency'],
            'plan' => $newUser['plan'],
            'subscription_end' => $newUser['subscription_end']
        ]
    ]);
    exit;
}

// =========================================================================
// 2. ACTION: FORGOT PASSWORD — HANTAR LINK RESET KATA LALUAN KE GMAIL
// =========================================================================
if ($action === 'forgot_password') {
    $email = strtolower(sanitizeClean($data['email'] ?? ''));
    if (empty($email) || !filter_var($email, FILTER_VALIDATE_EMAIL)) {
        echo json_encode([
            'status' => 'error',
            'message' => 'Sila masukkan alamat emel Gmail yang sah.'
        ]);
        exit;
    }

    // Jana token kriptografi 64-aksara yang selamat
    try {
        $rawToken = bin2hex(random_bytes(32));
    } catch (Exception $e) {
        $rawToken = hash('sha256', uniqid($email, true) . microtime(true));
    }
    $tokenHash = hash('sha256', $rawToken);
    $expiresAt = time() + (30 * 60); // Sah selama 30 minit

    $tokens = readJsonFile($resetTokensFile);
    // Buang token lama bagi emel ini
    $tokens = array_values(array_filter($tokens, function($t) use ($email) {
        return strtolower($t['email'] ?? '') !== $email && ($t['expires_at'] ?? 0) > time();
    }));

    $tokens[] = [
        'email' => $email,
        'token_hash' => $tokenHash,
        'created_at' => date('Y-m-d H:i:s'),
        'expires_at' => $expiresAt,
        'ip' => $clientIp
    ];
    writeJsonFile($resetTokensFile, $tokens);

    $protocol = (!empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off') ? 'https' : 'https';
    $host = $_SERVER['HTTP_HOST'] ?? 'www.psycotimexpro.my';
    $resetLink = "{$protocol}://{$host}/training-management/reset-password.php?email=" . urlencode($email) . "&token=" . urlencode($rawToken);

    // Hantar Emel HTML Rasmi ke Gmail pengguna
    $subject = "Reset Kata Laluan Anda - Psyco Time X Pro (Training Management)";
    $headers = "MIME-Version: 1.0\r\n";
    $headers .= "Content-type:text/html;charset=UTF-8\r\n";
    $headers .= "From: Psyco Time X Pro Security <noreply@psycotimexpro.my>\r\n";
    $headers .= "Reply-To: saliparjipun.atukoi@gmail.com\r\n";

    $emailHtml = "
    <div style='font-family:Arial,sans-serif;max-width:560px;margin:0 auto;background:#0A0F1D;color:#ffffff;padding:28px;border-radius:16px;border:1px solid #06B6D4;'>
        <h2 style='color:#06B6D4;margin-top:0;'>PSYCO TIME X PRO — RESET KATA LALUAN</h2>
        <p style='color:#E5E7EB;font-size:14px;line-height:1.6;'>
            Salam Jurulatih,<br><br>
            Kami menerima permintaan untuk menetapkan semula kata laluan (Forgot Password) bagi akaun Gmail anda: <strong>{$email}</strong>.
        </p>
        <p style='margin:24px 0;'>
            <a href='{$resetLink}' style='background:#06B6D4;color:#050811;padding:12px 24px;border-radius:10px;text-decoration:none;font-weight:bold;display:inline-block;'>
                Cipta Kata Laluan Baharu Sekarang
            </a>
        </p>
        <p style='color:#9CA3AF;font-size:12px;line-height:1.5;'>
            Pautan ini sah selama <strong>30 minit</strong>. Jika butang di atas tidak boleh diklik, salin dan buka pautan ini di pelayar anda:<br>
            <a href='{$resetLink}' style='color:#F97316;word-break:break-all;'>{$resetLink}</a>
        </p>
        <hr style='border:0;border-top:1px solid #1F2937;margin:20px 0;'>
        <p style='color:#6B7280;font-size:11px;'>
            Jika anda tidak meminta penukaran kata laluan ini, sila abaikan emel ini. Bantuan Admin WhatsApp: +6019-5326399.
        </p>
    </div>";

    $mailDispatched = @mail($email, $subject, $emailHtml, $headers);
    recordSecurityEvent($securityLogsFile, 'forgot_password_requested', $email, 'Reset link dijana & dihantar ke Gmail');

    echo json_encode([
        'status' => 'success',
        'mailDispatched' => (bool)$mailDispatched,
        'resetUrl' => $resetLink,
        'message' => "Pautan untuk mencipta kata laluan baharu telah dihantar ke Gmail ({$email}). Sila semak Inbox atau folder Spam Gmail anda, atau klik pautan pengesahan segera untuk menukar kata laluan dan log masuk semula."
    ], JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE);
    exit;
}

// =========================================================================
// 3. ACTION: RESET PASSWORD — SIMPAN KATA LALUAN BAHARU DENGAN BCRYPT
// =========================================================================
if ($action === 'reset_password') {
    $email = strtolower(sanitizeClean($data['email'] ?? ''));
    $rawToken = sanitizeClean($data['token'] ?? '');
    $newPassword = trim($data['newPassword'] ?? '');

    if (empty($email) || empty($rawToken)) {
        echo json_encode(['status' => 'error', 'message' => 'Pautan reset tidak lengkap atau tidak sah.']);
        exit;
    }
    if (strlen($newPassword) < 6) {
        echo json_encode(['status' => 'error', 'message' => 'Kata laluan baharu mestilah sekurang-kurangnya 6 aksara.']);
        exit;
    }

    $tokenHash = hash('sha256', $rawToken);
    $tokens = readJsonFile($resetTokensFile);
    $validToken = false;
    $remainingTokens = [];

    foreach ($tokens as $t) {
        if (strtolower($t['email'] ?? '') === $email && hash_equals($t['token_hash'] ?? '', $tokenHash)) {
            if (($t['expires_at'] ?? 0) >= time()) {
                $validToken = true;
            }
        } else {
            if (($t['expires_at'] ?? 0) >= time()) {
                $remainingTokens[] = $t;
            }
        }
    }

    if (!$validToken) {
        echo json_encode([
            'status' => 'error',
            'message' => 'Pautan reset kata laluan ini telah tamat tempoh (melebihi 30 minit) atau telah digunakan. Sila mohon pautan baharu melalui Forgot Password.'
        ]);
        exit;
    }

    // Kemas kini kata laluan dalam users.json dengan Bcrypt
    $users = readJsonFile($usersFile);
    $userUpdated = false;
    foreach ($users as $idx => $u) {
        if (strtolower($u['email'] ?? '') === $email) {
            $users[$idx]['password'] = password_hash($newPassword, PASSWORD_BCRYPT);
            $users[$idx]['password_updated_at'] = date('Y-m-d H:i:s');
            $userUpdated = true;
            break;
        }
    }

    // Jika pengguna belum wujud dalam users.json (contohnya baru daftar di Firebase), wujudkan rekodnya
    if (!$userUpdated) {
        $now = time();
        $users[] = [
            'id' => 'USR-' . strtoupper(substr(md5($email . $now), 0, 6)),
            'name' => 'Coach (' . substr($email, 0, strpos($email, '@')) . ')',
            'email' => $email,
            'password' => password_hash($newPassword, PASSWORD_BCRYPT),
            'role' => ($email === 'saliparjipun.atukoi@gmail.com') ? 'superadmin' : 'coach',
            'country' => 'Malaysia',
            'currency' => 'MYR',
            'phone' => '',
            'club_name' => 'Kelab Olahraga',
            'plan' => 'trial',
            'plan_name' => 'Percubaan Percuma (7 Hari)',
            'max_athletes' => 15,
            'max_sub_coaches' => 1,
            'registered_at' => date('Y-m-d H:i:s', $now),
            'subscription_start' => date('Y-m-d H:i:s', $now),
            'subscription_end' => date('Y-m-d H:i:s', $now + (7 * 86400)),
            'status' => 'active',
            'is_trial' => true,
            'whatsapp_verified' => false
        ];
    }

    writeJsonFile($usersFile, $users);
    writeJsonFile($resetTokensFile, $remainingTokens);
    clearFailedLoginAttempts($securityLogsFile, $email);
    recordSecurityEvent($securityLogsFile, 'password_reset_completed', $email, 'Kata laluan baharu disimpan dengan Bcrypt');

    echo json_encode([
        'status' => 'success',
        'message' => 'Kata laluan baharu anda telah berjaya disimpan! Anda kini boleh log masuk semula di Laman Web dan APK.'
    ], JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE);
    exit;
}

echo json_encode(['status' => 'error', 'message' => 'Tindakan keselamatan tidak dikenali.']);
