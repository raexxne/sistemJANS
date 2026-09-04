-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: Sep 04, 2026 at 03:06 AM
-- Server version: 10.4.32-MariaDB
-- PHP Version: 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `jans_access`
--

-- --------------------------------------------------------

--
-- Table structure for table `applications`
--

CREATE TABLE `applications` (
  `id` bigint(20) NOT NULL,
  `application_no` varchar(255) DEFAULT NULL,
  `applicant_name` varchar(255) DEFAULT NULL,
  `ic_no` varchar(255) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `email_wakil` varchar(255) DEFAULT NULL,
  `phone_mobile` varchar(255) DEFAULT NULL,
  `phone_office` varchar(255) DEFAULT NULL,
  `jawatan_gred` varchar(255) DEFAULT NULL,
  `application_date` date DEFAULT NULL,
  `organisation` varchar(255) DEFAULT NULL,
  `visit_date` date NOT NULL,
  `visit_time` time NOT NULL,
  `location_type` enum('LOJI','INTAKE') NOT NULL,
  `location_name` varchar(255) DEFAULT NULL,
  `purpose` text NOT NULL,
  `vehicle_no` varchar(255) DEFAULT NULL,
  `status` enum('DIHANTAR','SEMAKAN_STAFF','MENUNGGU_PENGARAH','DILULUSKAN','DITOLAK','PAS_DIKELUARKAN') NOT NULL,
  `staff_note` text DEFAULT NULL,
  `director_note` text DEFAULT NULL,
  `reviewed_by` bigint(20) DEFAULT NULL,
  `decided_by` bigint(20) DEFAULT NULL,
  `decision_at` datetime DEFAULT NULL,
  `pass_token` varchar(255) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `applications`
--

INSERT INTO `applications` (`id`, `application_no`, `applicant_name`, `ic_no`, `email`, `email_wakil`, `phone_mobile`, `phone_office`, `jawatan_gred`, `application_date`, `organisation`, `visit_date`, `visit_time`, `location_type`, `location_name`, `purpose`, `vehicle_no`, `status`, `staff_note`, `director_note`, `reviewed_by`, `decided_by`, `decision_at`, `pass_token`, `created_at`, `updated_at`) VALUES
(1, 'JAS-2026-000001', 'Madison Ramli', '098762-12-8796', 'madisonramli@gmail.com', 'madisonramli@gmail.com', '0123234565', NULL, NULL, '2026-07-23', '', '2026-07-30', '21:30:00', 'LOJI', 'Kg Air, KK', 'Hantar barang pembaikan tempat air.', 'SAB2343', 'DILULUSKAN', '', 'Boleh proceed', 1, 2, '2026-07-23 16:23:25', NULL, '2026-07-23 16:21:06', '2026-07-23 16:23:25'),
(2, 'JAS-2026-000002', 'William Adrison', '098762-12-0989', 'w1llyblly00@gmail.com', 'w1llyblly00@gmail.com', '0198987678', NULL, NULL, '2026-07-24', '', '2026-07-25', '23:30:00', 'LOJI', 'Kg Air, KK', 'Pindahkan barang', 'SAB2343', 'PAS_DIKELUARKAN', '', '', 1, 2, '2026-07-24 07:48:12', 'RxB82rYmTrl1LK-PYLgGkmAxS0ZIshAc', '2026-07-24 07:41:32', '2026-07-24 08:39:53'),
(3, 'JAS-2026-000003', 'Madison Ramli', '098762-12-8796', 'amanda@gmail.com', 'amanda@gmail.com', '012-3454323', NULL, NULL, '2026-07-24', 'Syarikat Amid Sdn Bhd', '2026-07-30', '18:30:00', 'LOJI', 'Kg Air, KK', 'Menjalankan pembaikan paip', 'SAB2343', 'DILULUSKAN', '', '', 1, 2, '2026-07-24 14:41:20', NULL, '2026-07-24 14:33:16', '2026-07-24 14:41:20'),
(4, 'JAS-2026-000004', 'Madison Ramli', '098762-12-8796', 'amanda@gmail.com', 'amanda@gmail.com', '012-3454323', NULL, NULL, '2026-07-24', 'Syarikat Amid Sdn Bhd', '2026-07-30', '14:51:00', 'LOJI', 'Kg Air, KK', 'Contoh', 'SAB2343', 'DILULUSKAN', '', '', 1, 2, '2026-07-24 15:28:45', NULL, '2026-07-24 14:51:19', '2026-07-24 15:28:45'),
(5, 'JAS-2026-000005', 'Madison Ramli', '098762-12-8796', 'amanda@gmail.com', 'amanda@gmail.com', '012-3454323', NULL, NULL, '2026-07-24', 'Syarikat Amid Sdn Bhd', '2026-07-30', '20:35:00', 'LOJI', 'Kg Air, KK', 'Contoh', 'SAB2343', 'PAS_DIKELUARKAN', '', '', 1, 2, '2026-07-24 15:35:18', 'yLRGT4zrd7Q5iYrUS2linbK3fRZ5Hrhw', '2026-07-24 15:34:50', '2026-07-24 15:36:54'),
(6, 'JAS-2026-000006', 'Madison Ramli', '098762-12-8796', 'w1llyblly00@gmail.com', 'w1llyblly00@gmail.com', '012-3454323', NULL, NULL, '2026-07-24', 'Syarikat Amid Sdn Bhd', '2026-07-30', '05:37:00', 'LOJI', 'Kg Air, KK', 'Contoh', 'SAB2343', 'PAS_DIKELUARKAN', '', '', 1, 2, '2026-07-24 15:38:27', '4kpntet-IoCzdMz3TaHkhvw4ljYMrIzy', '2026-07-24 15:37:52', '2026-07-24 15:38:52'),
(7, 'JAS-2026-000007', 'Madison Ramli', '098762-12-8796', 'w1llyblly00@gmail.com', 'w1llyblly00@gmail.com', '012-3454323', NULL, NULL, '2026-07-27', 'Syarikat Amid Sdn Bhd', '2026-07-30', '19:07:00', 'LOJI', 'Kg Air, KK', 'Menjalankan pekerjaan penyelenggaraan sistem paip', 'SAB2343', 'PAS_DIKELUARKAN', '', '', 1, 2, '2026-07-27 16:19:11', 'kSRQbN1xYT1NeV3VFo8OU7W1H_JZ7QLj', '2026-07-27 16:08:29', '2026-07-27 16:20:08'),
(8, 'JAS-2026-000008', 'Haaland Primus', '098789-12-8765', 'w1llyblly00@gmail.com', 'w1llyblly00@gmail.com', '012-1223234', NULL, NULL, '2026-07-28', 'Syarikat Lima Sdn Bhd', '2026-07-30', '17:30:00', 'INTAKE', 'Kampung Limbahau, Papar', 'Menjalankan pemantauan', 'SAB 456 W', 'DITOLAK', '', '', 1, 2, '2026-07-28 10:38:44', NULL, '2026-07-28 10:37:28', '2026-07-28 10:38:44'),
(9, 'JAS-2026-000009', 'Willy Billy', '875484-12-5487', 'w1llyblly00@gmail.com', 'w1llyblly00@gmail.com', '012-1223876', NULL, NULL, '2026-07-28', 'Syarikat Enam Sdn Bhd', '2026-07-31', '18:00:00', 'LOJI', 'Kampung Limbahau, Papar', 'Menjalankan pembaikan paip', 'SAB 456 W', 'PAS_DIKELUARKAN', '', '', 1, 2, '2026-07-28 12:32:44', '-BTC6CCcZfHdSxvCTMg_elempQT12LW3', '2026-07-28 12:31:33', '2026-07-28 12:33:15'),
(10, 'JAS-2026-000010', 'Maddy Lindrus', '231232-12-7685', 'w1llyblly00@gmail.com', 'w1llyblly00@gmail.com', '012-1223876', NULL, NULL, '2026-07-28', 'Syarikat Enam Sdn Bhd', '2026-07-31', '17:30:00', 'LOJI', 'Kampung Limbahau, Papar', 'Menjalankan pemantauan paip air', 'SAB 456 W', 'PAS_DIKELUARKAN', '', '', 1, 2, '2026-07-28 14:28:18', 'uTtoqPJLNmdSCxIr4WymFiKggh6pOrT0', '2026-07-28 14:13:37', '2026-07-28 14:33:48'),
(11, 'JAS-2026-000011', 'Sandreas Lima', '231232-12-7685', 'w1llyblly00@gmail.com', 'w1llyblly00@gmail.com', '012-1223876', NULL, NULL, '2026-07-28', 'Syarikat Enam Sdn Bhd', '2026-07-31', '17:20:00', 'LOJI', 'Kampung Limbahau, Papar', 'Menjalankan pemantauan paip air', 'SAB 456 W', 'MENUNGGU_PENGARAH', '', NULL, 1, NULL, NULL, NULL, '2026-07-28 15:20:25', '2026-07-28 15:21:35'),
(12, 'JAS-2026-000012', 'Hijau Biru', '231232-12-7685', 'w1llyblly00@gmail.com', 'w1llyblly00@gmail.com', '012-1223876', NULL, NULL, '2026-07-28', 'Syarikat Enam Sdn Bhd', '2026-07-31', '19:30:00', 'LOJI', 'Kampung Limbahau, Papar', 'Menjalankan penyelenggaraan paip.', 'SAB 456 W', 'PAS_DIKELUARKAN', '', 'lulus', 1, 2, '2026-07-29 09:57:36', 'j7ZwKa-qRjG2JdKQ7BKIdxuj5crOXcHm', '2026-07-28 15:21:00', '2026-07-30 08:00:53'),
(13, 'JAS-2026-000013', 'Marie Ramlie', '231232-12-7685', 'w1llyblly00@gmail.com', 'w1llyblly00@gmail.com', '012-1223876', NULL, NULL, '2026-07-29', 'Syarikat Lapan Sdn Bhd', '2026-08-01', '13:30:00', 'LOJI', 'Loji Air Tuaran', 'Menjalankan kerja penyelenggaraan sistem paip', 'SAB 456 W', 'PAS_DIKELUARKAN', '', '', 1, 2, '2026-07-29 08:44:31', 'oWCi4kLwNYjB1CMhnRkpK7Tztm5kpV45', '2026-07-29 08:43:01', '2026-07-29 08:44:58'),
(14, 'JAS-2026-000014', 'Marie Ramlie', '231232-12-7685', 'w1llyblly00@gmail.com', 'w1llyblly00@gmail.com', '012-1223876', NULL, NULL, '2026-07-29', 'Syarikat Lapan Sdn Bhd', '2026-08-01', '09:35:00', 'LOJI', 'Loji Air Tuaran', 'Menjalankan kerja penyelenggaraan sistem paip', 'SAB 456 W', 'PAS_DIKELUARKAN', '', '', 1, 2, '2026-07-29 09:37:25', 'tQ3afQx6qyE7t6wHo87o6OmzK7WjF3GB', '2026-07-29 09:36:06', '2026-07-29 09:37:54'),
(15, 'JAS-2026-000015', 'Sandreas Lima', '231232-12-7685', 'w1llyblly00@gmail.com', 'w1llyblly00@gmail.com', '012-1223876', NULL, NULL, '2026-07-29', 'Syarikat Enam Sdn Bhd', '2026-07-29', '09:41:00', 'LOJI', 'Kampung Limbahau, Papar', 'JLN2', 'SAB 456 W', 'DIHANTAR', NULL, NULL, NULL, NULL, NULL, NULL, '2026-07-29 09:42:15', '2026-07-29 09:42:15'),
(16, 'JAS-2026-000016', 'Hijau Biru', '231232-12-7685', 'w1llyblly00@gmail.com', 'w1llyblly00@gmail.com', '012-1223876', NULL, NULL, '2026-07-29', 'Syarikat Enam Sdn Bhd', '2026-07-31', '17:30:00', 'LOJI', 'Kampung Limbahau, Papar', 'Kelabu', 'SAB 456 W', 'DIHANTAR', NULL, NULL, NULL, NULL, NULL, NULL, '2026-07-29 11:17:38', '2026-07-29 11:17:38'),
(17, 'JAS-2026-000017', 'Test', '123', 'test@example.com', 'test@example.com', '0123456789', NULL, NULL, '2026-07-29', 'Org', '2026-08-01', '10:00:00', 'LOJI', 'Loji Uji', 'Ujian', 'ABC123', 'DIHANTAR', NULL, NULL, NULL, NULL, NULL, NULL, '2026-07-29 11:22:36', '2026-07-29 11:22:36'),
(18, 'JAS-2026-000018', 'Hijau Biru', '231232-12-7685', 'w1llyblly00@gmail.com', 'w1llyblly00@gmail.com', '012-1223876', NULL, NULL, '2026-07-29', 'Syarikat Enam Sdn Bhd', '2026-07-31', '11:35:00', 'LOJI', 'Kampung Limbahau, Papar', 'Gambar', 'SAB 456 W', 'MENUNGGU_PENGARAH', '', NULL, 1, NULL, NULL, NULL, '2026-07-29 11:35:20', '2026-07-29 15:21:08'),
(19, 'JAS-2026-000019', 'Madison Ramli', '098762-12-8796', 'w1llyblly00@gmail.com', 'w1llyblly00@gmail.com', '012-9878765', '088-090878', 'Jurutera (J41)', '2026-07-29', 'Syarikat Enam Sdn Bhd', '2026-07-31', '14:00:00', 'LOJI', 'Kg Air, KK', 'Penyelenggaraan paip', 'SAB 456 W', 'MENUNGGU_PENGARAH', '', NULL, 1, NULL, NULL, NULL, '2026-07-29 14:59:24', '2026-07-29 15:14:19'),
(20, 'JAS-2026-000020', 'Madison Ramli', '098762-12-8796', 'w1llyblly00@gmail.com', 'w1llyblly00@gmail.com', '012-9878765', '088-564567876', 'Jurutera (J41)', '2026-07-30', 'Syarikat Enam Sdn Bhd', '2026-07-31', '00:30:00', 'LOJI', 'Kg Air, KK', 'Menjalankan penyelenggaraan paip', 'SAB 456 W', 'MENUNGGU_PENGARAH', '', NULL, 1, NULL, NULL, NULL, '2026-07-30 09:27:07', '2026-07-30 09:28:41'),
(21, 'JAS-2026-000021', 'Kosong Kosong', '098762-12-7678', 'w1llyblly00@gmail.com', 'w1llyblly00@gmail.com', '012-9878765', '088-089786', 'Jurutera (J41)', '2026-07-30', 'Syarikat Enam Sdn Bhd', '2026-07-31', '10:30:00', 'LOJI', 'Kg Air, KK', 'Menjalankan pembaikan paip air', 'SAB 456 W', 'MENUNGGU_PENGARAH', '', NULL, 1, NULL, NULL, NULL, '2026-07-30 09:28:10', '2026-07-30 09:28:45'),
(22, 'JAS-2026-000022', 'Dermawaan Sima', '098762-12-8796', 'w1llyblly00@gmail.com', 'w1llyblly00@gmail.com', '012-9878765', '088-878657', 'Jurutera (J41)', '2026-07-30', 'Syarikat Enam Sdn Bhd', '2026-07-31', '10:30:00', 'LOJI', 'Kg Air, KK', 'Test', 'SAB 456 W', 'PAS_DIKELUARKAN', 'Semua dokumen lengkap', 'Lulus', 1, 2, '2026-07-30 09:38:29', '1B8nk3TQb0XuBYXUrU44jCNcUjXzxbzA', '2026-07-30 09:35:41', '2026-07-30 09:53:33'),
(23, 'JAS-2026-000023', 'Tema Kinta', '098762-12-8796', 'w1llyblly00@gmail.com', 'w1llyblly00@gmail.com', '012-9878765', '088-654568', 'Jurutera (J41)', '2026-07-30', 'Syarikat Enam Sdn Bhd', '2026-07-31', '17:30:00', 'LOJI', 'Kg Air, KK', 'Test2', 'SAB 456 W', 'DITOLAK', 'Dokumen lengkap', 'Tidak lulus', 1, 2, '2026-07-30 09:38:38', NULL, '2026-07-30 09:36:22', '2026-07-30 09:38:38'),
(24, 'JAS-2026-000024', 'Hima Naru', '098762-12-8796', 'w1llyblly00@gmail.com', 'w1llyblly00@gmail.com', '012-9878765', '088-564546', 'Jurutera (J41)', '2026-07-31', 'Syarikat Enam Sdn Bhd', '2026-07-31', '00:30:00', 'LOJI', 'Kg Air, KK', 'Menjalankan penyelenggaraan paip', 'SAB 456 W', 'PAS_DIKELUARKAN', '', '', 1, 2, '2026-07-31 09:02:16', 'ru2xj9Jn2SQ9F84zorYhkU5Lm4TZskIo', '2026-07-31 09:01:20', '2026-07-31 09:02:37'),
(25, 'JAS-2026-000025', 'Qilla Hamza', '098762-12-8796', 'w1llyblly00@gmail.com', 'w1llyblly00@gmail.com', '012-9878765', '088-564567876', 'Jurutera (J41)', '2026-07-31', 'Syarikat Enam Sdn Bhd', '2026-07-31', '11:30:00', 'LOJI', 'Kg Air, KK', 'Menjalankan penyelenggaraan paip', 'SAB 456 W', 'PAS_DIKELUARKAN', '', '', 1, 2, '2026-07-31 09:17:43', 'xZVrmoEIVbxyTTH7dx_KWO0wz_Tu7Wx2', '2026-07-31 09:17:13', '2026-07-31 09:18:07'),
(26, 'JAS-2026-000026', 'Silla Miwan', '098762-12-8796', 'w1llyblly00@gmail.com', 'w1llyblly00@gmail.com', '012-9878765', '088-564567876', 'Jurutera (J41)', '2026-07-31', 'Syarikat Enam Sdn Bhd', '2026-07-31', '13:30:00', 'LOJI', 'Kg Air, KK', 'Menjalankan penyelenggaraan paip', 'SAB 456 W', 'MENUNGGU_PENGARAH', 'test', NULL, 1, NULL, NULL, NULL, '2026-07-31 09:40:17', '2026-08-18 10:43:40'),
(27, 'JAS-2026-000027', 'Sina Markus', '098762-12-8796', 'w1llyblly00@gmail.com', 'w1llyblly00@gmail.com', '012-9878765', '', 'Jurutera (J41)', '2026-08-03', 'Syarikat Tujuh Sdn Bhd', '2026-07-31', '17:30:00', 'LOJI', 'Kg Air, KK', 'Menjalankan penyelenggaraan paip', 'SAB 456 W', 'MENUNGGU_PENGARAH', '', NULL, 1, NULL, NULL, NULL, '2026-08-03 14:22:31', '2026-08-03 15:24:38'),
(28, 'JAS-2026-000028', 'Mila Hamza', '098762-12-8796', 'w1llyblly00@gmail.com', 'w1llyblly00@gmail.com', '012-9878765', '088-898767', 'Jurutera (J41)', '2026-08-03', 'Syarikat Tujuh Sdn Bhd', '2026-07-31', '19:30:00', 'LOJI', 'Kg Air, KK', 'Menjalankan penyelenggaraan paip', 'SAB 456 W', 'DITOLAK', '', '', 1, 2, '2026-08-06 10:52:38', NULL, '2026-08-03 14:25:29', '2026-08-06 10:52:38'),
(29, 'JAS-2026-000029', 'Kinsantara Musim', '098762-12-8796', 'w1llyblly00@gmail.com', 'w1llyblly00@gmail.com', '012-9878765', '088-909343', 'Jurutera (J41)', '2026-08-03', 'Syarikat Tujuh Sdn Bhd', '2026-07-31', '18:30:00', 'LOJI', 'Kg Air, KK', 'Menjalankan penyelenggaraan paip', 'SAB 456 W', 'PAS_DIKELUARKAN', '', '', 1, 2, '2026-08-03 15:33:29', 'gBjM1RiLT1AhM7mdIpKfoqZVUpyY4XKA', '2026-08-03 14:27:41', '2026-08-03 15:34:22'),
(30, 'JAS-2026-000030', 'Sina Markus', '098762-12-8796', 'w1llyblly00@gmail.com', 'w1llyblly00@gmail.com', '012-9878765', '088-546376', 'Jurutera (J41)', '2026-08-03', 'Syarikat Tujuh Sdn Bhd', '2026-07-31', '16:30:00', 'LOJI', 'Kg Air, KK', 'Menjalankan penyelenggaraan paip', 'SAB 456 W', 'PAS_DIKELUARKAN', '', '', 1, 2, '2026-08-03 14:44:39', '62dsdeoa00AZRCXGSGDESiv8NQvqymC5', '2026-08-03 14:30:46', '2026-08-03 14:45:03'),
(31, 'JAS-2026-000031', 'Rina Zam', '980978-12-0998', 'w1llyblly00@gmail.com', 'w1llyblly00@gmail.com', '012-9657489', '088-398276', 'Jurutera (J41)', '2026-08-04', 'Syarikat Lapan Sdn Bhd', '2026-08-28', '10:30:00', 'LOJI', 'Kg Air, KK', 'Menjalankan penyelenggaraan paip', 'SAB 456 W', 'DITOLAK', '', '', 7, 2, '2026-08-24 10:40:34', NULL, '2026-08-04 08:41:54', '2026-08-24 10:40:38'),
(32, 'JAS-2026-000032', 'Tina Raymold', '980546-12-0987', 'w1llyblly00@gmail.com', 'w1llyblly00@gmail.com', '012-9657489', '088-234932', 'Jurutera (J41)', '2026-08-04', 'Syarikat Sembilan Sdn Bhd', '2026-10-14', '10:30:00', 'LOJI', 'Kg Air, KK', 'Menjalankan penyelenggaraan paip', 'SAB 456 W', 'PAS_DIKELUARKAN', '', '', 1, 2, '2026-08-04 08:52:50', 'MILMjAQiOV3rF-1onKsANAKiSDLHJlbB', '2026-08-04 08:50:03', '2026-08-04 08:53:09'),
(33, 'JAS-2026-000033', 'Man Samsur', '980346-12-0534', 'w1llyblly00@gmail.com', 'w1llyblly00@gmail.com', '012-9657489', '088-878987', 'Jurutera (J41)', '2026-08-06', 'Syarikat Sembilan Sdn Bhd', '2026-10-14', '00:30:00', 'LOJI', 'Kg Air, KK', 'Menjalankan penyelenggaraan paip', 'SAB 456 W', 'PAS_DIKELUARKAN', '', '', 1, 2, '2026-08-06 10:50:19', '8ry1OlpHw1ixfo-LsuFwX4uYHwMVaE60', '2026-08-06 10:41:03', '2026-08-06 10:56:09'),
(34, 'JAS-2026-000034', 'a', '980346-12-0534', 'w1llyblly00@gmail.com', 'w1llyblly00@gmail.com', '012-9657489', '', 'Jurutera (J41)', '2026-08-31', 'Syarikat Sembilan Sdn Bhd', '2026-08-07', '00:01:00', 'LOJI', 'Kg Air, KK', 's', 'SAB 456 W', 'DIHANTAR', NULL, NULL, NULL, NULL, NULL, NULL, '2026-08-18 11:01:02', '2026-08-18 11:01:02'),
(35, 'JAS-2026-000035', 'Hijau Biru', '980346-12-0534', 'w1llyblly00@gmail.com', 'w1llyblly00@gmail.com', '012-9657489', '', 'Jurutera (J41)', '2026-08-18', 'Syarikat Sembilan Sdn Bhd', '2026-08-28', '17:30:00', 'LOJI', 'Kg Air, KK', 'test', 'SAB 456 W', 'DIHANTAR', NULL, NULL, NULL, NULL, NULL, NULL, '2026-08-18 15:31:52', '2026-08-18 15:31:52'),
(36, 'JAS-2026-000036', 'Sandreas Lima', '980346-12-0534', 'w1llyblly00@gmail.com', 'w1llyblly00@gmail.com', '012-9657489', '45434', 'Jurutera (J41)', '2026-08-18', 'Syarikat Sembilan Sdn Bhd', '2026-08-28', '15:30:00', 'LOJI', 'Kg Air, KK', 'test', 'SAB 456 W', 'DIHANTAR', NULL, NULL, NULL, NULL, NULL, NULL, '2026-08-18 15:33:11', '2026-08-18 15:33:11'),
(37, 'JAS-2026-000037', 'Sandreas Lima', '980346-12-0534', 'w1llyblly00@gmail.com', 'w1llyblly00@gmail.com', '012-9657489', '', 'Jurutera (J41)', '2026-08-18', 'Syarikat Sembilan Sdn Bhd', '2026-08-28', '18:30:00', 'LOJI', 'Kg Air, KK', 'test', 'SAB 456 W', 'DIHANTAR', NULL, NULL, NULL, NULL, NULL, NULL, '2026-08-18 15:45:13', '2026-08-18 15:45:13'),
(38, 'JAS-2026-000038', 'Sandreas Lima', '980346-12-0534', 'w1llyblly00@gmail.com', 'w1llyblly00@gmail.com', '012-9657489', '', 'Jurutera (J41)', '2026-08-18', 'Syarikat Sembilan Sdn Bhd', '2026-08-28', '18:30:00', 'LOJI', 'Kg Air, KK', 'test', 'SAB 456 W', 'DIHANTAR', NULL, NULL, NULL, NULL, NULL, NULL, '2026-08-18 15:46:08', '2026-08-18 15:46:08'),
(39, 'JAS-2026-000039', 'Sandreas Lima', '980346-12-0534', 'w1llyblly00@gmail.com', 'w1llyblly00@gmail.com', '012-9657489', '', 'Jurutera (J41)', '2026-08-18', 'Syarikat Sembilan Sdn Bhd', '2026-08-28', '18:50:00', 'LOJI', 'Kg Air, KK', 'test', 'SAB 456 W', 'DIHANTAR', NULL, NULL, NULL, NULL, NULL, NULL, '2026-08-18 15:54:10', '2026-08-18 15:54:10'),
(40, 'JAS-2026-000040', 'Sandreas Lima', '980346-12-0534', 'w1llyblly00@gmail.com', 'w1llyblly00@gmail.com', '012-9657489', '', 'Jurutera (J41)', '2026-08-18', 'Syarikat Sembilan Sdn Bhd', '2026-08-28', '18:06:00', 'LOJI', 'Kg Air, KK', 'test', 'SAB 456 W', 'DITOLAK', '', '', 1, 2, '2026-08-24 10:30:58', NULL, '2026-08-18 16:01:39', '2026-08-24 10:31:02'),
(41, 'JAS-2026-000041', 'Sandreas Santro', '980346-12-0534', 'w1llyblly00@gmail.com', 'w1llyblly00@gmail.com', '012-9657489', '', 'Jurutera (J41)', '2026-08-18', 'Syarikat Sembilan Sdn Bhd', '2026-08-28', '08:30:00', 'LOJI', 'Kg Air, KK', 'test', 'SAB 456 W', 'DITOLAK', '', '', 1, 2, '2026-08-24 10:37:08', NULL, '2026-08-19 07:33:31', '2026-08-24 10:37:11'),
(42, 'JAS-2026-000042', 'Test', '123', 'test@example.com', 'test@example.com', '0123456789', '', 'Test', '2026-08-19', 'JANS', '2026-08-24', '10:00:00', 'LOJI', 'Test', 'Testing email', '', 'DITOLAK', '', '', 1, 2, '2026-08-24 10:34:04', NULL, '2026-08-19 07:36:44', '2026-08-24 10:34:08'),
(43, 'JAS-2026-000043', 'Test', '123', 'test@example.com', 'test@example.com', '0123456789', '', 'Test', '2026-08-19', 'JANS', '2026-08-24', '10:00:00', 'LOJI', 'Test', 'Testing email', '', 'DITOLAK', '', '', 1, 2, '2026-08-24 10:33:35', NULL, '2026-08-19 07:41:04', '2026-08-24 10:33:39'),
(44, 'JAS-2026-000044', 'Test', '123', 'test@example.com', 'test@example.com', '0123456789', '', 'Test', '2026-08-19', 'JANS', '2026-08-24', '10:00:00', 'LOJI', 'Test', 'Testing email', '', 'DITOLAK', '', '', 1, 2, '2026-08-19 09:20:36', NULL, '2026-08-19 07:45:55', '2026-08-19 09:20:40'),
(45, 'JAS-2026-000045', 'Sandreas Miami', '980346-12-0534', 'w1llyblly00@gmail.com', 'w1llyblly00@gmail.com', '012-9657489', '', 'Jurutera (J41)', '2026-08-18', 'Syarikat Sembilan Sdn Bhd', '2026-08-28', '08:50:00', 'LOJI', 'Kg Air, KK', 'test', 'SAB 456 W', 'DILULUSKAN', '', '', 1, 2, '2026-08-19 09:04:22', NULL, '2026-08-19 07:53:28', '2026-08-19 09:04:26'),
(46, 'JAS-2026-000046', 'SInma Rilas', '980346-12-0534', 'w1llyblly00@gmail.com', 'w1llyblly00@gmail.com', '012-9657489', '', 'Jurutera (J41)', '2026-08-18', 'Syarikat Sembilan Sdn Bhd', '2026-08-28', '09:00:00', 'LOJI', 'Kg Air, KK', 'test', 'SAB 456 W', 'DILULUSKAN', '', '', 1, 2, '2026-08-19 09:04:19', NULL, '2026-08-19 08:02:03', '2026-08-19 09:04:22'),
(47, 'JAS-2026-000047', 'Sandreas Santro', '980346-12-0534', 'w1llyblly00@gmail.com', 'w1llyblly00@gmail.com', '012-9657489', '', 'Jurutera (J41)', '2026-08-19', 'Syarikat Sembilan Sdn Bhd', '2026-08-28', '09:06:00', 'LOJI', 'Kg Air, KK', 'test', 'SAB 456 W', 'DILULUSKAN', '', '', 1, 2, '2026-08-19 09:04:15', NULL, '2026-08-19 08:06:21', '2026-08-19 09:04:19'),
(49, 'JAS-2026-000048', 'Sandreas Santro', '980346-12-0534', 'w1llyblly00@gmail.com', 'w1llyblly00@gmail.com', '012-9657489', '088-564567876', 'Jurutera (J41)', '2026-08-19', 'Syarikat Sembilan Sdn Bhd', '2026-08-28', '10:00:00', 'LOJI', 'Kg Air, KK', 'test', 'SAB 456 W', 'DILULUSKAN', '', '', 1, 2, '2026-08-19 09:04:10', NULL, '2026-08-19 09:01:02', '2026-08-19 09:04:15'),
(50, 'JAS-2026-000049', 'Sandreas Santro', '980346-12-0534', 'w1llyblly00@gmail.com', 'w1llyblly00@gmail.com', '012-9657489', '088-564567876', 'Jurutera (J41)', '2026-08-19', 'Syarikat Sembilan Sdn Bhd', '2026-08-28', '11:00:00', 'LOJI', 'Kg Air, KK', 'test', 'SAB 456 W', 'DILULUSKAN', '', '', 1, 2, '2026-08-19 10:04:15', NULL, '2026-08-19 10:01:44', '2026-08-19 10:04:19'),
(51, 'JAS-2026-000050', 'Sandreas Santro', '980346-12-0534', 'w1llyblly00@gmail.com', 'w1llyblly00@gmail.com', '012-9657489', '088-564567876', 'Jurutera (J41)', '2026-08-19', 'Syarikat Sembilan Sdn Bhd', '2026-09-22', '10:30:00', 'LOJI', 'Kg Air, KK', 'test', 'SAB 456 W', 'DILULUSKAN', '', '', 1, 2, '2026-08-19 10:30:58', NULL, '2026-08-19 10:25:20', '2026-08-19 10:31:02'),
(52, 'JAS-2026-000051', 'Sandreas Santro', '980346-12-0534', 'w1llyblly00@gmail.com', 'w1llyblly00@gmail.com', '012-9657489', '088-564567876', 'Jurutera (J41)', '2026-08-19', 'Syarikat Sembilan Sdn Bhd', '2026-09-22', '11:53:00', 'LOJI', 'Kg Air, KK', 'testt', 'SAB 456 W', 'PAS_DIKELUARKAN', '', '', 1, 2, '2026-08-19 11:55:26', 'Ya4wfNY8Q3TNVHeZnz7sFBKwD4VWSw8P', '2026-08-19 11:54:09', '2026-08-19 11:55:30'),
(53, 'JAS-2026-000052', 'Sandreas Santro', '980346-12-0534', 'w1llyblly00@gmail.com', 'w1llyblly00@gmail.com', '012-9657489', '088-564567876', 'Jurutera (J41)', '2026-08-19', 'Syarikat Sembilan Sdn Bhd', '2026-09-22', '14:34:00', 'LOJI', 'Kg Air, KK', 'testttt', 'SAB 456 W', 'DIHANTAR', NULL, NULL, NULL, NULL, NULL, NULL, '2026-08-19 14:34:57', '2026-08-19 14:34:57'),
(54, 'JAS-2026-000053', 'Jamrus Janis', '342957-12-0564', 'w1llyblly00@gmail.com', 'w1llyblly00@gmail.com', '012-78698733333333333', '088-908767', 'Jurutera (J41)', '2026-08-21', 'Syarikat Satu Sdn Bhd', '2026-08-25', '00:30:00', 'LOJI', 'Loji Air Tuaran', 'testtt2', 'SAB 435 W', 'PAS_DIKELUARKAN', '', '', 1, 2, '2026-08-21 10:24:25', 'ugAb00qAWGGq-Gskv3DTO6R0Vu3nhgPf', '2026-08-21 10:21:50', '2026-08-21 10:24:30'),
(55, 'JAS-2026-000054', 'Alana Liora Gantari', '987896-12-0932', 'w1llyblly00@gmail.com', 'w1llyblly00@gmail.com', '017-98324236', '089-732456', 'Jurutera (J41)', '2026-08-21', 'Syarikat Nila Sdn Bhd', '2026-08-26', '13:30:00', 'INTAKE', 'Intake Bandau', 'testtt4', 'SAB 789 W', 'PAS_DIKELUARKAN', '', '', 1, 2, '2026-08-21 10:36:23', 'cQdq1WVtmxZga65ry8zQd32WXNJAsTkg', '2026-08-21 10:35:02', '2026-08-21 10:36:27'),
(56, 'JAS-2026-000055', 'Kirana Dinas', '897689-12-0564', 'w1llyblly00@gmail.com', 'w1llyblly00@gmail.com', '012-78698733333333333', '', 'Jurutera (J41)', '2026-08-24', 'Syarikat Satu Sdn Bhd', '2026-09-01', '14:00:00', 'INTAKE', 'Intake Bandau', 'testt5', 'SAB 435 W', 'MENUNGGU_PENGARAH', '', NULL, 1, NULL, NULL, NULL, '2026-08-24 10:55:56', '2026-08-24 10:57:25'),
(57, 'JAS-2026-000056', 'Sandreas Tujuh', '099809-12-8796', 'w1llyblly00@gmail.com', 'w1llyblly00@gmail.com', '013-9897659', '088-564567876', 'Jurutera (J41)', '2026-08-27', 'Syarikat Tujuh Sdn Bhd', '2026-09-02', '13:30:00', 'LOJI', 'LRA Telibong I', 'testtt01', 'SAB 456 W', 'PAS_DIKELUARKAN', '', '', 1, 2, '2026-08-27 11:25:41', 'd3w4L47jZVtaYQ4uAXosp5D39oCZ74A5', '2026-08-27 11:23:44', '2026-08-27 11:25:44'),
(61, 'JAS-2026-000057', 'Sina Marlin', '099809-12-8796', 'w1llyblly00@gmail.com', 'w1llyblly00@gmail.com', '014-6765489', '088-564567876', 'Jurutera (J41)', '2026-08-27', 'Syarikat Tujuh Sdn Bhd', '2026-09-02', '14:00:00', 'LOJI', 'LRA Topokon', 'testtt02', 'SAB 456 W', 'DITOLAK', '', '', 1, 2, '2026-08-27 11:42:58', NULL, '2026-08-27 11:41:50', '2026-08-27 11:43:02'),
(62, 'JAS-2026-000058', 'Winsom Kinarun', '099809-12-8796', 'w1llyblly00@gmail.com', 'w1llyblly00@gmail.com', '015-8796568', '088-564567876', 'Jurutera (J41)', '2026-08-27', 'Syarikat Tujuh Sdn Bhd', '2026-09-02', '17:30:00', 'INTAKE', 'Intake Bandau', 'testttt09', 'SAB 456 W', 'PAS_DIKELUARKAN', '', '', 1, 2, '2026-08-27 12:20:33', 'MHcR6dUjk0fqjo6czlJOwWmT8HCXx1-A', '2026-08-27 12:19:14', '2026-08-27 12:20:37'),
(63, 'JAS-2026-000059', 'Lindah Miza', '098790-12-8796', 'w1llyblly00@gmail.com', 'w1llyblly00@gmail.com', '014-6765489', '088-564567876', 'Jurutera (J41)', '2026-09-01', 'Syarikat Sembilan Sdn Bhd', '2026-09-10', '10:30:00', 'LOJI', 'LRA Telibong I', 'testt', 'SAB 456 W', 'PAS_DIKELUARKAN', '', '', 1, 2, '2026-09-02 07:22:46', 'XaNVG0HQD6xkZD0vzkxIXjcTubVb5B-D', '2026-09-02 07:21:30', '2026-09-02 07:22:51'),
(64, 'JAS-2026-000060', 'Lindah Miza', '098790-12-8796', 'w1llyblly00@gmail.com', 'w1llyblly00@gmail.com', '014-6765489', '088-564567876', 'Jurutera (J41)', '2026-09-02', 'Syarikat Sembilan Sdn Bhd', '2026-09-10', '00:30:00', 'LOJI', 'LRA Tamparuli', 'test', 'SAB 456 W', 'PAS_DIKELUARKAN', '', '', 1, 2, '2026-09-02 10:31:47', 'gf53vsSHxxODEDQNzUJpnPq26am8c_Ck', '2026-09-02 10:16:46', '2026-09-02 10:31:52'),
(65, 'JAS-2026-000061', 'Amanda Lindrus', '675311-12-4532', NULL, NULL, '012-9878967', '088-564567876', 'Jurutera (J41)', '2026-09-03', 'Syarikat Sembilan Sdn Bhd', '2026-09-17', '10:30:00', 'LOJI', 'LRA Topokon', 'testt', 'SAB 124 W', 'DIHANTAR', NULL, NULL, NULL, NULL, NULL, NULL, '2026-09-03 09:27:32', '2026-09-03 09:27:32'),
(66, 'JAS-2026-000062', 'Amanda Lindrus', '675311-12-4532', NULL, 'w1llyblly00@gmail.com', '012-9878967', '088-564567876', 'Jurutera (J41)', '2026-09-03', 'Syarikat Sembilan Sdn Bhd', '2026-09-17', '10:30:00', 'LOJI', 'LRA Telibong I', 'testt', 'SAB 124 W', 'MENUNGGU_PENGARAH', '', NULL, 1, NULL, NULL, NULL, '2026-09-03 09:33:32', '2026-09-03 09:35:51'),
(67, 'JAS-2026-000063', 'Amanda Lindrus', '675311-12-4532', NULL, 'w1llyblly00@gmail.com', '012-9878967', '088-564567876', 'Jurutera (J41)', '2026-09-03', 'Syarikat Sembilan Sdn Bhd', '2026-09-17', '11:00:00', 'LOJI', 'LRA Telibong I', 'testttt', 'SAB 124 W', 'DITOLAK', '', 'Tarikh lawatan tidak sesuai', 1, 2, '2026-09-03 10:15:36', NULL, '2026-09-03 09:59:33', '2026-09-03 10:15:39'),
(68, 'JAS-2026-000064', 'Amanda Lindrus', '675311-12-4532', NULL, 'w1llyblly00@gmail.com', '012-9878967', '088-564567876', 'Jurutera (J41)', '2026-09-03', 'Syarikat Lambung Sdn Bhd', '2026-09-17', '07:30:00', 'LOJI', 'LRA Telibong I', 'testt', 'SAB 124 W', 'DIHANTAR', NULL, NULL, NULL, NULL, NULL, NULL, '2026-09-04 07:30:27', '2026-09-04 07:30:27');

--
-- Indexes for dumped tables
--

--
-- Indexes for table `applications`
--
ALTER TABLE `applications`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `application_no` (`application_no`),
  ADD UNIQUE KEY `pass_token` (`pass_token`),
  ADD KEY `fk_reviewed_by` (`reviewed_by`),
  ADD KEY `fk_decided_by` (`decided_by`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `applications`
--
ALTER TABLE `applications`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=69;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `applications`
--
ALTER TABLE `applications`
  ADD CONSTRAINT `fk_decided_by` FOREIGN KEY (`decided_by`) REFERENCES `users` (`id`),
  ADD CONSTRAINT `fk_reviewed_by` FOREIGN KEY (`reviewed_by`) REFERENCES `users` (`id`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
