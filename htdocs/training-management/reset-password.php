<?php
/**
 * Psyco Time X Pro - Halaman Rasmi Cipta Kata Laluan Baharu (Password Reset Page)
 * Lokasi: htdocs/training-management/reset-password.php
 * Dibuka apabila pengguna menekan pautan "Forgot Password" dari dalam Gmail mereka.
 */
$email = isset($_GET['email']) ? htmlspecialchars(strip_tags($_GET['email']), ENT_QUOTES, 'UTF-8') : '';
$token = isset($_GET['token']) ? htmlspecialchars(strip_tags($_GET['token']), ENT_QUOTES, 'UTF-8') : '';
?>
<!DOCTYPE html>
<html lang="ms">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Cipta Kata Laluan Baharu | Psyco Time X Pro Security</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css" rel="stylesheet">
    <link href="https://fonts.googleapis.com/css2?family=Teko:wght@600;700&family=Inter:wght@400;600;800&display=swap" rel="stylesheet">
</head>
<body class="bg-[#050811] text-gray-100 min-h-screen flex items-center justify-center p-4 font-sans">
    <div class="max-w-md w-full bg-[#111827] border-2 border-[#06B6D4]/50 rounded-3xl p-6 sm:p-8 shadow-2xl space-y-6">
        <div class="text-center space-y-2">
            <div class="w-14 h-14 rounded-2xl bg-[#06B6D4]/15 border border-[#06B6D4] mx-auto flex items-center justify-center text-[#06B6D4] text-2xl">
                <i class="fa-solid fa-shield-halved"></i>
            </div>
            <h1 class="text-3xl font-bold uppercase tracking-wider text-white" style="font-family: 'Teko', sans-serif;">
                CIPTA KATA LALUAN BAHARU
            </h1>
            <p class="text-xs text-gray-400">
                Pautan Pemulihan Akaun Gmail • Enkripsi Keselamatan Bcrypt &amp; Firebase Auth
            </p>
        </div>

        <form id="resetPasswordForm" onsubmit="submitNewPassword(event)" class="space-y-4">
            <div>
                <label class="block text-xs font-bold text-gray-300 mb-1">Akaun Gmail Anda</label>
                <input type="email" id="resetEmail" value="<?php echo $email; ?>" required readonly class="w-full px-4 py-2.5 rounded-xl bg-[#0A0F1D] border border-gray-700 text-[#06B6D4] text-xs font-mono">
            </div>

            <input type="hidden" id="resetToken" value="<?php echo $token; ?>">

            <div>
                <label class="block text-xs font-bold text-gray-300 mb-1">Kata Laluan Baharu (Min 6 Aksara) *</label>
                <input type="password" id="newPassword" required minlength="6" placeholder="Masukkan kata laluan baharu" class="w-full px-4 py-2.5 rounded-xl bg-[#0A0F1D] border border-gray-700 text-white text-sm focus:outline-none focus:border-[#06B6D4]">
            </div>

            <div>
                <label class="block text-xs font-bold text-gray-300 mb-1">Sahkan Kata Laluan Baharu *</label>
                <input type="password" id="confirmPassword" required minlength="6" placeholder="Taip semula kata laluan baharu" class="w-full px-4 py-2.5 rounded-xl bg-[#0A0F1D] border border-gray-700 text-white text-sm focus:outline-none focus:border-[#06B6D4]">
            </div>

            <div id="resetStatusBox" class="hidden p-3.5 rounded-xl text-xs font-semibold"></div>

            <button type="submit" id="btnSubmitReset" class="w-full py-3.5 rounded-xl bg-gradient-to-r from-[#06B6D4] to-blue-600 hover:from-cyan-400 hover:to-blue-500 text-black font-extrabold text-sm shadow-lg transition">
                <i class="fa-solid fa-key mr-1"></i> Simpan Kata Laluan Baharu &amp; Log Masuk
            </button>
        </form>

        <div class="pt-4 border-t border-gray-800 text-center space-y-2 text-xs">
            <a href="index.php" class="text-[#06B6D4] hover:underline font-bold inline-flex items-center gap-1.5">
                <i class="fa-solid fa-arrow-left"></i> Kembali ke Halaman Log Masuk Utama
            </a>
        </div>
    </div>

    <script>
        async function submitNewPassword(e) {
            e.preventDefault();
            const email = document.getElementById('resetEmail').value.trim();
            const token = document.getElementById('resetToken').value.trim();
            const pass1 = document.getElementById('newPassword').value.trim();
            const pass2 = document.getElementById('confirmPassword').value.trim();
            const statusBox = document.getElementById('resetStatusBox');
            const btn = document.getElementById('btnSubmitReset');

            if (pass1.length < 6) {
                statusBox.className = 'p-3.5 rounded-xl text-xs font-semibold bg-red-500/20 border border-red-500 text-red-300';
                statusBox.textContent = 'Kata laluan baharu mestilah sekurang-kurangnya 6 aksara.';
                statusBox.classList.remove('hidden');
                return;
            }
            if (pass1 !== pass2) {
                statusBox.className = 'p-3.5 rounded-xl text-xs font-semibold bg-red-500/20 border border-red-500 text-red-300';
                statusBox.textContent = 'Kata laluan baharu dan pengesahan kata laluan tidak sepadan.';
                statusBox.classList.remove('hidden');
                return;
            }

            btn.disabled = true;
            btn.textContent = 'Menyimpan Kata Laluan Baharu...';

            try {
                const res = await fetch('api/auth-security.php', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        action: 'reset_password',
                        email: email,
                        token: token,
                        newPassword: pass1
                    })
                });
                const data = await res.json();
                if (data.status === 'success') {
                    statusBox.className = 'p-3.5 rounded-xl text-xs font-semibold bg-emerald-500/20 border border-emerald-500 text-emerald-300';
                    statusBox.innerHTML = '<i class="fa-solid fa-circle-check mr-1"></i> ' + data.message + ' Mengalihkan anda ke halaman log masuk...';
                    statusBox.classList.remove('hidden');
                    setTimeout(() => {
                        window.location.href = 'index.php';
                    }, 2200);
                } else {
                    statusBox.className = 'p-3.5 rounded-xl text-xs font-semibold bg-red-500/20 border border-red-500 text-red-300';
                    statusBox.textContent = data.message || 'Gagal menetapkan semula kata laluan.';
                    statusBox.classList.remove('hidden');
                    btn.disabled = false;
                    btn.textContent = 'Simpan Kata Laluan Baharu & Log Masuk';
                }
            } catch (err) {
                statusBox.className = 'p-3.5 rounded-xl text-xs font-semibold bg-red-500/20 border border-red-500 text-red-300';
                statusBox.textContent = 'Ralat sambungan pelayan.';
                statusBox.classList.remove('hidden');
                btn.disabled = false;
            }
        }
    </script>
</body>
</html>
