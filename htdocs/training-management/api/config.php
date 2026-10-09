<?php
/**
 * Psyco Time X Pro - Database Configuration File
 * Lokasi: htdocs/training-management/api/config.php
 * Sila isi maklumat MySQL database hosting cPanel anda di bawah:
 */

// Tukar parameter ini mengikut tetapan MySQL cPanel hosting anda:
define('DB_HOST', 'localhost');
define('DB_USER', 'psycotim_dbuser');    // Username MySQL cPanel anda
define('DB_PASS', 'KatalaluanDBAnda');  // Password MySQL cPanel anda
define('DB_NAME', 'psycotim_training');  // Nama Database MySQL anda
define('DB_PORT', 3306);
define('DB_CHARSET', 'utf8mb4');

// Jika MySQL cPanel belum disetup atau gagal bersambung, sistem automatik beralih ke mod fail JSON
define('ENABLE_JSON_FALLBACK', true);
