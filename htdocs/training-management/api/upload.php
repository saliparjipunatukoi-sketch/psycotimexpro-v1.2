<?php
/**
 * Psyco Time X Pro - Training Management Media Upload Handler
 * Lokasi: htdocs/training-management/api/upload.php
 * Menerima muat naik fail video (Cam 1 & Cam 2), gambar Photo Finish, dan kad teknikal daripada APK
 */

require_once __DIR__ . '/db.php';

header('Content-Type: application/json; charset=utf-8');

$type = $_POST['type'] ?? 'file'; // 'video', 'photofinish', 'photo', 'document'
$raceTitle = preg_replace('/[^A-Za-z0-9_\-]/', '_', $_POST['raceTitle'] ?? 'Race');
$athleteName = preg_replace('/[^A-Za-z0-9_\-]/', '_', $_POST['athleteName'] ?? 'Athlete');
$coachEmail = trim($_POST['coachEmail'] ?? 'saliparjipun.atukoi@gmail.com');

if (!isset($_FILES['file']) || $_FILES['file']['error'] !== UPLOAD_ERR_OK) {
    echo json_encode([
        'status' => 'error',
        'message' => 'Tiada fail diterima atau ralat semasa memuat naik fail.'
    ]);
    exit;
}

$uploadedFile = $_FILES['file'];
$fileExt = strtolower(pathinfo($uploadedFile['name'], PATHINFO_EXTENSION));

$targetDir = $uploadsDir;
if ($type === 'video') {
    $targetDir = $videosDir;
} elseif ($type === 'photofinish') {
    $targetDir = $photosDir;
}

$newFileName = sprintf("%s_%s_%s_%s.%s", 
    strtoupper($type),
    $raceTitle,
    $athleteName,
    date('Ymd_His'),
    $fileExt ?: 'dat'
);

$destinationPath = $targetDir . '/' . $newFileName;

if (move_uploaded_file($uploadedFile['tmp_name'], $destinationPath)) {
    // Relative URL for web access
    $relativeUrl = 'api/' . basename($targetDir) . '/' . $newFileName;
    $fullUrl = 'https://www.psycotimexpro.my/training-management/' . $relativeUrl;

    echo json_encode([
        'status' => 'success',
        'message' => 'Fail berjaya disimpan di pelayan hosting Psyco Time X Pro!',
        'fileName' => $newFileName,
        'relativeUrl' => $relativeUrl,
        'fullUrl' => $fullUrl,
        'sizeBytes' => filesize($destinationPath),
        'uploadedAt' => date('Y-m-d H:i:s')
    ]);
} else {
    echo json_encode([
        'status' => 'error',
        'message' => 'Gagal memindahkan fail ke folder storan pelayan.'
    ]);
}
