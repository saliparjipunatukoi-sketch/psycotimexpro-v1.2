-- ===================================================================
-- Psyco Time X Pro - Isolated Database Schema (PREFIX KHAS: ptx_)
-- AMARAN KESELAMATAN: Tidak menyentuh / tidak memadam jadual sedia ada laman web utama anda!
-- Semua jadual modul training-management menggunakan prefix `ptx_`
-- ===================================================================

SET FOREIGN_KEY_CHECKS = 0;
SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET time_zone = "+08:00";

-- 1. Padam hanya jadual modul ptx_ jika sudah wujud (Selamat untuk jadual lain)
DROP TABLE IF EXISTS `ptx_payments`;
DROP TABLE IF EXISTS `ptx_sub_users`;
DROP TABLE IF EXISTS `ptx_race_records`;
DROP TABLE IF EXISTS `ptx_athletes`;
DROP TABLE IF EXISTS `ptx_users`;

-- -------------------------------------------------------------------
-- 1. JADUAL PENGGUNA & LANGGANAN (ptx_users)
-- -------------------------------------------------------------------
CREATE TABLE `ptx_users` (
  `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `user_code` VARCHAR(50) NOT NULL UNIQUE,
  `name` VARCHAR(150) NOT NULL,
  `email` VARCHAR(150) NOT NULL UNIQUE,
  `password` VARCHAR(255) NOT NULL,
  `role` VARCHAR(50) DEFAULT 'coach',
  `phone` VARCHAR(30) DEFAULT NULL,
  `club_name` VARCHAR(150) DEFAULT 'Psyco Track & Field Elite',
  `club_logo_url` TEXT DEFAULT NULL,
  `profile_photo_url` TEXT DEFAULT NULL,
  `plan` VARCHAR(50) DEFAULT 'trial',
  `plan_name` VARCHAR(100) DEFAULT 'Percubaan Percuma (7 Hari)',
  `max_athletes` INT UNSIGNED DEFAULT 10,
  `max_sub_coaches` INT UNSIGNED DEFAULT 1,
  `subscription_start` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `subscription_end` DATETIME NOT NULL,
  `status` VARCHAR(30) DEFAULT 'active',
  `is_trial` TINYINT(1) DEFAULT 1,
  `whatsapp_verified` TINYINT(1) DEFAULT 0,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_ptx_email` (`email`),
  INDEX `idx_ptx_plan` (`plan`),
  INDEX `idx_ptx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------------------------------
-- 2. JADUAL ATLIT & PROFIL PB (ptx_athletes)
-- -------------------------------------------------------------------
CREATE TABLE `ptx_athletes` (
  `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `athlete_code` VARCHAR(50) DEFAULT NULL,
  `name` VARCHAR(150) NOT NULL,
  `category` VARCHAR(50) NOT NULL DEFAULT 'Track',
  `specific_event` VARCHAR(100) NOT NULL,
  `pb_value` VARCHAR(50) NOT NULL,
  `pb_unit` VARCHAR(80) NOT NULL DEFAULT 'Saat (s)',
  `gender` VARCHAR(30) DEFAULT 'Lelaki',
  `date_of_birth` DATE DEFAULT NULL,
  `club_name` VARCHAR(150) DEFAULT NULL,
  `coach_email` VARCHAR(150) NOT NULL,
  `registration_fee` DECIMAL(10,2) DEFAULT 50.00,
  `fee_paid_status` TINYINT(1) DEFAULT 1,
  `photo_url` TEXT DEFAULT NULL,
  `notes` TEXT DEFAULT NULL,
  `synced_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_ptx_coach_email` (`coach_email`),
  INDEX `idx_ptx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------------------------------
-- 3. JADUAL KEPUTUSAN PERLUMBAAN & BIOMEKANIK (ptx_race_records)
-- -------------------------------------------------------------------
CREATE TABLE `ptx_race_records` (
  `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `race_code` VARCHAR(50) NOT NULL UNIQUE,
  `title` VARCHAR(150) NOT NULL,
  `event_name` VARCHAR(100) NOT NULL,
  `athlete_name` VARCHAR(150) NOT NULL,
  `recorded_time_millis` BIGINT UNSIGNED NOT NULL,
  `formatted_time` VARCHAR(30) NOT NULL,
  `wind_reading` VARCHAR(30) DEFAULT '+0.0 m/s',
  `lane` INT DEFAULT 1,
  `coach_email` VARCHAR(150) NOT NULL,
  `created_by_email` VARCHAR(150) NOT NULL,
  `cam1_video_url` TEXT DEFAULT NULL,
  `cam2_video_url` TEXT DEFAULT NULL,
  `photo_finish_url` TEXT DEFAULT NULL,
  `cadence_spm` INT DEFAULT 0,
  `stride_frequency_hz` DECIMAL(5,2) DEFAULT 0.00,
  `ground_contact_time_ms` INT DEFAULT 0,
  `torso_lean_angle_deg` DECIMAL(5,2) DEFAULT 0.00,
  `has_motion_detected` TINYINT(1) DEFAULT 1,
  `notes` TEXT DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_ptx_coach_race` (`coach_email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------------------------------
-- 4. JADUAL SUB-COACH / URUSETIA (ptx_sub_users)
-- -------------------------------------------------------------------
CREATE TABLE `ptx_sub_users` (
  `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `username` VARCHAR(80) NOT NULL,
  `full_name` VARCHAR(150) NOT NULL,
  `role` VARCHAR(80) NOT NULL,
  `parent_coach_email` VARCHAR(150) NOT NULL,
  `pin_code` VARCHAR(10) NOT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uniq_ptx_subuser` (`username`, `parent_coach_email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------------------------------
-- 5. JADUAL TRANSAKSI PEMBAYARAN LANGGANAN (ptx_payments)
-- -------------------------------------------------------------------
CREATE TABLE `ptx_payments` (
  `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `payment_code` VARCHAR(50) NOT NULL UNIQUE,
  `user_email` VARCHAR(150) NOT NULL,
  `plan_key` VARCHAR(50) NOT NULL,
  `plan_name` VARCHAR(100) NOT NULL,
  `amount` DECIMAL(10,2) NOT NULL,
  `payment_method` VARCHAR(50) DEFAULT 'WhatsApp Admin',
  `receipt_url` TEXT DEFAULT NULL,
  `status` VARCHAR(50) DEFAULT 'PENDING_WHATSAPP_CONFIRMATION',
  `new_expiry_date` DATETIME NOT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------------------------------
-- SEED DATA: Superadmin (Coach Salipar Jipun)
-- -------------------------------------------------------------------
INSERT INTO `ptx_users` (
  `user_code`, `name`, `email`, `password`, `role`, `phone`, `club_name`, 
  `plan`, `plan_name`, `max_athletes`, `max_sub_coaches`, 
  `subscription_start`, `subscription_end`, `status`, `is_trial`, `whatsapp_verified`
) VALUES (
  'USR-ADMIN-01', 
  'Coach Salipar Jipun', 
  'saliparjipun.atukoi@gmail.com', 
  MD5('admin123'), 
  'superadmin', 
  '601112345678', 
  'Psyco Track & Field Elite Malaysia', 
  'plan_rm80', 
  'Superadmin Unlimited', 
  9999, 
  99, 
  NOW(), 
  DATE_ADD(NOW(), INTERVAL 5 YEAR), 
  'active', 
  0, 
  1
);

SET FOREIGN_KEY_CHECKS = 1;
