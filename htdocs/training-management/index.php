<?php
/**
 * Psyco Time X Pro - Training Management Portal Entry Point (index.php)
 * Lokasi: htdocs/training-management/index.php
 * Memastikan pautan /training-management/index.php dan /training-management/index.html
 * kedua-duanya berfungsi dengan lancar tanpa ralat 404, serta sedia menerima
 * parameter callback daripada Payment Gateway (ToyyibPay / Stripe / PayPal).
 */

$htmlFile = __DIR__ . '/index.html';
if (file_exists($htmlFile)) {
    readfile($htmlFile);
} else {
    header('Location: index.html');
    exit;
}
