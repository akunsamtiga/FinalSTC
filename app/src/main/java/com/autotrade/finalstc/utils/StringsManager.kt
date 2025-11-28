package com.autotrade.finalstc.utils

object StringsManager {

    fun getLoginTitle(lang: String) = when(lang) {
        "id" -> "Login"
        "en" -> "Login"
        "es" -> "Iniciar Sesión"
        "vi" -> "Đăng Nhập"
        "tr" -> "Giriş"
        "hi" -> "लॉगिन"
        "ms" -> "Log Masuk"
        "bn" -> "লগইন"
        "ru" -> "Вход"
        else -> "Login"
    }

    fun getEmailLabel(lang: String) = when(lang) {
        "id" -> "Email"
        "en" -> "Email"
        "es" -> "Correo Electrónico"
        "vi" -> "Email"
        "tr" -> "E-posta"
        "hi" -> "ईमेल"
        "ms" -> "E-mel"
        "bn" -> "ইমেল"
        "ru" -> "Электронная почта"
        else -> "Email"
    }

    fun getPasswordLabel(lang: String) = when(lang) {
        "id" -> "Password"
        "en" -> "Password"
        "es" -> "Contraseña"
        "vi" -> "Mật Khẩu"
        "tr" -> "Şifre"
        "hi" -> "पासवर्ड"
        "ms" -> "Kata Laluan"
        "bn" -> "পাসওয়ার্ড"
        "ru" -> "Пароль"
        else -> "Password"
    }

    fun getRememberMe(lang: String) = when(lang) {
        "id" -> "Ingat saya"
        "en" -> "Remember me"
        "es" -> "Recuérdame"
        "vi" -> "Ghi nhớ đăng nhập"
        "tr" -> "Beni hatırla"
        "hi" -> "मुझे याद रखें"
        "ms" -> "Ingat saya"
        "bn" -> "আমাকে মনে রাখুন"
        "ru" -> "Запомнить меня"
        else -> "Ingat saya"
    }

    fun getLoginButton(lang: String) = when(lang) {
        "id" -> "Masuk"
        "en" -> "Sign In"
        "es" -> "Entrar"
        "vi" -> "Đăng Nhập"
        "tr" -> "Giriş Yap"
        "hi" -> "साइन इन करें"
        "ms" -> "Log Masuk"
        "bn" -> "সাইন ইন"
        "ru" -> "Войти"
        else -> "Masuk"
    }

    fun getRegisterButton(lang: String) = when(lang) {
        "id" -> "Daftar Akun"
        "en" -> "Create Account"
        "es" -> "Crear Cuenta"
        "vi" -> "Tạo Tài Khoản"
        "tr" -> "Hesap Oluştur"
        "hi" -> "खाता बनाएं"
        "ms" -> "Daftar Akaun"
        "bn" -> "অ্যাকাউন্ট তৈরি করুন"
        "ru" -> "Создать аккаунт"
        else -> "Daftar Akun"
    }

    fun getNeedHelp(lang: String) = when(lang) {
        "id" -> "Butuh Bantuan?"
        "en" -> "Need Help?"
        "es" -> "¿Necesitas Ayuda?"
        "vi" -> "Cần Trợ Giúp?"
        "tr" -> "Yardıma İhtiyacınız Var mı?"
        "hi" -> "मदद चाहिए?"
        "ms" -> "Perlukan Bantuan?"
        "bn" -> "সাহায্য প্রয়োজন?"
        "ru" -> "Нужна помощь?"
        else -> "Butuh Bantuan?"
    }

    fun getOrText(lang: String) = when(lang) {
        "id" -> "atau"
        "en" -> "or"
        "es" -> "o"
        "vi" -> "hoặc"
        "tr" -> "veya"
        "hi" -> "या"
        "ms" -> "atau"
        "bn" -> "অথবা"
        "ru" -> "или"
        else -> "atau"
    }

    // Validation Messages
    fun getEmailEmpty(lang: String) = when(lang) {
        "id" -> "Email tidak boleh kosong"
        "en" -> "Email cannot be empty"
        "es" -> "El correo no puede estar vacío"
        "vi" -> "Email không được để trống"
        "tr" -> "E-posta boş olamaz"
        "hi" -> "ईमेल खाली नहीं हो सकता"
        "ms" -> "E-mel tidak boleh kosong"
        "bn" -> "ইমেল খালি থাকতে পারে না"
        "ru" -> "Email не может быть пустым"
        else -> "Email tidak boleh kosong"
    }

    fun getPasswordEmpty(lang: String) = when(lang) {
        "id" -> "Password tidak boleh kosong"
        "en" -> "Password cannot be empty"
        "es" -> "La contraseña no puede estar vacía"
        "vi" -> "Mật khẩu không được để trống"
        "tr" -> "Şifre boş olamaz"
        "hi" -> "पासवर्ड खाली नहीं हो सकता"
        "ms" -> "Kata laluan tidak boleh kosong"
        "bn" -> "পাসওয়ার্ড খালি থাকতে পারে না"
        "ru" -> "Пароль не может быть пустым"
        else -> "Password tidak boleh kosong"
    }

    fun getEmailInvalid(lang: String) = when(lang) {
        "id" -> "Format email tidak valid"
        "en" -> "Invalid email format"
        "es" -> "Formato de correo inválido"
        "vi" -> "Định dạng email không hợp lệ"
        "tr" -> "Geçersiz e-posta formatı"
        "hi" -> "अमान्य ईमेल प्रारूप"
        "ms" -> "Format e-mel tidak sah"
        "bn" -> "অবৈধ ইমেল ফরম্যাট"
        "ru" -> "Неверный формат email"
        else -> "Format email tidak valid"
    }

    fun getPasswordTooShort(lang: String) = when(lang) {
        "id" -> "Password minimal 6 karakter"
        "en" -> "Password must be at least 6 characters"
        "es" -> "La contraseña debe tener al menos 6 caracteres"
        "vi" -> "Mật khẩu phải có ít nhất 6 ký tự"
        "tr" -> "Şifre en az 6 karakter olmalıdır"
        "hi" -> "पासवर्ड कम से कम 6 अक्षर का होना चाहिए"
        "ms" -> "Kata laluan mestilah sekurang-kurangnya 6 aksara"
        "bn" -> "পাসওয়ার্ড কমপক্ষে ৬ অক্ষরের হতে হবে"
        "ru" -> "Пароль должен содержать минимум 6 символов"
        else -> "Password minimal 6 karakter"
    }

    // Loading Messages
    fun getLoadingDefault(lang: String) = when(lang) {
        "id" -> "Sedang memuat..."
        "en" -> "Loading..."
        "es" -> "Cargando..."
        "vi" -> "Đang tải..."
        "tr" -> "Yükleniyor..."
        "hi" -> "लोड हो रहा है..."
        "ms" -> "Memuatkan..."
        "bn" -> "লোড হচ্ছে..."
        "ru" -> "Загрузка..."
        else -> "Sedang memuat..."
    }

    fun getLoadingStockity(lang: String) = when(lang) {
        "id" -> "Menghubungi server Stockity..."
        "en" -> "Connecting to Stockity server..."
        "es" -> "Conectando al servidor Stockity..."
        "vi" -> "Đang kết nối máy chủ Stockity..."
        "tr" -> "Stockity sunucusuna bağlanılıyor..."
        "hi" -> "Stockity सर्वर से कनेक्ट हो रहा है..."
        "ms" -> "Menghubungi pelayan Stockity..."
        "bn" -> "Stockity সার্ভারে সংযোগ করা হচ্ছে..."
        "ru" -> "Подключение к серверу Stockity..."
        else -> "Menghubungi server Stockity..."
    }

    fun getLoadingAuth(lang: String) = when(lang) {
        "id" -> "Melakukan autentikasi Stockity..."
        "en" -> "Authenticating with Stockity..."
        "es" -> "Autenticando con Stockity..."
        "vi" -> "Đang xác thực với Stockity..."
        "tr" -> "Stockity ile kimlik doğrulanıyor..."
        "hi" -> "Stockity के साथ प्रमाणित हो रहा है..."
        "ms" -> "Mengesahkan dengan Stockity..."
        "bn" -> "Stockity এর সাথে প্রমাণীকরণ করা হচ্ছে..."
        "ru" -> "Аутентификация в Stockity..."
        else -> "Melakukan autentikasi Stockity..."
    }

    fun getLoadingAccess(lang: String) = when(lang) {
        "id" -> "Memeriksa akses aplikasi..."
        "en" -> "Checking app access..."
        "es" -> "Verificando acceso a la aplicación..."
        "vi" -> "Đang kiểm tra quyền truy cập..."
        "tr" -> "Uygulama erişimi kontrol ediliyor..."
        "hi" -> "ऐप एक्सेस की जांच हो रही है..."
        "ms" -> "Memeriksa akses aplikasi..."
        "bn" -> "অ্যাপ অ্যাক্সেস পরীক্ষা করা হচ্ছে..."
        "ru" -> "Проверка доступа к приложению..."
        else -> "Memeriksa akses aplikasi..."
    }

    fun getLoginSuccess(lang: String) = when(lang) {
        "id" -> "Login berhasil!"
        "en" -> "Login successful!"
        "es" -> "¡Inicio de sesión exitoso!"
        "vi" -> "Đăng nhập thành công!"
        "tr" -> "Giriş başarılı!"
        "hi" -> "लॉगिन सफल!"
        "ms" -> "Log masuk berjaya!"
        "bn" -> "লগইন সফল!"
        "ru" -> "Вход выполнен успешно!"
        else -> "Login berhasil!"
    }

    fun getShowPassword(lang: String) = when(lang) {
        "id" -> "Tampilkan password"
        "en" -> "Show password"
        "es" -> "Mostrar contraseña"
        "vi" -> "Hiển thị mật khẩu"
        "tr" -> "Şifreyi göster"
        "hi" -> "पासवर्ड दिखाएं"
        "ms" -> "Tunjukkan kata laluan"
        "bn" -> "পাসওয়ার্ড দেখান"
        "ru" -> "Показать пароль"
        else -> "Tampilkan password"
    }

    fun getHidePassword(lang: String) = when(lang) {
        "id" -> "Sembunyikan password"
        "en" -> "Hide password"
        "es" -> "Ocultar contraseña"
        "vi" -> "Ẩn mật khẩu"
        "tr" -> "Şifreyi gizle"
        "hi" -> "पासवर्ड छुपाएं"
        "ms" -> "Sembunyikan kata laluan"
        "bn" -> "পাসওয়ার্ড লুকান"
        "ru" -> "Скрыть пароль"
        else -> "Sembunyikan password"
    }

    fun getSelectLanguage(lang: String) = when(lang) {
        "id" -> "Pilih Bahasa"
        "en" -> "Select Language"
        "es" -> "Seleccionar Idioma"
        "vi" -> "Chọn Ngôn Ngữ"
        "tr" -> "Dil Seçin"
        "hi" -> "भाषा चुनें"
        "ms" -> "Pilih Bahasa"
        "bn" -> "ভাষা নির্বাচন করুন"
        "ru" -> "Выберите язык"
        else -> "Pilih Bahasa"
    }

    fun getSelectCountry(lang: String) = when(lang) {
        "id" -> "Pilih Negara"
        "en" -> "Select Country"
        "es" -> "Seleccionar País"
        "vi" -> "Chọn Quốc Gia"
        "tr" -> "Ülke Seçin"
        "hi" -> "देश चुनें"
        "ms" -> "Pilih Negara"
        "bn" -> "দেশ নির্বাচন করুন"
        "ru" -> "Выберите страну"
        else -> "Pilih Negara"
    }

    fun getTradingHistory(lang: String) = when(lang) {
        "id" -> "Riwayat Trading"
        "en" -> "Trading History"
        "es" -> "Historial de Trading"
        "vi" -> "Lịch Sử Giao Dịch"
        "tr" -> "İşlem Geçmişi"
        "hi" -> "ट्रेडिंग इतिहास"
        "ms" -> "Sejarah Dagangan"
        "bn" -> "ট্রেডিং ইতিহাস"
        "ru" -> "История торговли"
        else -> "Riwayat Trading"
    }

    fun getDemoAccount(lang: String) = when(lang) {
        "id" -> "Demo"
        "en" -> "Demo"
        "es" -> "Demo"
        "vi" -> "Demo"
        "tr" -> "Demo"
        "hi" -> "डेमो"
        "ms" -> "Demo"
        "bn" -> "ডেমো"
        "ru" -> "Демо"
        else -> "Demo"
    }

    fun getRealAccount(lang: String) = when(lang) {
        "id" -> "Real"
        "en" -> "Real"
        "es" -> "Real"
        "vi" -> "Thật"
        "tr" -> "Gerçek"
        "hi" -> "असली"
        "ms" -> "Sebenar"
        "bn" -> "আসল"
        "ru" -> "Реальный"
        else -> "Real"
    }

    fun getThisWeek(lang: String) = when(lang) {
        "id" -> "MINGGU INI"
        "en" -> "THIS WEEK"
        "es" -> "ESTA SEMANA"
        "vi" -> "TUẦN NÀY"
        "tr" -> "BU HAFTA"
        "hi" -> "इस सप्ताह"
        "ms" -> "MINGGU INI"
        "bn" -> "এই সপ্তাহ"
        "ru" -> "НА ЭТОЙ НЕДЕЛЕ"
        else -> "MINGGU INI"
    }

    fun getFilter(lang: String) = when(lang) {
        "id" -> "Filter"
        "en" -> "Filter"
        "es" -> "Filtrar"
        "vi" -> "Lọc"
        "tr" -> "Filtre"
        "hi" -> "फ़िल्टर"
        "ms" -> "Tapis"
        "bn" -> "ফিল্টার"
        "ru" -> "Фильтр"
        else -> "Filter"
    }

    fun getToggleAccount(lang: String) = when(lang) {
        "id" -> "Tukar Akun"
        "en" -> "Toggle Account"
        "es" -> "Cambiar Cuenta"
        "vi" -> "Chuyển Tài Khoản"
        "tr" -> "Hesap Değiştir"
        "hi" -> "खाता बदलें"
        "ms" -> "Tukar Akaun"
        "bn" -> "অ্যাকাউন্ট পরিবর্তন করুন"
        "ru" -> "Переключить аккаунт"
        else -> "Tukar Akun"
    }

    fun getThisWeekPerformance(lang: String) = when(lang) {
        "id" -> "Performa Minggu Ini"
        "en" -> "Week Performance"
        "es" -> "Rendimiento de Esta Semana"
        "vi" -> "Hiệu Suất Tuần Này"
        "tr" -> "Bu Hafta Performansı"
        "hi" -> "इस सप्ताह का प्रदर्शन"
        "ms" -> "Prestasi Minggu Ini"
        "bn" -> "এই সপ্তাহের পারফরম্যান্স"
        "ru" -> "Производительность недели"
        else -> "Performa Minggu Ini"
    }

    fun getWinRate(lang: String) = when(lang) {
        "id" -> "Win Rate"
        "en" -> "Win Rate"
        "es" -> "Tasa de Éxito"
        "vi" -> "Tỷ Lệ Thắng"
        "tr" -> "Kazanma Oranı"
        "hi" -> "जीत दर"
        "ms" -> "Kadar Menang"
        "bn" -> "জয় হার"
        "ru" -> "Процент побед"
        else -> "Win Rate"
    }

    fun getTotalPnL(lang: String) = when(lang) {
        "id" -> "Total P&L"
        "en" -> "Total P&L"
        "es" -> "P&L Total"
        "vi" -> "Tổng L/R"
        "tr" -> "Toplam K/Z"
        "hi" -> "कुल लाभ/हानि"
        "ms" -> "Jumlah K&R"
        "bn" -> "মোট লাভ/ক্ষতি"
        "ru" -> "Общая П/У"
        else -> "Total P&L"
    }

    fun getTrades(lang: String) = when(lang) {
        "id" -> "trades"
        "en" -> "trades"
        "es" -> "operaciones"
        "vi" -> "giao dịch"
        "tr" -> "işlem"
        "hi" -> "ट्रेड"
        "ms" -> "dagangan"
        "bn" -> "ট্রেড"
        "ru" -> "сделок"
        else -> "trades"
    }

    fun getNoTradesThisWeek(lang: String) = when(lang) {
        "id" -> "Tidak ada trading minggu ini"
        "en" -> "No trades this week"
        "es" -> "No hay operaciones esta semana"
        "vi" -> "Không có giao dịch tuần này"
        "tr" -> "Bu hafta işlem yok"
        "hi" -> "इस सप्ताह कोई ट्रेड नहीं"
        "ms" -> "Tiada dagangan minggu ini"
        "bn" -> "এই সপ্তাহে কোনো ট্রেড নেই"
        "ru" -> "На этой неделе сделок нет"
        else -> "Tidak ada trading minggu ini"
    }

    fun getTotal(lang: String) = when(lang) {
        "id" -> "Total"
        "en" -> "Total"
        "es" -> "Total"
        "vi" -> "Tổng"
        "tr" -> "Toplam"
        "hi" -> "कुल"
        "ms" -> "Jumlah"
        "bn" -> "মোট"
        "ru" -> "Всего"
        else -> "Total"
    }

    fun getAllPnL(lang: String) = when(lang) {
        "id" -> "Semua P&L"
        "en" -> "All P&L"
        "es" -> "Todo P&L"
        "vi" -> "Tất Cả L/R"
        "tr" -> "Tüm K/Z"
        "hi" -> "सभी लाभ/हानि"
        "ms" -> "Semua K&R"
        "bn" -> "সব লাভ/ক্ষতি"
        "ru" -> "Все П/У"
        else -> "Semua P&L"
    }

    fun getAmount(lang: String) = when(lang) {
        "id" -> "Jumlah"
        "en" -> "Amount"
        "es" -> "Cantidad"
        "vi" -> "Số Tiền"
        "tr" -> "Miktar"
        "hi" -> "राशि"
        "ms" -> "Jumlah"
        "bn" -> "পরিমাণ"
        "ru" -> "Сумма"
        else -> "Jumlah"
    }

    fun getDirection(lang: String) = when(lang) {
        "id" -> "Arah"
        "en" -> "Direction"
        "es" -> "Dirección"
        "vi" -> "Hướng"
        "tr" -> "Yön"
        "hi" -> "दिशा"
        "ms" -> "Arah"
        "bn" -> "দিক"
        "ru" -> "Направление"
        else -> "Arah"
    }

    fun getOpen(lang: String) = when(lang) {
        "id" -> "Buka"
        "en" -> "Open"
        "es" -> "Apertura"
        "vi" -> "Mở"
        "tr" -> "Açılış"
        "hi" -> "खुला"
        "ms" -> "Buka"
        "bn" -> "খোলা"
        "ru" -> "Открыть"
        else -> "Buka"
    }

    fun getClose(lang: String) = when(lang) {
        "id" -> "Tutup"
        "en" -> "Close"
        "es" -> "Cierre"
        "vi" -> "Đóng"
        "tr" -> "Kapanış"
        "hi" -> "बंद"
        "ms" -> "Tutup"
        "bn" -> "বন্ধ"
        "ru" -> "Закрыть"
        else -> "Tutup"
    }

    fun getPayout(lang: String) = when(lang) {
        "id" -> "Payout"
        "en" -> "Payout"
        "es" -> "Pago"
        "vi" -> "Thanh Toán"
        "tr" -> "Ödeme"
        "hi" -> "भुगतान"
        "ms" -> "Bayaran"
        "bn" -> "প্রদান"
        "ru" -> "Выплата"
        else -> "Payout"
    }

    fun getLoadingTradingHistory(lang: String) = when(lang) {
        "id" -> "Memuat riwayat trading..."
        "en" -> "Loading trading history..."
        "es" -> "Cargando historial de trading..."
        "vi" -> "Đang tải lịch sử giao dịch..."
        "tr" -> "İşlem geçmişi yükleniyor..."
        "hi" -> "ट्रेडिंग इतिहास लोड हो रहा है..."
        "ms" -> "Memuatkan sejarah dagangan..."
        "bn" -> "ট্রেডিং ইতিহাস লোড হচ্ছে..."
        "ru" -> "Загрузка истории торговли..."
        else -> "Memuat riwayat trading..."
    }

    fun getSomethingWentWrong(lang: String) = when(lang) {
        "id" -> "Terjadi kesalahan"
        "en" -> "Something went wrong"
        "es" -> "Algo salió mal"
        "vi" -> "Đã xảy ra lỗi"
        "tr" -> "Bir şeyler yanlış gitti"
        "hi" -> "कुछ गलत हो गया"
        "ms" -> "Sesuatu tidak kena"
        "bn" -> "কিছু ভুল হয়েছে"
        "ru" -> "Что-то пошло не так"
        else -> "Terjadi kesalahan"
    }

    fun getDismiss(lang: String) = when(lang) {
        "id" -> "Tutup"
        "en" -> "Dismiss"
        "es" -> "Cerrar"
        "vi" -> "Đóng"
        "tr" -> "Kapat"
        "hi" -> "बंद करें"
        "ms" -> "Tutup"
        "bn" -> "বন্ধ করুন"
        "ru" -> "Закрыть"
        else -> "Tutup"
    }

    fun getRetry(lang: String) = when(lang) {
        "id" -> "Coba Lagi"
        "en" -> "Retry"
        "es" -> "Reintentar"
        "vi" -> "Thử Lại"
        "tr" -> "Tekrar Dene"
        "hi" -> "पुनः प्रयास करें"
        "ms" -> "Cuba Lagi"
        "bn" -> "আবার চেষ্টা করুন"
        "ru" -> "Повторить"
        else -> "Coba Lagi"
    }

    fun getNoTradingHistory(lang: String) = when(lang) {
        "id" -> "Tidak ada riwayat trading"
        "en" -> "No trading history"
        "es" -> "Sin historial de trading"
        "vi" -> "Không có lịch sử giao dịch"
        "tr" -> "İşlem geçmişi yok"
        "hi" -> "कोई ट्रेडिंग इतिहास नहीं"
        "ms" -> "Tiada sejarah dagangan"
        "bn" -> "কোনো ট্রেডিং ইতিহাস নেই"
        "ru" -> "История торговли отсутствует"
        else -> "Tidak ada riwayat trading"
    }

    fun getStartTradingToSeeHistory(lang: String) = when(lang) {
        "id" -> "Mulai trading untuk melihat riwayat di sini"
        "en" -> "Start trading to see your history here"
        "es" -> "Comienza a operar para ver tu historial aquí"
        "vi" -> "Bắt đầu giao dịch để xem lịch sử tại đây"
        "tr" -> "Geçmişinizi görmek için işlem yapmaya başlayın"
        "hi" -> "अपना इतिहास देखने के लिए ट्रेडिंग शुरू करें"
        "ms" -> "Mula berdagang untuk melihat sejarah anda di sini"
        "bn" -> "এখানে আপনার ইতিহাস দেখতে ট্রেডিং শুরু করুন"
        "ru" -> "Начните торговать, чтобы увидеть историю здесь"
        else -> "Mulai trading untuk melihat riwayat di sini"
    }

    fun getNoTradesFound(lang: String) = when(lang) {
        "id" -> "Tidak ditemukan trading dalam 7 hari terakhir"
        "en" -> "No trades found in the last 7 days"
        "es" -> "No se encontraron operaciones en los últimos 7 días"
        "vi" -> "Không tìm thấy giao dịch trong 7 ngày qua"
        "tr" -> "Son 7 günde işlem bulunamadı"
        "hi" -> "पिछले 7 दिनों में कोई ट्रेड नहीं मिला"
        "ms" -> "Tiada dagangan dijumpai dalam 7 hari terakhir"
        "bn" -> "গত ৭ দিনে কোনো ট্রেড পাওয়া যায়নি"
        "ru" -> "За последние 7 дней сделок не найдено"
        else -> "Tidak ditemukan trading dalam 7 hari terakhir"
    }

    fun getTryChangingFilter(lang: String) = when(lang) {
        "id" -> "Coba ubah filter untuk melihat lebih banyak trading"
        "en" -> "Try changing the filter to see more trades"
        "es" -> "Intenta cambiar el filtro para ver más operaciones"
        "vi" -> "Thử thay đổi bộ lọc để xem thêm giao dịch"
        "tr" -> "Daha fazla işlem görmek için filtreyi değiştirmeyi deneyin"
        "hi" -> "अधिक ट्रेड देखने के लिए फ़िल्टर बदलने का प्रयास करें"
        "ms" -> "Cuba tukar penapis untuk melihat lebih banyak dagangan"
        "bn" -> "আরও ট্রেড দেখতে ফিল্টার পরিবর্তন করার চেষ্টা করুন"
        "ru" -> "Попробуйте изменить фильтр, чтобы увидеть больше сделок"
        else -> "Coba ubah filter untuk melihat lebih banyak trading"
    }

    fun getFilterTrades(lang: String) = when(lang) {
        "id" -> "Filter Trading"
        "en" -> "Filter Trades"
        "es" -> "Filtrar Operaciones"
        "vi" -> "Lọc Giao Dịch"
        "tr" -> "İşlemleri Filtrele"
        "hi" -> "ट्रेड फ़िल्टर करें"
        "ms" -> "Tapis Dagangan"
        "bn" -> "ট্রেড ফিল্টার করুন"
        "ru" -> "Фильтровать сделки"
        else -> "Filter Trading"
    }

    fun getAllTrades(lang: String) = when(lang) {
        "id" -> "Semua Trading"
        "en" -> "All Trades"
        "es" -> "Todas las Operaciones"
        "vi" -> "Tất Cả Giao Dịch"
        "tr" -> "Tüm İşlemler"
        "hi" -> "सभी ट्रेड"
        "ms" -> "Semua Dagangan"
        "bn" -> "সব ট্রেড"
        "ru" -> "Все сделки"
        else -> "Semua Trading"
    }

    fun getWonTrades(lang: String) = when(lang) {
        "id" -> "Trading Menang"
        "en" -> "Won Trades"
        "es" -> "Operaciones Ganadoras"
        "vi" -> "Giao Dịch Thắng"
        "tr" -> "Kazanılan İşlemler"
        "hi" -> "जीते गए ट्रेड"
        "ms" -> "Dagangan Menang"
        "bn" -> "জিতে যাওয়া ট্রেড"
        "ru" -> "Выигранные сделки"
        else -> "Trading Menang"
    }

    fun getLostTrades(lang: String) = when(lang) {
        "id" -> "Trading Kalah"
        "en" -> "Lost Trades"
        "es" -> "Operaciones Perdidas"
        "vi" -> "Giao Dịch Thua"
        "tr" -> "Kaybedilen İşlemler"
        "hi" -> "हारे गए ट्रेड"
        "ms" -> "Dagangan Kalah"
        "bn" -> "হারানো ট্রেড"
        "ru" -> "Проигранные сделки"
        else -> "Trading Kalah"
    }

    fun getOpenTrades(lang: String) = when(lang) {
        "id" -> "Trading Terbuka"
        "en" -> "Open Trades"
        "es" -> "Operaciones Abiertas"
        "vi" -> "Giao Dịch Mở"
        "tr" -> "Açık İşlemler"
        "hi" -> "खुले ट्रेड"
        "ms" -> "Dagangan Terbuka"
        "bn" -> "খোলা ট্রেড"
        "ru" -> "Открытые сделки"
        else -> "Trading Terbuka"
    }

    fun getNoWonTrades(lang: String) = when(lang) {
        "id" -> "Tidak ada trading menang"
        "en" -> "No won trades"
        "es" -> "Sin operaciones ganadoras"
        "vi" -> "Không có giao dịch thắng"
        "tr" -> "Kazanılan işlem yok"
        "hi" -> "कोई जीता हुआ ट्रेड नहीं"
        "ms" -> "Tiada dagangan menang"
        "bn" -> "কোনো জয়ী ট্রেড নেই"
        "ru" -> "Нет выигранных сделок"
        else -> "Tidak ada trading menang"
    }

    fun getNoLostTrades(lang: String) = when(lang) {
        "id" -> "Tidak ada trading kalah"
        "en" -> "No lost trades"
        "es" -> "Sin operaciones perdidas"
        "vi" -> "Không có giao dịch thua"
        "tr" -> "Kaybedilen işlem yok"
        "hi" -> "कोई हारा हुआ ट्रेड नहीं"
        "ms" -> "Tiada dagangan kalah"
        "bn" -> "কোনো হারানো ট্রেড নেই"
        "ru" -> "Нет проигранных сделок"
        else -> "Tidak ada trading kalah"
    }

    fun getNoOpenedTrades(lang: String) = when(lang) {
        "id" -> "Tidak ada trading terbuka"
        "en" -> "No opened trades"
        "es" -> "Sin operaciones abiertas"
        "vi" -> "Không có giao dịch mở"
        "tr" -> "Açık işlem yok"
        "hi" -> "कोई खुला ट्रेड नहीं"
        "ms" -> "Tiada dagangan terbuka"
        "bn" -> "কোনো খোলা ট্রেড নেই"
        "ru" -> "Нет открытых сделок"
        else -> "Tidak ada trading terbuka"
    }

    fun getTodayProfit(lang: String) = when(lang) {
        "id" -> "PROFIT HARI INI"
        "en" -> "TODAY PROFIT"
        "es" -> "GANANCIA DE HOY"
        "vi" -> "LỢI NHUẬN HÔM NAY"
        "tr" -> "BUGÜNKÜ KAR"
        "hi" -> "आज का लाभ"
        "ms" -> "KEUNTUNGAN HARI INI"
        "bn" -> "আজকের লাভ"
        "ru" -> "ПРИБЫЛЬ СЕГОДНЯ"
        else -> "PROFIT HARI INI"
    }

    fun getRefreshData(lang: String) = when(lang) {
        "id" -> "Memperbarui data..."
        "en" -> "Refreshing data..."
        "es" -> "Actualizando datos..."
        "vi" -> "Đang làm mới dữ liệu..."
        "tr" -> "Veriler yenileniyor..."
        "hi" -> "डेटा रीफ़्रेश हो रहा है..."
        "ms" -> "Menyegarkan data..."
        "bn" -> "ডেটা রিফ্রেশ হচ্ছে..."
        "ru" -> "Обновление данных..."
        else -> "Memperbarui data..."
    }

    fun getCalculating(lang: String) = when(lang) {
        "id" -> "Menghitung..."
        "en" -> "Calculating..."
        "es" -> "Calculando..."
        "vi" -> "Đang tính toán..."
        "tr" -> "Hesaplanıyor..."
        "hi" -> "गणना हो रही है..."
        "ms" -> "Mengira..."
        "bn" -> "গণনা করা হচ্ছে..."
        "ru" -> "Вычисление..."
        else -> "Menghitung..."
    }

    fun getTapToRefresh(lang: String) = when(lang) {
        "id" -> "Ketuk untuk memperbarui"
        "en" -> "Tap to refresh"
        "es" -> "Toca para actualizar"
        "vi" -> "Chạm để làm mới"
        "tr" -> "Yenilemek için dokunun"
        "hi" -> "रीफ़्रेश करने के लिए टैप करें"
        "ms" -> "Ketik untuk menyegarkan"
        "bn" -> "রিফ্রেশ করতে ট্যাপ করুন"
        "ru" -> "Нажмите для обновления"
        else -> "Ketuk untuk memperbarui"
    }

    fun getLongPressToRecalculate(lang: String) = when(lang) {
        "id" -> "Tekan lama untuk kalkulasi ulang"
        "en" -> "Long press to recalculate"
        "es" -> "Mantén presionado para recalcular"
        "vi" -> "Nhấn giữ để tính lại"
        "tr" -> "Yeniden hesaplamak için basılı tutun"
        "hi" -> "पुनर्गणना के लिए लंबे समय तक दबाएं"
        "ms" -> "Tekan lama untuk mengira semula"
        "bn" -> "পুনর্গণনার জন্য দীর্ঘ চাপুন"
        "ru" -> "Удерживайте для пересчета"
        else -> "Tekan lama untuk kalkulasi ulang"
    }

    // Bot Control Strings
    fun getBotControl(lang: String) = when(lang) {
        "id" -> "Kontrol Bot"
        "en" -> "Bot Control"
        "es" -> "Control del Bot"
        "vi" -> "Điều Khiển Bot"
        "tr" -> "Bot Kontrolü"
        "hi" -> "बॉट नियंत्रण"
        "ms" -> "Kawalan Bot"
        "bn" -> "বট নিয়ন্ত্রণ"
        "ru" -> "Управление ботом"
        else -> "Kontrol Bot"
    }

    fun getBotRunning(lang: String) = when(lang) {
        "id" -> "Berjalan"
        "en" -> "Running"
        "es" -> "Ejecutando"
        "vi" -> "Đang Chạy"
        "tr" -> "Çalışıyor"
        "hi" -> "चल रहा है"
        "ms" -> "Berjalan"
        "bn" -> "চলছে"
        "ru" -> "Работает"
        else -> "Berjalan"
    }

    fun getBotPaused(lang: String) = when(lang) {
        "id" -> "Dijeda"
        "en" -> "Paused"
        "es" -> "Pausado"
        "vi" -> "Tạm Dừng"
        "tr" -> "Duraklatıldı"
        "hi" -> "रोका गया"
        "ms" -> "Dijeda"
        "bn" -> "বিরতি"
        "ru" -> "Приостановлен"
        else -> "Dijeda"
    }

    fun getBotStopped(lang: String) = when(lang) {
        "id" -> "Berhenti"
        "en" -> "Stopped"
        "es" -> "Detenido"
        "vi" -> "Đã Dừng"
        "tr" -> "Durduruldu"
        "hi" -> "बंद"
        "ms" -> "Berhenti"
        "bn" -> "থামানো"
        "ru" -> "Остановлен"
        else -> "Berhenti"
    }

    fun getBotPause(lang: String) = when(lang) {
        "id" -> "Jeda"
        "en" -> "Pause"
        "es" -> "Pausar"
        "vi" -> "Tạm Dừng"
        "tr" -> "Duraklat"
        "hi" -> "रोकें"
        "ms" -> "Jeda"
        "bn" -> "বিরতি দিন"
        "ru" -> "Пауза"
        else -> "Jeda"
    }

    fun getBotResume(lang: String) = when(lang) {
        "id" -> "Lanjutkan"
        "en" -> "Resume"
        "es" -> "Reanudar"
        "vi" -> "Tiếp Tục"
        "tr" -> "Devam Et"
        "hi" -> "फिर से शुरू करें"
        "ms" -> "Sambung"
        "bn" -> "পুনরায় শুরু করুন"
        "ru" -> "Возобновить"
        else -> "Lanjutkan"
    }

    fun getBotStop(lang: String) = when(lang) {
        "id" -> "Hentikan"
        "en" -> "Stop"
        "es" -> "Detener"
        "vi" -> "Dừng"
        "tr" -> "Durdur"
        "hi" -> "बंद करें"
        "ms" -> "Hentikan"
        "bn" -> "থামান"
        "ru" -> "Остановить"
        else -> "Hentikan"
    }

    fun getBotAnalyzingMarket(lang: String) = when(lang) {
        "id" -> "Menganalisis data pasar"
        "en" -> "Analyzing market data"
        "es" -> "Analizando datos del mercado"
        "vi" -> "Đang phân tích dữ liệu thị trường"
        "tr" -> "Piyasa verileri analiz ediliyor"
        "hi" -> "बाजार डेटा का विश्लेषण"
        "ms" -> "Menganalisis data pasaran"
        "bn" -> "বাজারের ডেটা বিশ্লেষণ করা হচ্ছে"
        "ru" -> "Анализ рыночных данных"
        else -> "Menganalisis data pasar"
    }

    fun getBotMonitoring(lang: String) = when(lang) {
        "id" -> "Memantau sinyal trading untuk peluang trading"
        "en" -> "Monitoring trading signals for opportunities"
        "es" -> "Monitoreando señales de trading para oportunidades"
        "vi" -> "Theo dõi tín hiệu giao dịch để tìm cơ hội"
        "tr" -> "Fırsatlar için ticaret sinyalleri izleniyor"
        "hi" -> "अवसरों के लिए ट्रेडिंग संकेतों की निगरानी"
        "ms" -> "Memantau isyarat dagangan untuk peluang"
        "bn" -> "সুযোগের জন্য ট্রেডিং সিগন্যাল পর্যবেক্ষণ করা হচ্ছে"
        "ru" -> "Мониторинг торговых сигналов для поиска возможностей"
        else -> "Memantau sinyal trading untuk peluang trading"
    }

    fun getBotActive(lang: String) = when(lang) {
        "id" -> "Aktif"
        "en" -> "Active"
        "es" -> "Activo"
        "vi" -> "Hoạt Động"
        "tr" -> "Aktif"
        "hi" -> "सक्रिय"
        "ms" -> "Aktif"
        "bn" -> "সক্রিয়"
        "ru" -> "Активен"
        else -> "Aktif"
    }

    fun getBotInactive(lang: String) = when(lang) {
        "id" -> "Tidak Aktif"
        "en" -> "Inactive"
        "es" -> "Inactivo"
        "vi" -> "Không Hoạt Động"
        "tr" -> "Pasif"
        "hi" -> "निष्क्रिय"
        "ms" -> "Tidak Aktif"
        "bn" -> "নিষ্ক্রিয়"
        "ru" -> "Неактивен"
        else -> "Tidak Aktif"
    }

    // Stop Loss & Profit Strings
    fun getStopLossProfit(lang: String) = when(lang) {
        "id" -> "Stop Loss & Profit"
        "en" -> "Stop Loss & Profit"
        "es" -> "Stop Loss y Ganancia"
        "vi" -> "Stop Loss & Lợi Nhuận"
        "tr" -> "Zarar Durdur & Kar"
        "hi" -> "स्टॉप लॉस और प्रॉफिट"
        "ms" -> "Stop Loss & Keuntungan"
        "bn" -> "স্টপ লস এবং লাভ"
        "ru" -> "Стоп-лосс и прибыль"
        else -> "Stop Loss & Profit"
    }

    fun getResetSession(lang: String) = when(lang) {
        "id" -> "Reset Sesi"
        "en" -> "Reset Session"
        "es" -> "Reiniciar Sesión"
        "vi" -> "Đặt Lại Phiên"
        "tr" -> "Oturumu Sıfırla"
        "hi" -> "सत्र रीसेट करें"
        "ms" -> "Set Semula Sesi"
        "bn" -> "সেশন রিসেট করুন"
        "ru" -> "Сбросить сессию"
        else -> "Reset Sesi"
    }

    fun getNetProfit(lang: String) = when(lang) {
        "id" -> "Profit Bersih"
        "en" -> "Net Profit"
        "es" -> "Ganancia Neta"
        "vi" -> "Lợi Nhuận Ròng"
        "tr" -> "Net Kar"
        "hi" -> "शुद्ध लाभ"
        "ms" -> "Keuntungan Bersih"
        "bn" -> "নেট লাভ"
        "ru" -> "Чистая прибыль"
        else -> "Profit Bersih"
    }

    fun getTotalLoss(lang: String) = when(lang) {
        "id" -> "Total Rugi"
        "en" -> "Total Loss"
        "es" -> "Pérdida Total"
        "vi" -> "Tổng Lỗ"
        "tr" -> "Toplam Zarar"
        "hi" -> "कुल हानि"
        "ms" -> "Jumlah Kerugian"
        "bn" -> "মোট ক্ষতি"
        "ru" -> "Общий убыток"
        else -> "Total Rugi"
    }

    fun getStopLoss(lang: String) = when(lang) {
        "id" -> "Batas Rugi"
        "en" -> "Stop Loss"
        "es" -> "Límite de Pérdida"
        "vi" -> "Dừng Lỗ"
        "tr" -> "Zarar Durdur"
        "hi" -> "स्टॉप लॉस"
        "ms" -> "Had Kerugian"
        "bn" -> "স্টপ লস"
        "ru" -> "Стоп-лосс"
        else -> "Batas Rugi"
    }

    fun getTargetProfit(lang: String) = when(lang) {
        "id" -> "Target Profit"
        "en" -> "Target Profit"
        "es" -> "Objetivo de Ganancia"
        "vi" -> "Mục Tiêu Lợi Nhuận"
        "tr" -> "Hedef Kar"
        "hi" -> "लक्ष्य लाभ"
        "ms" -> "Sasaran Keuntungan"
        "bn" -> "লক্ষ্য লাভ"
        "ru" -> "Целевая прибыль"
        else -> "Target Profit"
    }

    fun getStopLossSettings(lang: String) = when(lang) {
        "id" -> "Pengaturan Stop Loss"
        "en" -> "Stop Loss Settings"
        "es" -> "Configuración de Stop Loss"
        "vi" -> "Cài Đặt Stop Loss"
        "tr" -> "Zarar Durdur Ayarları"
        "hi" -> "स्टॉप लॉस सेटिंग्स"
        "ms" -> "Tetapan Stop Loss"
        "bn" -> "স্টপ লস সেটিংস"
        "ru" -> "Настройки стоп-лосса"
        else -> "Pengaturan Stop Loss"
    }

    fun getTargetProfitSettings(lang: String) = when(lang) {
        "id" -> "Pengaturan Target Profit"
        "en" -> "Target Profit Settings"
        "es" -> "Configuración de Objetivo de Ganancia"
        "vi" -> "Cài Đặt Mục Tiêu Lợi Nhuận"
        "tr" -> "Hedef Kar Ayarları"
        "hi" -> "लक्ष्य लाभ सेटिंग्स"
        "ms" -> "Tetapan Sasaran Keuntungan"
        "bn" -> "লক্ষ্য লাভ সেটিংস"
        "ru" -> "Настройки целевой прибыли"
        else -> "Pengaturan Target Profit"
    }

    fun getMaxLossAmount(lang: String) = when(lang) {
        "id" -> "Jumlah Kerugian Maksimal"
        "en" -> "Max Loss Amount"
        "es" -> "Monto Máximo de Pérdida"
        "vi" -> "Số Tiền Lỗ Tối Đa"
        "tr" -> "Maksimum Zarar Tutarı"
        "hi" -> "अधिकतम हानि राशि"
        "ms" -> "Jumlah Kerugian Maksimum"
        "bn" -> "সর্বাধিক ক্ষতির পরিমাণ"
        "ru" -> "Максимальная сумма убытка"
        else -> "Jumlah Kerugian Maksimal"
    }

    fun getTargetProfitAmount(lang: String) = when(lang) {
        "id" -> "Jumlah Target Profit"
        "en" -> "Target Profit Amount"
        "es" -> "Monto de Ganancia Objetivo"
        "vi" -> "Số Tiền Lợi Nhuận Mục Tiêu"
        "tr" -> "Hedef Kar Tutarı"
        "hi" -> "लक्ष्य लाभ राशि"
        "ms" -> "Jumlah Sasaran Keuntungan"
        "bn" -> "লক্ষ্য লাভের পরিমাণ"
        "ru" -> "Целевая сумма прибыли"
        else -> "Jumlah Target Profit"
    }

    fun getPlaceholderAmount(lang: String) = when(lang) {
        "id" -> "cth: 500000, 1M, 5M"
        "en" -> "e.g., 500000, 1M, 5M"
        "es" -> "ej: 500000, 1M, 5M"
        "vi" -> "vd: 500000, 1M, 5M"
        "tr" -> "örn: 500000, 1M, 5M"
        "hi" -> "उदा: 500000, 1M, 5M"
        "ms" -> "cth: 500000, 1M, 5M"
        "bn" -> "যেমন: 500000, 1M, 5M"
        "ru" -> "напр: 500000, 1M, 5M"
        else -> "cth: 500000, 1M, 5M"
    }

    fun getCurrentMaxLoss(lang: String) = when(lang) {
        "id" -> "Batas Kerugian Saat Ini:"
        "en" -> "Current Max Loss:"
        "es" -> "Pérdida Máxima Actual:"
        "vi" -> "Lỗ Tối Đa Hiện Tại:"
        "tr" -> "Mevcut Maksimum Zarar:"
        "hi" -> "वर्तमान अधिकतम हानि:"
        "ms" -> "Had Kerugian Semasa:"
        "bn" -> "বর্তমান সর্বাধিক ক্ষতি:"
        "ru" -> "Текущий макс. убыток:"
        else -> "Batas Kerugian Saat Ini:"
    }

    fun getCurrentTargetProfit(lang: String) = when(lang) {
        "id" -> "Target Profit Saat Ini:"
        "en" -> "Current Target Profit:"
        "es" -> "Objetivo de Ganancia Actual:"
        "vi" -> "Mục Tiêu Lợi Nhuận Hiện Tại:"
        "tr" -> "Mevcut Hedef Kar:"
        "hi" -> "वर्तमान लक्ष्य लाभ:"
        "ms" -> "Sasaran Keuntungan Semasa:"
        "bn" -> "বর্তমান লক্ষ্য লাভ:"
        "ru" -> "Текущая целевая прибыль:"
        else -> "Target Profit Saat Ini:"
    }

    fun getAmountFormatHint(lang: String) = when(lang) {
        "id" -> "Format: 1M = 1 Juta, 500K = 500 Ribu • Tekan Enter untuk konfirmasi"
        "en" -> "Format: 1M = 1 Million, 500K = 500 Thousand • Press Enter to confirm"
        "es" -> "Formato: 1M = 1 Millón, 500K = 500 Mil • Presiona Enter para confirmar"
        "vi" -> "Định dạng: 1M = 1 Triệu, 500K = 500 Nghìn • Nhấn Enter để xác nhận"
        "tr" -> "Format: 1M = 1 Milyon, 500K = 500 Bin • Onaylamak için Enter'a basın"
        "hi" -> "प्रारूप: 1M = 1 मिलियन, 500K = 500 हजार • पुष्टि के लिए Enter दबाएं"
        "ms" -> "Format: 1M = 1 Juta, 500K = 500 Ribu • Tekan Enter untuk pengesahan"
        "bn" -> "ফরম্যাট: 1M = 1 মিলিয়ন, 500K = 500 হাজার • নিশ্চিত করতে Enter চাপুন"
        "ru" -> "Формат: 1M = 1 млн, 500K = 500 тыс • Нажмите Enter для подтверждения"
        else -> "Format: 1M = 1 Juta, 500K = 500 Ribu • Tekan Enter untuk konfirmasi"
    }

    // Trading Settings Strings
    fun getTradingSettings(lang: String) = when(lang) {
        "id" -> "Pengaturan Trading"
        "en" -> "Trading Settings"
        "es" -> "Configuración de Trading"
        "vi" -> "Cài Đặt Giao Dịch"
        "tr" -> "İşlem Ayarları"
        "hi" -> "ट्रेडिंग सेटिंग्स"
        "ms" -> "Tetapan Dagangan"
        "bn" -> "ট্রেডিং সেটিংস"
        "ru" -> "Настройки торговли"
        else -> "Pengaturan Trading"
    }

    fun getAccountConfiguration(lang: String) = when(lang) {
        "id" -> "Konfigurasi Akun"
        "en" -> "Account Configuration"
        "es" -> "Configuración de Cuenta"
        "vi" -> "Cấu Hình Tài Khoản"
        "tr" -> "Hesap Yapılandırması"
        "hi" -> "खाता विन्यास"
        "ms" -> "Konfigurasi Akaun"
        "bn" -> "অ্যাকাউন্ট কনফিগারেশন"
        "ru" -> "Конфигурация аккаунта"
        else -> "Konfigurasi Akun"
    }

    fun getDemoAccountFull(lang: String) = when(lang) {
        "id" -> "Akun Demo"
        "en" -> "Demo Account"
        "es" -> "Cuenta Demo"
        "vi" -> "Tài Khoản Demo"
        "tr" -> "Demo Hesap"
        "hi" -> "डेमो खाता"
        "ms" -> "Akaun Demo"
        "bn" -> "ডেমো অ্যাকাউন্ট"
        "ru" -> "Демо-счет"
        else -> "Akun Demo"
    }

    fun getRealAccountFull(lang: String) = when(lang) {
        "id" -> "Akun Real"
        "en" -> "Real Account"
        "es" -> "Cuenta Real"
        "vi" -> "Tài Khoản Thật"
        "tr" -> "Gerçek Hesap"
        "hi" -> "असली खाता"
        "ms" -> "Akaun Sebenar"
        "bn" -> "আসল অ্যাকাউন্ট"
        "ru" -> "Реальный счет"
        else -> "Akun Real"
    }

    fun getAutoDuration(lang: String) = when(lang) {
        "id" -> "Durasi Otomatis"
        "en" -> "Auto Duration"
        "es" -> "Duración Automática"
        "vi" -> "Thời Lượng Tự Động"
        "tr" -> "Otomatik Süre"
        "hi" -> "ऑटो अवधि"
        "ms" -> "Tempoh Auto"
        "bn" -> "স্বয়ংক্রিয় সময়কাল"
        "ru" -> "Авто продолжительность"
        else -> "Durasi Otomatis"
    }
    fun getOneMinute(lang: String) = when(lang) {
        "id" -> "1 Menit"
        "en" -> "1 Minute"
        "es" -> "1 Minuto"
        "vi" -> "1 Phút"
        "tr" -> "1 Dakika"
        "hi" -> "1 मिनट"
        "ms" -> "1 Minit"
        "bn" -> "১ মিনিট"
        "ru" -> "1 минута"
        else -> "1 Menit"
    }
    fun getCurrencySelection(lang: String) = when(lang) {
        "id" -> "Pilihan Mata Uang"
        "en" -> "Currency Selection"
        "es" -> "Selección de Moneda"
        "vi" -> "Lựa Chọn Tiền Tệ"
        "tr" -> "Para Birimi Seçimi"
        "hi" -> "मुद्रा चयन"
        "ms" -> "Pemilihan Mata Wang"
        "bn" -> "মুদ্রা নির্বাচন"
        "ru" -> "Выбор валюты"
        else -> "Pilihan Mata Uang"
    }
    fun getTradeAmount(lang: String) = when(lang) {
        "id" -> "Jumlah Trading"
        "en" -> "Trade Amount"
        "es" -> "Monto de Operación"
        "vi" -> "Số Tiền Giao Dịch"
        "tr" -> "İşlem Tutarı"
        "hi" -> "ट्रेड राशि"
        "ms" -> "Jumlah Dagangan"
        "bn" -> "ট্রেড পরিমাণ"
        "ru" -> "Сумма сделки"
        else -> "Jumlah Trading"
    }
    fun getMinimum(lang: String) = when(lang) {
        "id" -> "Min"
        "en" -> "Min"
        "es" -> "Mín"
        "vi" -> "Tối thiểu"
        "tr" -> "Min"
        "hi" -> "न्यूनतम"
        "ms" -> "Min"
        "bn" -> "সর্বনিম্ন"
        "ru" -> "Мин"
        else -> "Min"
    }
    fun getQuick(lang: String) = when(lang) {
        "id" -> "Cepat"
        "en" -> "Quick"
        "es" -> "Rápido"
        "vi" -> "Nhanh"
        "tr" -> "Hızlı"
        "hi" -> "त्वरित"
        "ms" -> "Pantas"
        "bn" -> "দ্রুত"
        "ru" -> "Быстро"
        else -> "Cepat"
    }
    fun getMartingaleStrategy(lang: String) = when(lang) {
        "id" -> "Strategi Martingale"
        "en" -> "Martingale Strategy"
        "es" -> "Estrategia Martingale"
        "vi" -> "Chiến Lược Martingale"
        "tr" -> "Martingale Stratejisi"
        "hi" -> "मार्टिंगेल रणनीति"
        "ms" -> "Strategi Martingale"
        "bn" -> "মার্টিনগেল কৌশল"
        "ru" -> "Стратегия Мартингейла"
        else -> "Strategi Martingale"
    }
    fun getMartingale(lang: String) = when(lang) {
        "id" -> "MARTINGALE"
        "en" -> "MARTINGALE"
        "es" -> "MARTINGALE"
        "vi" -> "MARTINGALE"
        "tr" -> "MARTINGALE"
        "hi" -> "मार्टिंगेल"
        "ms" -> "MARTINGALE"
        "bn" -> "মার্টিনগেল"
        "ru" -> "МАРТИНГЕЙЛ"
        else -> "MARTINGALE"
    }
    fun getMaxSteps(lang: String) = when(lang) {
        "id" -> "Kompensasi"
        "en" -> "Max Steps"
        "es" -> "Pasos Máximos"
        "vi" -> "Số Bước Tối Đa"
        "tr" -> "Maksimum Adım"
        "hi" -> "अधिकतम कदम"
        "ms" -> "Kompensasi"
        "bn" -> "সর্বাধিক পদক্ষেপ"
        "ru" -> "Макс. шаги"
        else -> "Kompensasi Maks"
    }
    fun getSettings(lang: String) = when(lang) {
        "id" -> "Pengaturan"
        "en" -> "Settings"
        "es" -> "Configuración"
        "vi" -> "Cài Đặt"
        "tr" -> "Ayarlar"
        "hi" -> "सेटिंग्स"
        "ms" -> "Tetapan"
        "bn" -> "সেটিংস"
        "ru" -> "Настройки"
        else -> "Pengaturan"
    }

    fun getAssets(lang: String) = when(lang) {
        "id" -> "Pilih Aset"
        "en" -> "Select Asset"
        "es" -> "Seleccionar Activo"
        "vi" -> "Chọn Tài Sản"
        "tr" -> "Varlık Seç"
        "hi" -> "संपत्ति चुनें"
        "ms" -> "Pilih Aset"
        "bn" -> "সম্পদ নির্বাচন করুন"
        "ru" -> "Выбрать актив"
        else -> "Pilih Aset"
    }

    fun getAddAsset(lang: String) = when(lang) {
        "id" -> "Tambah Aset"
        "en" -> "Add Asset"
        "es" -> "Agregar Activo"
        "vi" -> "Thêm Tài Sản"
        "tr" -> "Varlık Ekle"
        "hi" -> "संपत्ति जोड़ें"
        "ms" -> "Tambah Aset"
        "bn" -> "সম্পদ যোগ করুন"
        "ru" -> "Добавить актив"
        else -> "Tambah Aset"
    }

    fun getStatistics(lang: String) = when(lang) {
        "id" -> "Statistik"
        "en" -> "Statistics"
        "es" -> "Estadísticas"
        "vi" -> "Thống Kê"
        "tr" -> "İstatistikler"
        "hi" -> "सांख्यिकी"
        "ms" -> "Statistik"
        "bn" -> "পরিসংখ্যান"
        "ru" -> "Статистика"
        else -> "Statistik"
    }

    fun getWin(lang: String) = when(lang) {
        "id" -> "Menang"
        "en" -> "Win"
        "es" -> "Ganar"
        "vi" -> "Thắng"
        "tr" -> "Kazanç"
        "hi" -> "जीत"
        "ms" -> "Menang"
        "bn" -> "জয়"
        "ru" -> "Выигрыш"
        else -> "Menang"
    }

    fun getDraw(lang: String) = when(lang) {
        "id" -> "Seri"
        "en" -> "Draw"
        "es" -> "Empate"
        "vi" -> "Hòa"
        "tr" -> "Berabere"
        "hi" -> "ड्रॉ"
        "ms" -> "Seri"
        "bn" -> "ড্র"
        "ru" -> "Ничья"
        else -> "Seri"
    }

    fun getLose(lang: String) = when(lang) {
        "id" -> "Kalah"
        "en" -> "Lose"
        "es" -> "Perder"
        "vi" -> "Thua"
        "tr" -> "Kayıp"
        "hi" -> "हार"
        "ms" -> "Kalah"
        "bn" -> "পরাজয়"
        "ru" -> "Проигрыш"
        else -> "Kalah"
    }

    fun getShowClock(lang: String) = when(lang) {
        "id" -> "Tampilkan Jam"
        "en" -> "Show Clock"
        "es" -> "Mostrar Reloj"
        "vi" -> "Hiển Thị Đồng Hồ"
        "tr" -> "Saati Göster"
        "hi" -> "घड़ी दिखाएं"
        "ms" -> "Tunjukkan Jam"
        "bn" -> "ঘড়ি দেখান"
        "ru" -> "Показать часы"
        else -> "Tampilkan Jam"
    }

    fun getShowStats(lang: String) = when(lang) {
        "id" -> "Tampilkan Statistik"
        "en" -> "Show Statistics"
        "es" -> "Mostrar Estadísticas"
        "vi" -> "Hiển Thị Thống Kê"
        "tr" -> "İstatistikleri Göster"
        "hi" -> "सांख्यिकी दिखाएं"
        "ms" -> "Tunjukkan Statistik"
        "bn" -> "পরিসংখ্যান দেখান"
        "ru" -> "Показать статистику"
        else -> "Tampilkan Statistik"
    }

    fun getSelectAsset(lang: String) = when(lang) {
        "id" -> "Pilih Aset"
        "en" -> "Select Asset"
        "es" -> "Seleccionar Activo"
        "vi" -> "Chọn Tài Sản"
        "tr" -> "Varlık Seç"
        "hi" -> "संपत्ति चुनें"
        "ms" -> "Pilih Aset"
        "bn" -> "সম্পদ নির্বাচন করুন"
        "ru" -> "Выбрать актив"
        else -> "Pilih Aset"
    }

    fun getChooseFromAvailable(lang: String) = when(lang) {
        "id" -> "Pilih dari aset yang tersedia"
        "en" -> "Choose from available assets"
        "es" -> "Elegir de activos disponibles"
        "vi" -> "Chọn từ tài sản có sẵn"
        "tr" -> "Mevcut varlıklardan seçin"
        "hi" -> "उपलब्ध संपत्तियों में से चुनें"
        "ms" -> "Pilih dari aset yang tersedia"
        "bn" -> "উপলব্ধ সম্পদ থেকে নির্বাচন করুন"
        "ru" -> "Выберите из доступных активов"
        else -> "Pilih dari aset yang tersedia"
    }

    fun getRefresh(lang: String) = when(lang) {
        "id" -> "Muat Ulang"
        "en" -> "Refresh"
        "es" -> "Actualizar"
        "vi" -> "Làm Mới"
        "tr" -> "Yenile"
        "hi" -> "रीफ्रेश करें"
        "ms" -> "Muat Semula"
        "bn" -> "রিফ্রেশ করুন"
        "ru" -> "Обновить"
        else -> "Muat Ulang"
    }

    fun getLoadingAssets(lang: String) = when(lang) {
        "id" -> "Memuat Aset"
        "en" -> "Loading Assets"
        "es" -> "Cargando Activos"
        "vi" -> "Đang Tải Tài Sản"
        "tr" -> "Varlıklar Yükleniyor"
        "hi" -> "संपत्ति लोड हो रही है"
        "ms" -> "Memuatkan Aset"
        "bn" -> "সম্পদ লোড হচ্ছে"
        "ru" -> "Загрузка активов"
        else -> "Memuat Aset"
    }

    fun getPleaseWaitFetchingAssets(lang: String) = when(lang) {
        "id" -> "Mohon tunggu saat kami mengambil aset Anda"
        "en" -> "Please wait while we fetch your assets"
        "es" -> "Por favor espere mientras obtenemos sus activos"
        "vi" -> "Vui lòng đợi trong khi chúng tôi tải tài sản của bạn"
        "tr" -> "Varlıklarınızı alırken lütfen bekleyin"
        "hi" -> "कृपया प्रतीक्षा करें जबकि हम आपकी संपत्ति प्राप्त करते हैं"
        "ms" -> "Sila tunggu semasa kami mengambil aset anda"
        "bn" -> "অনুগ্রহ করে অপেক্ষা করুন যখন আমরা আপনার সম্পদ আনছি"
        "ru" -> "Пожалуйста, подождите, пока мы загружаем ваши активы"
        else -> "Mohon tunggu saat kami mengambil aset Anda"
    }

    fun getNoAssetsAvailable(lang: String) = when(lang) {
        "id" -> "Tidak Ada Aset Tersedia"
        "en" -> "No Assets Available"
        "es" -> "No Hay Activos Disponibles"
        "vi" -> "Không Có Tài Sản"
        "tr" -> "Kullanılabilir Varlık Yok"
        "hi" -> "कोई संपत्ति उपलब्ध नहीं"
        "ms" -> "Tiada Aset Tersedia"
        "bn" -> "কোনো সম্পদ উপলব্ধ নেই"
        "ru" -> "Нет доступных активов"
        else -> "Tidak Ada Aset Tersedia"
    }

    fun getNoAssetsToDisplay(lang: String) = when(lang) {
        "id" -> "Tidak ada aset untuk ditampilkan saat ini.\nCoba muat ulang untuk memuat daftar aset."
        "en" -> "There are no assets to display at the moment.\nTry refreshing to reload the asset list."
        "es" -> "No hay activos para mostrar en este momento.\nIntenta actualizar para recargar la lista de activos."
        "vi" -> "Hiện không có tài sản để hiển thị.\nThử làm mới để tải lại danh sách tài sản."
        "tr" -> "Şu anda gösterilecek varlık yok.\nVarlık listesini yeniden yüklemek için yenilemeyi deneyin."
        "hi" -> "इस समय प्रदर्शित करने के लिए कोई संपत्ति नहीं है।\nसंपत्ति सूची पुनः लोड करने के लिए रीफ्रेश करने का प्रयास करें।"
        "ms" -> "Tiada aset untuk dipaparkan pada masa ini.\nCuba muat semula untuk memuatkan senarai aset."
        "bn" -> "এই মুহূর্তে প্রদর্শনের জন্য কোনো সম্পদ নেই।\nসম্পদ তালিকা পুনরায় লোড করতে রিফ্রেশ করার চেষ্টা করুন।"
        "ru" -> "В настоящее время нет активов для отображения.\nПопробуйте обновить, чтобы перезагрузить список активов."
        else -> "Tidak ada aset untuk ditampilkan saat ini.\nCoba muat ulang untuk memuat daftar aset."
    }

    fun getRefreshAssets(lang: String) = when(lang) {
        "id" -> "Muat Ulang Aset"
        "en" -> "Refresh Assets"
        "es" -> "Actualizar Activos"
        "vi" -> "Làm Mới Tài Sản"
        "tr" -> "Varlıkları Yenile"
        "hi" -> "संपत्ति रीफ्रेश करें"
        "ms" -> "Muat Semula Aset"
        "bn" -> "সম্পদ রিফ্রেশ করুন"
        "ru" -> "Обновить активы"
        else -> "Muat Ulang Aset"
    }

    fun getProfit(lang: String) = when(lang) {
        "id" -> "profit"
        "en" -> "profit"
        "es" -> "beneficio"
        "vi" -> "lợi nhuận"
        "tr" -> "kâr"
        "hi" -> "लाभ"
        "ms" -> "keuntungan"
        "bn" -> "লাভ"
        "ru" -> "прибыль"
        else -> "profit"
    }

    fun getCurrentTime(lang: String) = when(lang) {
        "id" -> "Waktu Saat Ini"
        "en" -> "Current Time"
        "es" -> "Hora Actual"
        "vi" -> "Giờ Hiện Tại"
        "tr" -> "Şu Anki Saat"
        "hi" -> "वर्तमान समय"
        "ms" -> "Masa Sekarang"
        "bn" -> "বর্তমান সময়"
        "ru" -> "Текущее время"
        else -> "Waktu Saat Ini"
    }

    fun getServerTime(lang: String) = when(lang) {
        "id" -> "Waktu Server"
        "en" -> "Server Time"
        "es" -> "Hora del Servidor"
        "vi" -> "Giờ Máy Chủ"
        "tr" -> "Sunucu Saati"
        "hi" -> "सर्वर समय"
        "ms" -> "Masa Pelayan"
        "bn" -> "সার্ভার সময়"
        "ru" -> "Время сервера"
        else -> "Waktu Server"
    }

    fun getLocalTime(lang: String) = when(lang) {
        "id" -> "Waktu Lokal"
        "en" -> "Local Time"
        "es" -> "Hora Local"
        "vi" -> "Giờ Địa Phương"
        "tr" -> "Yerel Saat"
        "hi" -> "स्थानीय समय"
        "ms" -> "Masa Tempatan"
        "bn" -> "স্থানীয় সময়"
        "ru" -> "Местное время"
        else -> "Waktu Lokal"
    }

    // Profile Screen Strings
    fun getProfileInformation(lang: String) = when(lang) {
        "id" -> "Informasi Profil"
        "en" -> "Profile Information"
        "es" -> "Información del Perfil"
        "vi" -> "Thông Tin Hồ Sơ"
        "tr" -> "Profil Bilgileri"
        "hi" -> "प्रोफ़ाइल जानकारी"
        "ms" -> "Maklumat Profil"
        "bn" -> "প্রোফাইল তথ্য"
        "ru" -> "Информация о профиле"
        else -> "Informasi Profil"
    }

    fun getFullName(lang: String) = when(lang) {
        "id" -> "Nama Lengkap"
        "en" -> "Full Name"
        "es" -> "Nombre Completo"
        "vi" -> "Tên Đầy Đủ"
        "tr" -> "Tam İsim"
        "hi" -> "पूरा नाम"
        "ms" -> "Nama Penuh"
        "bn" -> "সম্পূর্ণ নাম"
        "ru" -> "Полное имя"
        else -> "Nama Lengkap"
    }

    fun getUserId(lang: String) = when(lang) {
        "id" -> "ID Pengguna"
        "en" -> "User ID"
        "es" -> "ID de Usuario"
        "vi" -> "ID Người Dùng"
        "tr" -> "Kullanıcı ID"
        "hi" -> "यूज़र आईडी"
        "ms" -> "ID Pengguna"
        "bn" -> "ব্যবহারকারী আইডি"
        "ru" -> "ID пользователя"
        else -> "ID Pengguna"
    }

    fun getEmailAddress(lang: String) = when(lang) {
        "id" -> "Alamat Email"
        "en" -> "Email Address"
        "es" -> "Dirección de Email"
        "vi" -> "Địa Chỉ Email"
        "tr" -> "E-posta Adresi"
        "hi" -> "ईमेल पता"
        "ms" -> "Alamat E-mel"
        "bn" -> "ইমেইল ঠিকানা"
        "ru" -> "Адрес электронной почты"
        else -> "Alamat Email"
    }

    fun getTimezone(lang: String) = when(lang) {
        "id" -> "Zona Waktu"
        "en" -> "Timezone"
        "es" -> "Zona Horaria"
        "vi" -> "Múi Giờ"
        "tr" -> "Zaman Dilimi"
        "hi" -> "समय क्षेत्र"
        "ms" -> "Zon Masa"
        "bn" -> "সময় অঞ্চল"
        "ru" -> "Часовой пояс"
        else -> "Zona Waktu"
    }

    fun getDeviceInformation(lang: String) = when(lang) {
        "id" -> "Informasi Perangkat"
        "en" -> "Device Information"
        "es" -> "Información del Dispositivo"
        "vi" -> "Thông Tin Thiết Bị"
        "tr" -> "Cihaz Bilgileri"
        "hi" -> "डिवाइस जानकारी"
        "ms" -> "Maklumat Peranti"
        "bn" -> "ডিভাইস তথ্য"
        "ru" -> "Информация об устройстве"
        else -> "Informasi Perangkat"
    }

    fun getDeviceId(lang: String) = when(lang) {
        "id" -> "ID Perangkat"
        "en" -> "Device ID"
        "es" -> "ID del Dispositivo"
        "vi" -> "ID Thiết Bị"
        "tr" -> "Cihaz ID"
        "hi" -> "डिवाइस आईडी"
        "ms" -> "ID Peranti"
        "bn" -> "ডিভাইস আইডি"
        "ru" -> "ID устройства"
        else -> "ID Perangkat"
    }

    fun getBrowser(lang: String) = when(lang) {
        "id" -> "Browser"
        "en" -> "Browser"
        "es" -> "Navegador"
        "vi" -> "Trình Duyệt"
        "tr" -> "Tarayıcı"
        "hi" -> "ब्राउज़र"
        "ms" -> "Pelayar"
        "bn" -> "ব্রাউজার"
        "ru" -> "Браузер"
        else -> "Browser"
    }

    fun getSecurityStatus(lang: String) = when(lang) {
        "id" -> "Status Keamanan"
        "en" -> "Security Status"
        "es" -> "Estado de Seguridad"
        "vi" -> "Trạng Thái Bảo Mật"
        "tr" -> "Güvenlik Durumu"
        "hi" -> "सुरक्षा स्थिति"
        "ms" -> "Status Keselamatan"
        "bn" -> "নিরাপত্তা অবস্থা"
        "ru" -> "Статус безопасности"
        else -> "Status Keamanan"
    }

    fun getVerified(lang: String) = when(lang) {
        "id" -> "Terverifikasi"
        "en" -> "Verified"
        "es" -> "Verificado"
        "vi" -> "Đã Xác Minh"
        "tr" -> "Doğrulandı"
        "hi" -> "सत्यापित"
        "ms" -> "Disahkan"
        "bn" -> "যাচাইকৃত"
        "ru" -> "Подтверждено"
        else -> "Terverifikasi"
    }

    fun getLogoutFromAccount(lang: String) = when(lang) {
        "id" -> "Keluar dari Akun"
        "en" -> "Logout from Account"
        "es" -> "Cerrar Sesión"
        "vi" -> "Đăng Xuất Khỏi Tài Khoản"
        "tr" -> "Hesaptan Çıkış Yap"
        "hi" -> "खाते से लॉगआउट करें"
        "ms" -> "Log Keluar dari Akaun"
        "bn" -> "অ্যাকাউন্ট থেকে লগআউট"
        "ru" -> "Выйти из аккаунта"
        else -> "Keluar dari Akun"
    }

    fun getLogoutConfirmation(lang: String) = when(lang) {
        "id" -> "Konfirmasi Keluar"
        "en" -> "Logout Confirmation"
        "es" -> "Confirmación de Cierre"
        "vi" -> "Xác Nhận Đăng Xuất"
        "tr" -> "Çıkış Onayı"
        "hi" -> "लॉगआउट पुष्टि"
        "ms" -> "Pengesahan Log Keluar"
        "bn" -> "লগআউট নিশ্চিতকরণ"
        "ru" -> "Подтверждение выхода"
        else -> "Konfirmasi Keluar"
    }

    fun getAreYouSureLogout(lang: String) = when(lang) {
        "id" -> "Apakah Anda yakin ingin keluar dari akun Stockity?"
        "en" -> "Are you sure you want to logout from your Stockity account?"
        "es" -> "¿Estás seguro de que quieres cerrar sesión de tu cuenta Stockity?"
        "vi" -> "Bạn có chắc chắn muốn đăng xuất khỏi tài khoản Stockity của mình không?"
        "tr" -> "Stockity hesabınızdan çıkmak istediğinizden emin misiniz?"
        "hi" -> "क्या आप वाकई अपने Stockity खाते से लॉगआउट करना चाहते हैं?"
        "ms" -> "Adakah anda pasti mahu log keluar dari akaun Stockity anda?"
        "bn" -> "আপনি কি নিশ্চিত যে আপনি আপনার Stockity অ্যাকাউন্ট থেকে লগআউট করতে চান?"
        "ru" -> "Вы уверены, что хотите выйти из вашего аккаунта Stockity?"
        else -> "Apakah Anda yakin ingin keluar dari akun Stockity?"
    }

    fun getNeedLoginAgain(lang: String) = when(lang) {
        "id" -> "Anda perlu login kembali untuk mengakses akun."
        "en" -> "You need to login again to access your account."
        "es" -> "Necesitas iniciar sesión nuevamente para acceder a tu cuenta."
        "vi" -> "Bạn cần đăng nhập lại để truy cập tài khoản của mình."
        "tr" -> "Hesabınıza erişmek için tekrar giriş yapmanız gerekiyor."
        "hi" -> "अपने खाते तक पहुंचने के लिए आपको फिर से लॉगिन करना होगा।"
        "ms" -> "Anda perlu log masuk semula untuk mengakses akaun anda."
        "bn" -> "আপনার অ্যাকাউন্ট অ্যাক্সেস করতে আপনাকে আবার লগইন করতে হবে।"
        "ru" -> "Вам нужно снова войти в систему, чтобы получить доступ к вашему аккаунту."
        else -> "Anda perlu login kembali untuk mengakses akun."
    }

    fun getYesLogout(lang: String) = when(lang) {
        "id" -> "Ya, Keluar"
        "en" -> "Yes, Logout"
        "es" -> "Sí, Cerrar Sesión"
        "vi" -> "Có, Đăng Xuất"
        "tr" -> "Evet, Çıkış Yap"
        "hi" -> "हां, लॉगआउट करें"
        "ms" -> "Ya, Log Keluar"
        "bn" -> "হ্যাঁ, লগআউট"
        "ru" -> "Да, выйти"
        else -> "Ya, Keluar"
    }

    fun getCancel(lang: String) = when(lang) {
        "id" -> "Batal"
        "en" -> "Cancel"
        "es" -> "Cancelar"
        "vi" -> "Hủy"
        "tr" -> "İptal"
        "hi" -> "रद्द करें"
        "ms" -> "Batal"
        "bn" -> "বাতিল"
        "ru" -> "Отмена"
        else -> "Batal"
    }

    fun getSuperAdminAccount(lang: String) = when(lang) {
        "id" -> "Akun Super Admin"
        "en" -> "Super Admin Account"
        "es" -> "Cuenta Super Admin"
        "vi" -> "Tài Khoản Super Admin"
        "tr" -> "Süper Admin Hesabı"
        "hi" -> "सुपर एडमिन खाता"
        "ms" -> "Akaun Super Admin"
        "bn" -> "সুপার অ্যাডমিন অ্যাকাউন্ট"
        "ru" -> "Аккаунт супер-администратора"
        else -> "Akun Super Admin"
    }

    fun getAdminAccount(lang: String) = when(lang) {
        "id" -> "Akun Admin"
        "en" -> "Admin Account"
        "es" -> "Cuenta Admin"
        "vi" -> "Tài Khoản Admin"
        "tr" -> "Admin Hesabı"
        "hi" -> "एडमिन खाता"
        "ms" -> "Akaun Admin"
        "bn" -> "অ্যাডমিন অ্যাকাউন্ট"
        "ru" -> "Аккаунт администратора"
        else -> "Akun Admin"
    }

    fun getStockityAccount(lang: String) = when(lang) {
        "id" -> "Akun Stockity"
        "en" -> "Stockity Account"
        "es" -> "Cuenta Stockity"
        "vi" -> "Tài Khoản Stockity"
        "tr" -> "Stockity Hesabı"
        "hi" -> "Stockity खाता"
        "ms" -> "Akaun Stockity"
        "bn" -> "Stockity অ্যাকাউন্ট"
        "ru" -> "Аккаунт Stockity"
        else -> "Akun Stockity"
    }

    fun getOnline(lang: String) = when(lang) {
        "id" -> "Online"
        "en" -> "Online"
        "es" -> "En Línea"
        "vi" -> "Trực Tuyến"
        "tr" -> "Çevrimiçi"
        "hi" -> "ऑनलाइन"
        "ms" -> "Dalam Talian"
        "bn" -> "অনলাইন"
        "ru" -> "Онлайн"
        else -> "Online"
    }

    fun getPortfolio(lang: String) = when(lang) {
        "id" -> "Portfolio"
        "en" -> "Portfolio"
        "es" -> "Cartera"
        "vi" -> "Danh Mục"
        "tr" -> "Portföy"
        "hi" -> "पोर्टफोलियो"
        "ms" -> "Portfolio"
        "bn" -> "পোর্টফোলিও"
        "ru" -> "Портфель"
        else -> "Portfolio"
    }

    fun getActive(lang: String) = when(lang) {
        "id" -> "Aktif"
        "en" -> "Active"
        "es" -> "Activo"
        "vi" -> "Hoạt Động"
        "tr" -> "Aktif"
        "hi" -> "सक्रिय"
        "ms" -> "Aktif"
        "bn" -> "সক্রিয়"
        "ru" -> "Активный"
        else -> "Aktif"
    }

    fun getSuperAdminPanel(lang: String) = when(lang) {
        "id" -> "Panel Super Admin"
        "en" -> "Super Admin Panel"
        "es" -> "Panel Super Admin"
        "vi" -> "Bảng Điều Khiển Super Admin"
        "tr" -> "Süper Admin Paneli"
        "hi" -> "सुपर एडमिन पैनल"
        "ms" -> "Panel Super Admin"
        "bn" -> "সুপার অ্যাডমিন প্যানেল"
        "ru" -> "Панель супер-администратора"
        else -> "Panel Super Admin"
    }

    fun getAdminPanel(lang: String) = when(lang) {
        "id" -> "Panel Admin"
        "en" -> "Admin Panel"
        "es" -> "Panel de Admin"
        "vi" -> "Bảng Điều Khiển Admin"
        "tr" -> "Admin Paneli"
        "hi" -> "एडमिन पैनल"
        "ms" -> "Panel Admin"
        "bn" -> "অ্যাডমিন প্যানেল"
        "ru" -> "Панель администратора"
        else -> "Panel Admin"
    }

    fun getSignalMode(lang: String) = when(lang) {
        "id" -> "Pilih Mode"
        "en" -> "Select Mode"
        "es" -> "Seleccionar Modo"
        "vi" -> "Chọn Chế Độ"
        "tr" -> "Modu Seçin"
        "hi" -> "मोड चुनें"
        "ms" -> "Pilih Mod"
        "bn" -> "মোড নির্বাচন করুন"
        "ru" -> "Выбрать режим"
        else -> "Pilih Mode"
    }

    fun getFastradeFTTMode(lang: String) = when(lang) {
        "id" -> "Mode Fastrade FTT"
        "en" -> "Fastrade FTT Mode"
        "es" -> "Modo Fastrade FTT"
        "vi" -> "Chế Độ Fastrade FTT"
        "tr" -> "Fastrade FTT Modu"
        "hi" -> "Fastrade FTT मोड"
        "ms" -> "Mod Fastrade FTT"
        "bn" -> "Fastrade FTT মোড"
        "ru" -> "Режим Fastrade FTT"
        else -> "Mode Fastrade FTT"
    }

    fun getAnalisisStrategyMode(lang: String) = when(lang) {
        "id" -> "Mode Strategi Analisis"
        "en" -> "Analisis Strategy Mode"
        "es" -> "Modo Estrategia de Análisis"
        "vi" -> "Chế Độ Chiến Lược Phân Tích"
        "tr" -> "Analiz Stratejisi Modu"
        "hi" -> "विश्लेषण रणनीति मोड"
        "ms" -> "Mod Strategi Analisis"
        "bn" -> "বিশ্লেষণ কৌশল মোড"
        "ru" -> "Режим стратегии анализа"
        else -> "Mode Strategi Analisis"
    }

    fun getFastradeCTCMode(lang: String) = when(lang) {
        "id" -> "Mode Fastrade CTC"
        "en" -> "Fastrade CTC Mode"
        "es" -> "Modo Fastrade CTC"
        "vi" -> "Chế Độ Fastrade CTC"
        "tr" -> "Fastrade CTC Modu"
        "hi" -> "Fastrade CTC मोड"
        "ms" -> "Mod Fastrade CTC"
        "bn" -> "Fastrade CTC মোড"
        "ru" -> "Режим Fastrade CTC"
        else -> "Mode Fastrade CTC"
    }

    fun getSelectTradingMode(lang: String) = when(lang) {
        "id" -> "Pilih Mode"
        "en" -> "Select Mode"
        "es" -> "Seleccionar Modo"
        "vi" -> "Chọn Chế Độ"
        "tr" -> "Modu Seçin"
        "hi" -> "मोड चुनें"
        "ms" -> "Pilih Mod"
        "bn" -> "মোড নির্বাচন করুন"
        "ru" -> "Выбрать режим"
        else -> "Pilih Mode"
    }

    fun getScheduleOrders(lang: String) = when(lang) {
        "id" -> "Pesan Terjadwal"
        "en" -> "Scheduled Orders"
        "es" -> "Órdenes Programadas"
        "vi" -> "Lệnh Được Lên Lịch"
        "tr" -> "Planlanmış Siparişler"
        "hi" -> "निर्धारित आदेश"
        "ms" -> "Pesanan Terjadwal"
        "bn" -> "নির্ধারিত অর্ডার"
        else -> "Pesan Terjadwal"
    }fun getFollowCandle(lang: String) = when(lang) {
        "id" -> "Ikuti Lilin"
        "en" -> "Follow Candle"
        "es" -> "Seguir Vela"
        "vi" -> "Theo Dõi Nến"
        "tr" -> "Mum Takip Et"
        "hi" -> "मोमबत्ती का पालन करें"
        "ms" -> "Ikut Lilin"
        "bn" -> "মোমবাতি অনুসরণ করুন"
        else -> "Ikuti Lilin"
    }fun getIndicatorAnalysis(lang: String) = when(lang) {
        "id" -> "Analisis Indikator"
        "en" -> "Indicator Analysis"
        "es" -> "Análisis de Indicadores"
        "vi" -> "Phân Tích Chỉ Báo"
        "tr" -> "Gösterge Analizi"
        "hi" -> "संकेतक विश्लेषण"
        "ms" -> "Analisis Penunjuk"
        "bn" -> "সূচক বিশ্লেষণ"
        else -> "Analisis Indikator"
    }fun getCTCUltraFast(lang: String) = when(lang) {
        "id" -> "CTC Ultra-Cepat"
        "en" -> "CTC Ultra-Fast"
        "es" -> "CTC Ultra-Rápido"
        "vi" -> "CTC Siêu Tốc"
        "tr" -> "CTC Ultra-Hızlı"
        "hi" -> "CTC अल्ट्रा-फास्ट"
        "ms" -> "CTC Ultra-Pantas"
        "bn" -> "CTC অতি-দ্রুত"
        else -> "CTC Ultra-Cepat"
    }fun getReadyForSignals(lang: String) = when(lang) {
        "id" -> "Siap untuk sinyal"
        "en" -> "Ready for signals"
        "es" -> "Listo para señales"
        "vi" -> "Sẵn sàng cho tín hiệu"
        "tr" -> "Sinyallere Hazır"
        "hi" -> "सिग्नल के लिए तैयार"
        "ms" -> "Sedia untuk isyarat"
        "bn" -> "সংকেতের জন্য প্রস্তুত"
        else -> "Siap untuk sinyal"
    }fun getReadyForAnalysis(lang: String) = when(lang) {
        "id" -> "Siap untuk analisis"
        "en" -> "Ready for analysis"
        "es" -> "Listo para análisis"
        "vi" -> "Sẵn sàng để phân tích"
        "tr" -> "Analiz İçin Hazır"
        "hi" -> "विश्लेषण के लिए तैयार"
        "ms" -> "Sedia untuk analisis"
        "bn" -> "বিশ্লেষণের জন্য প্রস্তুত"
        else -> "Siap untuk analisis"
    }fun getReadyForExecution(lang: String) = when(lang) {
        "id" -> "Siap untuk eksekusi"
        "en" -> "Ready for execution"
        "es" -> "Listo para ejecución"
        "vi" -> "Sẵn sàng để thực hiện"
        "tr" -> "Yürütme İçin Hazır"
        "hi" -> "निष्पादन के लिए तैयार"
        "ms" -> "Sedia untuk pelaksanaan"
        "bn" -> "সম্পাদনের জন্য প্রস্তুত"
        else -> "Siap untuk eksekusi"
    }fun getMonitoringSignals(lang: String) = when(lang) {
        "id" -> "Memantau sinyal trading"
        "en" -> "Monitoring trading signals"
        "es" -> "Monitoreando señales de trading"
        "vi" -> "Giám sát tín hiệu giao dịch"
        "tr" -> "Trading sinyalleri izleniyor"
        "hi" -> "ट्रेडिंग सिग्नल की निगरानी"
        "ms" -> "Memantau isyarat perdagangan"
        "bn" -> "ট্রেডিং সিগন্যাল পর্যবেক্ষণ করা হচ্ছে"
        else -> "Memantau sinyal trading"
    }fun getAnalyzingMarket(lang: String) = when(lang) {
        "id" -> "Menganalisis data pasar"
        "en" -> "Analyzing market data"
        "es" -> "Analizando datos de mercado"
        "vi" -> "Phân tích dữ liệu thị trường"
        "tr" -> "Piyasa verileri analiz ediliyor"
        "hi" -> "बाजार डेटा का विश्लेषण"
        "ms" -> "Menganalisis data pasaran"
        "bn" -> "বাজার ডেটা বিশ্লেষণ করা হচ্ছে"
        else -> "Menganalisis data pasar"
    }fun getTotalSignals(lang: String) = when(lang) {
        "id" -> "Total Sinyal"
        "en" -> "Total Signals"
        "es" -> "Señales Totales"
        "vi" -> "Tổng Tín Hiệu"
        "tr" -> "Toplam Sinyaller"
        "hi" -> "कुल संकेत"
        "ms" -> "Jumlah Isyarat"
        "bn" -> "মোট সংকেত"
        else -> "Total Sinyal"
    }fun getTotalOrders(lang: String) = when(lang) {
        "id" -> "Total Pesanan"
        "en" -> "Total Orders"
        "es" -> "Órdenes Totales"
        "vi" -> "Tổng Lệnh"
        "tr" -> "Toplam Siparişler"
        "hi" -> "कुल आदेश"
        "ms" -> "Jumlah Pesanan"
        "bn" -> "মোট অর্ডার"
        else -> "Total Pesanan"
    }fun getExecuted(lang: String) = when(lang) {
        "id" -> "Dieksekusi"
        "en" -> "Executed"
        "es" -> "Ejecutado"
        "vi" -> "Đã Thực Hiện"
        "tr" -> "Yürütüldü"
        "hi" -> "निष्पादित"
        "ms" -> "Dilaksanakan"
        "bn" -> "সম্পাদিত"
        else -> "Dieksekusi"
    }fun getPending(lang: String) = when(lang) {
        "id" -> "Tertunda"
        "en" -> "Pending"
        "es" -> "Pendiente"
        "vi" -> "Đang Chờ"
        "tr" -> "Beklemede"
        "hi" -> "लंबित"
        "ms" -> "Tertunda"
        "bn" -> "মুলতুবি"
        else -> "Tertunda"
    }fun getBuySignals(lang: String) = when(lang) {
        "id" -> "Sinyal Beli"
        "en" -> "Buy Signals"
        "es" -> "Señales de Compra"
        "vi" -> "Tín Hiệu Mua"
        "tr" -> "Satın Alma Sinyalleri"
        "hi" -> "खरीद सिग्नल"
        "ms" -> "Isyarat Beli"
        "bn" -> "ক্রয় সংকেত"
        else -> "Sinyal Beli"
    }fun getSellSignals(lang: String) = when(lang) {
        "id" -> "Sinyal Jual"
        "en" -> "Sell Signals"
        "es" -> "Señales de Venta"
        "vi" -> "Tín Hiệu Bán"
        "tr" -> "Satış Sinyalleri"
        "hi" -> "बेचने के संकेत"
        "ms" -> "Isyarat Jual"
        "bn" -> "বিক্রয় সংকেত"
        else -> "Sinyal Jual"
    }fun getHighConfidence(lang: String) = when(lang) {
        "id" -> "Kepercayaan Tinggi"
        "en" -> "High Confidence"
        "es" -> "Alta Confianza"
        "vi" -> "Độ Tin Cậy Cao"
        "tr" -> "Yüksek Güven"
        "hi" -> "उच्च आत्मविश्वास"
        "ms" -> "Keyakinan Tinggi"
        "bn" -> "উচ্চ আত্মবিশ্বাস"
        else -> "Kepercayaan Tinggi"
    }fun getActiveTargets(lang: String) = when(lang) {
        "id" -> "Target Aktif"
        "en" -> "Active Targets"
        "es" -> "Objetivos Activos"
        "vi" -> "Mục Tiêu Hoạt Động"
        "tr" -> "Aktif Hedefler"
        "hi" -> "सक्रिय लक्ष्य"
        "ms" -> "Sasaran Aktif"
        "bn" -> "সক্রিয় লক্ষ্য"
        else -> "Target Aktif"
    }fun getStartTrading(lang: String) = when(lang) {
        "id" -> "Mulai Trading"
        "en" -> "Start Trading"
        "es" -> "Comenzar a Operar"
        "vi" -> "Bắt Đầu Giao Dịch"
        "tr" -> "İşleme Başla"
        "hi" -> "ट्रेडिंग शुरू करें"
        "ms" -> "Mula Berdagang"
        "bn" -> "ট্রেডিং শুরু করুন"
        else -> "Mulai Trading"
    }fun getStopTrading(lang: String) = when(lang) {
        "id" -> "Hentikan Trading"
        "en" -> "Stop Trading"
        "es" -> "Detener Operación"
        "vi" -> "Dừng Giao Dịch"
        "tr" -> "İşlemi Durdur"
        "hi" -> "ट्रेडिंग बंद करें"
        "ms" -> "Hentikan Perdagangan"
        "bn" -> "ট্রেডিং বন্ধ করুন"
        else -> "Hentikan Trading"
    }fun getModeChanged(lang: String) = when(lang) {
        "id" -> "Mode trading telah diubah"
        "en" -> "Trading mode has been changed"
        "es" -> "El modo de trading ha sido cambiado"
        "vi" -> "Chế độ giao dịch đã được thay đổi"
        "tr" -> "İşlem modu değiştirildi"
        "hi" -> "ट्रेडिंग मोड बदल दिया गया है"
        "ms" -> "Mod perdagangan telah diubah"
        "bn" -> "ট্রেডিং মোড পরিবর্তন করা হয়েছে"
        else -> "Mode trading telah diubah"
    }fun getCannotSwitchMode(lang: String) = when(lang) {
        "id" -> "Tidak dapat mengganti mode saat bot sedang berjalan"
        "en" -> "Cannot switch mode while bot is running"
        "es" -> "No se puede cambiar el modo mientras el bot está ejecutándose"
        "vi" -> "Không thể chuyển chế độ khi bot đang chạy"
        "tr" -> "Bot çalışırken modu değiştiremezsiniz"
        "hi" -> "बॉट चल रहा है तो मोड नहीं बदल सकते"
        "ms" -> "Tidak boleh tukar mod semasa bot berjalan"
        "bn" -> "বট চলার সময় মোড পরিবর্তন করা যাবে না"
        else -> "Tidak dapat mengganti mode saat bot sedang berjalan"
    }fun getInactiveStatus(lang: String) = when(lang) {
        "id" -> "Tidak Aktif"
        "en" -> "Inactive"
        "es" -> "Inactivo"
        "vi" -> "Không Hoạt Động"
        "tr" -> "Pasif"
        "hi" -> "निष्क्रिय"
        "ms" -> "Tidak Aktif"
        "bn" -> "নিষ্ক্রিয়"
        else -> "Tidak Aktif"
    }fun getActiveStatus(lang: String) = when(lang) {
        "id" -> "Aktif"
        "en" -> "Active"
        "es" -> "Activo"
        "vi" -> "Hoạt Động"
        "tr" -> "Aktif"
        "hi" -> "सक्रिय"
        "ms" -> "Aktif"
        "bn" -> "সক্রিয়"
        else -> "Aktif"
    }fun getAdjustToYourTradingStyle(lang: String) = when(lang) {
        "id" -> "Sesuaikan dengan gaya bermain trading anda"
        "en" -> "Adjust to your trading style"
        "es" -> "Ajusta a tu estilo de trading"
        "vi" -> "Điều chỉnh theo phong cách trading của bạn"
        "tr" -> "Trading stilinize göre ayarlayın"
        "hi" -> "अपनी ट्रेडिंग शैली के अनुसार समायोजित करें"
        "ms" -> "Sesuaikan dengan gaya trading anda"
        "bn" -> "আপনার ট্রেডিং শৈলী অনুযায়ী সামঞ্জস্য করুন"
        else -> "Sesuaikan dengan gaya bermain trading anda"
    }fun getCurrencyLabel(lang: String): String {
        return when(lang) {
            "id" -> "Mata Uang"
            "en" -> "Currency"
            "es" -> "Moneda"
            "vi" -> "Tiền tệ"
            "tr" -> "Para Birimi"
            "hi" -> "मुद्रा"
            "ms" -> "Mata Wang"
            "bn" -> "মুদ্রা"
            else -> "Currency"
        }
    }fun getAccountInformation(lang: String): String {
        return when(lang) {
            "id" -> "Informasi Akun"
            "en" -> "Account Information"
            "es" -> "Información de Cuenta"
            "vi" -> "Thông tin Tài khoản"
            "tr" -> "Hesap Bilgileri"
            "hi" -> "खाता जानकारी"
            "ms" -> "Maklumat Akaun"
            "bn" -> "অ্যাকাউন্ট তথ্য"
            else -> "Account Information"
        }
    }fun getBalance(language: String) = when(language) {
        "id" -> "Saldo"
        "bn" -> "ব্যালেন্স"
        else -> "Balance"
    }fun getEntry(language: String): String {
        return when (language) {
            "id" -> "Masuk"
            "en" -> "Entry"
            "es" -> "Entrada"
            "vi" -> "Vào"
            "tr" -> "Giriş"
            "hi" -> "प्रवेश"
            "ms" -> "Masuk"
            "bn" -> "প্রবেশ"
            else -> "Entry"
        }
    }fun getExit(language: String): String {
        return when (language) {
            "id" -> "Keluar"
            "en" -> "Exit"
            "es" -> "Salida"
            "vi" -> "Ra"
            "tr" -> "Çıkış"
            "hi" -> "निकास"
            "ms" -> "Keluar"
            "bn" -> "প্রস্থান"
            else -> "Exit"
        }
    }fun getDemoBalance(language: String) = when(language) {
        "id" -> "Saldo Demo"
        "bn" -> "ডেমো ব্যালেন্স"
        else -> "Demo Balance"
    }fun getRealBalance(language: String) = when(language) {
        "id" -> "Saldo Real"
        "bn" -> "প্রকৃত ব্যালেন্স"
        else -> "Real Balance"
    }fun getRefreshBalance(language: String) = when(language) {
        "id" -> "Perbarui Saldo"
        "bn" -> "ব্যালেন্স রিফ্রেশ করুন"
        else -> "Refresh Balance"
    }

    fun getToday(lang: String): String {
        return when (lang) {
            "id" -> "Hari Ini"
            "en" -> "Today"
            "bn" -> "আজ"
            "ru" -> "Сегодня"
            else -> "Today"
        }
    }

    fun getNoTradesToday(lang: String): String {
        return when (lang) {
            "id" -> "Tidak ada trade hari ini"
            "en" -> "No trades today"
            "bn" -> "আজ কোনো ট্রেড নেই"
            "ru" -> "Сегодня нет сделок"
            else -> "No trades today"
        }
    }

    fun getTodayPerformance(lang: String): String {
        return when (lang) {
            "id" -> "Performa Hari Ini"
            "en" -> "Today's Performance"
            "zh" -> "今日表现"
            "hi" -> "आज का प्रदर्शन"
            "ar" -> "أداء اليوم"
            "pt" -> "Desempenho de Hoje"
            "bn" -> "আজকের পারফরম্যান্স"
            "ru" -> "Сегодняшние результаты"
            "ja" -> "今日のパフォーマンス"
            "es" -> "Rendimiento de Hoy"
            "fr" -> "Performance d'Aujourd'hui"
            "de" -> "Heutige Leistung"
            "ko" -> "오늘의 실적"
            "vi" -> "Hiệu Suất Hôm Nay"
            "th" -> "ผลการดำเนินงานวันนี้"
            "tr" -> "Bugünün Performansı"
            "it" -> "Prestazioni di Oggi"
            "pl" -> "Dzisiejsza Wydajność"
            "uk" -> "Сьогоднішня продуктивність"
            "nl" -> "Prestaties van Vandaag"
            "ms" -> "Prestasi Hari Ini"
            else -> "Today's Performance"
        }
    }

    fun getVerifyingAccess(lang: String) = when(lang) {
        "id" -> "Memverifikasi Akses"
        "en" -> "Verifying Access"
        "es" -> "Verificando Acceso"
        "vi" -> "Xác Minh Truy Cập"
        "tr" -> "Erişim Doğrulanıyor"
        "hi" -> "पहुंच सत्यापित करना"
        "ms" -> "Mengesahkan Akses"
        "ru" -> "Проверка Доступа"
        else -> "Verifying Access"
    }

    fun getPleaseWaitMoment(lang: String) = when(lang) {
        "id" -> "Mohon tunggu sebentar..."
        "en" -> "Please wait a moment..."
        "es" -> "Por favor espera un momento..."
        "vi" -> "Vui lòng đợi một chút..."
        "tr" -> "Lütfen bir dakika bekleyin..."
        "hi" -> "कृपया एक पल प्रतीक्षा करें..."
        "ms" -> "Sila tunggu sebentar..."
        "ru" -> "Пожалуйста, подождите..."
        else -> "Please wait a moment..."
    }

    fun getCheckingAccountStatus(lang: String) = when(lang) {
        "id" -> "Memeriksa Status Akun"
        "en" -> "Checking Account Status"
        "es" -> "Verificando Estado de Cuenta"
        "vi" -> "Kiểm Tra Trạng Thái Tài Khoản"
        "tr" -> "Hesap Durumu Kontrol Ediliyor"
        "hi" -> "खाता स्थिति जाँच रहा है"
        "ms" -> "Memeriksa Status Akaun"
        "ru" -> "Проверка Статуса Аккаунта"
        else -> "Checking Account Status"
    }

    fun getValidatingCredentials(lang: String) = when(lang) {
        "id" -> "Sistem sedang memvalidasi kredensial Anda"
        "en" -> "System is validating your credentials"
        "es" -> "El sistema está validando tus credenciales"
        "vi" -> "Hệ thống đang xác thực thông tin của bạn"
        "tr" -> "Sistem kimlik bilgilerinizi doğruluyor"
        "hi" -> "सिस्टम आपकी साख को सत्यापित कर रहा है"
        "ms" -> "Sistem sedang mengesahkan kelayakan anda"
        "ru" -> "Система проверяет ваши учетные данные"
        else -> "System is validating your credentials"
    }

    fun getPersonalInformation(lang: String) = when(lang) {
        "id" -> "Informasi Personal"
        "en" -> "Personal Information"
        else -> "Personal Information"
    }

    fun getGender(lang: String) = when(lang) {
        "id" -> "Jenis Kelamin"
        "en" -> "Gender"
        else -> "Gender"
    }

    fun getCountry(lang: String) = when(lang) {
        "id" -> "Negara"
        "en" -> "Country"
        else -> "Country"
    }

    fun getBirthday(lang: String) = when(lang) {
        "id" -> "Tanggal Lahir"
        "en" -> "Birthday"
        else -> "Birthday"
    }

    fun getDepositStatus(lang: String) = when(lang) {
        "id" -> "Status Deposit"
        "en" -> "Deposit Status"
        else -> "Deposit Status"
    }

    fun getHasDeposited(lang: String) = when(lang) {
        "id" -> "Sudah Deposit"
        "en" -> "Has Deposited"
        else -> "Has Deposited"
    }

    fun getNoDeposit(lang: String) = when(lang) {
        "id" -> "Belum Deposit"
        "en" -> "No Deposit"
        else -> "No Deposit"
    }
}