-- ===================================================================
-- Psyco Time X Pro - Official MySQL Database Schema
-- Database: psycotimexpro_db (atau nama database hosting anda di cPanel)
-- Disediakan khas untuk: Coach Salipar Jipun (saliparjipun.atukoi@gmail.com)
-- Web Portal & APK Server: https://www.psycotimexpro.my/training-management/
-- ===================================================================

SET FOREIGN_KEY_CHECKS = 0;
SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET time_zone = "+08:00"; -- Waktu Malaysia (MYT)

-- -------------------------------------------------------------------
-- 1. JADUAL PENGGUNA & LANGGANAN (users)
-- Menyimpan maklumat Jurulatih, Superadmin, Sub-User dan Status Langganan
-- -------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `users` (
  `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `user_code` VARCHAR(50) NOT NULL UNIQUE,
  `name` VARCHAR(150) NOT NULL,
  `email` VARCHAR(150) NOT NULL UNIQUE,
  `password` VARCHAR(255) NOT NULL,
  `role` ENUM('superadmin', 'coach', 'official', 'subuser') DEFAULT 'coach',
  `phone` VARCHAR(30) DEFAULT NULL,
  `club_name` VARCHAR(150) DEFAULT 'Psyco Track & Field Elite',
  `club_logo_url` TEXT DEFAULT NULL,
  `profile_photo_url` TEXT DEFAULT NULL,
  `plan` ENUM('trial', 'basic', 'pro', 'elite', 'unlimited') DEFAULT 'trial',
  `plan_name` VARCHAR(100) DEFAULT 'Percubaan Percuma (7 Hari)',
  `max_athletes` INT UNSIGNED DEFAULT 10,
  `subscription_start` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `subscription_end` DATETIME NOT NULL,
  `status` ENUM('active', 'expired', 'suspended') DEFAULT 'active',
  `is_trial` TINYINT(1) DEFAULT 1,
  `whatsapp_verified` TINYINT(1) DEFAULT 0,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_email` (`email`),
  INDEX `idx_plan` (`plan`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------------------------------
-- 2. JADUAL ATLIT & PROFIL PB (athletes)
-- Diselaraskan secara automatik daripada APK Android
-- -------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `athletes` (
  `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `athlete_code` VARCHAR(50) DEFAULT NULL,
  `name` VARCHAR(150) NOT NULL,
  `category` ENUM('Track', 'Field') NOT NULL DEFAULT 'Track',
  `specific_event` VARCHAR(100) NOT NULL,
  `pb_value` VARCHAR(50) NOT NULL,
  `pb_unit` VARCHAR(80) NOT NULL DEFAULT 'Saat (s)',
  `gender` ENUM('Lelaki', 'Perempuan') DEFAULT 'Lelaki',
  `date_of_birth` DATE DEFAULT NULL,
  `club_name` VARCHAR(150) DEFAULT NULL,
  `coach_email` VARCHAR(150) NOT NULL,
  `registration_fee` DECIMAL(10,2) DEFAULT 50.00,
  `fee_paid_status` TINYINT(1) DEFAULT 1,
  `photo_url` TEXT DEFAULT NULL,
  `notes` TEXT DEFAULT NULL,
  `synced_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_coach_email` (`coach_email`),
  INDEX `idx_category` (`category`),
  INDEX `idx_specific_event` (`specific_event`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------------------------------
-- 3. JADUAL KEPUTUSAN RACE & TELEMETRI BIOMEKANIK (race_records)
-- Menyimpan catatan masa rasmi jam randik, data Cam 1, Cam 2, & Photo Finish
-- -------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `race_records` (
  `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `race_code` VARCHAR(50) DEFAULT NULL,
  `title` VARCHAR(150) NOT NULL, -- cth: "Race 1", "Saringan 1 100m"
  `event_name` VARCHAR(100) NOT NULL,
  `athlete_id` INT UNSIGNED DEFAULT NULL,
  `athlete_name` VARCHAR(150) NOT NULL,
  `recorded_time_millis` BIGINT UNSIGNED NOT NULL, -- cth: 10180 ms
  `formatted_time` VARCHAR(30) NOT NULL, -- cth: "0:00:10:180"
  `wind_reading` VARCHAR(20) DEFAULT '+0.0 m/s',
  `lane` INT DEFAULT 4,
  `coach_email` VARCHAR(150) NOT NULL,
  `created_by_email` VARCHAR(150) NOT NULL,
  `cam1_video_url` TEXT DEFAULT NULL,
  `cam2_video_url` TEXT DEFAULT NULL,
  `photo_finish_url` TEXT DEFAULT NULL,
  `has_motion_detected` TINYINT(1) DEFAULT 1,
  `cadence_spm` INT DEFAULT 260,
  `stride_freq_hz` DECIMAL(5,2) DEFAULT 4.35,
  `ground_contact_time_ms` INT DEFAULT 108,
  `torso_lean_angle_deg` DECIMAL(5,2) DEFAULT 14.50,
  `notes` TEXT DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_coach_race` (`coach_email`),
  INDEX `idx_event` (`event_name`),
  INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------------------------------
-- 4. JADUAL PEMBAYARAN LANGGANAN & YURAN APK (subscription_payments)
-- Merekodkan transaksi bulanan RM30, RM40, RM50, bukti WhatsApp dan invois
-- -------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `subscription_payments` (
  `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `receipt_no` VARCHAR(60) NOT NULL UNIQUE,
  `user_email` VARCHAR(150) NOT NULL,
  `plan_key` VARCHAR(50) NOT NULL,
  `plan_name` VARCHAR(100) NOT NULL,
  `amount` DECIMAL(10,2) NOT NULL,
  `payment_method` VARCHAR(50) DEFAULT 'WhatsApp Admin',
  `payment_status` ENUM('PENDING', 'COMPLETED', 'REJECTED') DEFAULT 'PENDING',
  `old_expiry_date` DATETIME DEFAULT NULL,
  `new_expiry_date` DATETIME NOT NULL,
  `admin_notes` TEXT DEFAULT NULL,
  `verified_by` VARCHAR(150) DEFAULT 'saliparjipun.atukoi@gmail.com',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_user_pay` (`user_email`),
  INDEX `idx_receipt` (`receipt_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------------------------------
-- 5. JADUAL SUB-USERS (PEGAWAI TEKNIKAL & URUSETIA - MAX 10 ORANG)
-- Menguruskan akaun krew teknikal kejohanan atau jurulatih pembantu
-- -------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sub_users` (
  `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `username` VARCHAR(60) NOT NULL,
  `full_name` VARCHAR(150) NOT NULL,
  `role` VARCHAR(80) NOT NULL DEFAULT 'Pegawai Masa ET',
  `parent_coach_email` VARCHAR(150) NOT NULL,
  `pin_code` VARCHAR(10) NOT NULL DEFAULT '1234',
  `is_active` TINYINT(1) DEFAULT 1,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_parent_coach` (`parent_coach_email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===================================================================
-- DATA AWAL DEFAULT (SEEDED DATA)
-- Superadmin, Atlit Kebangsaan & Contoh Perlumbaan
-- ===================================================================

-- 1. Masukkan Superadmin (Coach Salipar Jipun) & Demo Coach
INSERT INTO `users` (`user_code`, `name`, `email`, `password`, `role`, `phone`, `club_name`, `plan`, `plan_name`, `max_athletes`, `subscription_start`, `subscription_end`, `status`, `is_trial`, `whatsapp_verified`)
VALUES 
('USR-ADMIN-01', 'Coach Salipar Jipun', 'saliparjipun.atukoi@gmail.com', MD5('admin123'), 'superadmin', '601112345678', 'Psyco Track & Field Elite Malaysia', 'unlimited', 'Superadmin Unlimited', 9999, NOW(), DATE_ADD(NOW(), INTERVAL 5 YEAR), 'active', 0, 1),
('USR-COACH-02', 'Jurulatih Percubaan (Demo Club)', 'coach.demo@gmail.com', MD5('demo123'), 'coach', '60123456789', 'Kelab Olahraga Gemilang', 'trial', 'Percubaan Percuma (7 Hari)', 10, NOW(), DATE_ADD(NOW(), INTERVAL 7 DAY), 'active', 1, 0)
ON DUPLICATE KEY UPDATE `name`=VALUES(`name`);

-- 2. Masukkan Contoh Rekod Atlit Kebangsaan
INSERT INTO `athletes` (`athlete_code`, `name`, `category`, `specific_event`, `pb_value`, `pb_unit`, `gender`, `date_of_birth`, `club_name`, `coach_email`, `registration_fee`, `fee_paid_status`, `notes`)
VALUES
('ATH-001', 'Muhammad Azeem Fahmi', 'Track', '100m Sprint', '10.09', 'Saat (s)', 'Lelaki', '2004-04-29', 'Psyco Track & Field Elite Malaysia', 'saliparjipun.atukoi@gmail.com', 50.00, 1, 'Sasaran Kelayakan Olimpik & Kejohanan Asia'),
('ATH-002', 'Shereen Samson Vallabouy', 'Track', '400m', '51.80', 'Saat (s)', 'Perempuan', '1998-07-10', 'Psyco Track & Field Elite Malaysia', 'saliparjipun.atukoi@gmail.com', 50.00, 1, 'Pemegang Rekod Kebangsaan 400m'),
('ATH-003', 'Andre Anura Anuar', 'Field', 'Lompat Jauh', '8.02', 'Meter (Jarak - Lompat Jauh/Kijang)', 'Lelaki', '1999-06-12', 'Psyco Track & Field Elite Malaysia', 'saliparjipun.atukoi@gmail.com', 50.00, 1, 'Sasaran Emas Sukan SEA');

-- 3. Masukkan Contoh Perlumbaan & Rekod Masa Jam Randik
INSERT INTO `race_records` (`race_code`, `title`, `event_name`, `athlete_name`, `recorded_time_millis`, `formatted_time`, `wind_reading`, `lane`, `coach_email`, `created_by_email`, `cam1_video_url`, `cam2_video_url`, `photo_finish_url`, `cadence_spm`, `ground_contact_time_ms`, `torso_lean_angle_deg`, `notes`)
VALUES
('RACE-101', 'Race 1 - Saringan 100m', '100m Sprint', 'Muhammad Azeem Fahmi', 10180, '0:00:10:180', '+1.2 m/s', 4, 'saliparjipun.atukoi@gmail.com', 'saliparjipun.atukoi@gmail.com', 'api/videos/CAM1_Race1_Demo.mp4', 'api/videos/CAM2_Race1_Torso.mp4', 'api/photofinish/PhotoFinish_Race1.png', 268, 96, 15.20, 'Finisher Torso Lean tepat pada garisan penamat Cam 2.');

SET FOREIGN_KEY_CHECKS = 1;
