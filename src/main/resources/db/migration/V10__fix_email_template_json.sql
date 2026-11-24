UPDATE system_settings SET setting_value = '{"name":"Permohonan Diluluskan","subject":"Permohonan Diluluskan - {{applicationNo}}","body":"<p>Tuan/Puan,</p><p>Permohonan akses anda dengan nombor <b>{{applicationNo}}</b> telah <b>diluluskan</b> oleh Pengarah Jabatan Air Sabah dan pas kebenaran telah dikeluarkan.</p><p>Sila semak status permohonan untuk memilih tindakan seterusnya (cetak PDF atau tunjuk QR). <a href=\\"{{link}}\\">Klik sini</a>.</p><p>Terima kasih.</p>","enabled":true}'
WHERE category = 'email-template' AND setting_key = 'decision-approved';

UPDATE system_settings SET setting_value = '{"name":"Kod Reset Kata Laluan","subject":"Kod Reset Kata Laluan JAS","body":"<p>Tuan/Puan,</p><p>Kod 6 digit untuk reset kata laluan akaun JAS anda ialah:</p><p style=\\"font-size:1.4rem;font-weight:700;letter-spacing:0.12em;\\">{{code}}</p><p>Kod ini sah selama <b>10 minit</b>.</p><p>Jika anda tidak meminta reset kata laluan, sila abaikan e-mel ini.</p>","enabled":true}'
WHERE category = 'email-template' AND setting_key = 'password-reset-code';

UPDATE system_settings SET setting_value = '{"name":"Kata Laluan Baharu","subject":"Reset Kata Laluan JAS","body":"<p>Tuan/Puan,</p><p>Kata laluan baharu untuk akaun JAS anda telah dijana.</p><p><b>Kata laluan baharu:</b></p><p style=\\"font-size:1.2rem;font-weight:700;\\">{{password}}</p><p>Sila log masuk semula menggunakan kata laluan ini.</p><p>Terima kasih.</p>","enabled":true}'
WHERE category = 'email-template' AND setting_key = 'password-reset-complete';

UPDATE system_settings SET setting_value = '{"name":"Kod Semakan Permohonan","subject":"Kod Pengesahan Semakan Permohonan JAS","body":"<p>Tuan/Puan,</p><p>Kod pengesahan untuk melihat status permohonan JAS anda ialah:</p><p style=\\"font-size:1.4rem;font-weight:700;letter-spacing:0.12em;\\">{{code}}</p><p>Kod ini sah selama 10 minit. Jangan kongsi kod ini dengan orang lain.</p><p>Jika anda tidak meminta kod ini, sila abaikan e-mel ini.</p>","enabled":true}'
WHERE category = 'email-template' AND setting_key = 'application-access-code';
