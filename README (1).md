# STIS-MAN

Game arcade bergaya Pac-Man — Java Swing/AWT murni (tanpa library eksternal, tanpa build tool). Pemain mengendalikan STIS-Man di dalam labirin 21×19 untuk menghabiskan semua makanan sambil menghindari 4 hantu yang bergerak acak, dengan power-up yang membuat hantu bisa dimakan sementara. Dilengkapi menu utama, sistem nyawa & skor, dan layar menang/kalah dengan tombol Restart/Next/Menu.

## Stack

- Java 21 (dites dengan OpenJDK 21; kode hanya memakai fitur Java 8+ seperti lambda, jadi JDK 8+ seharusnya cukup)
- Java Swing (`javax.swing`) — jendela, panel, tombol, `CardLayout` untuk pindah layar
- Java AWT (`java.awt`) — rendering 2D lewat `paintComponent`, input via `KeyListener`
- Game loop: `javax.swing.Timer` 50 ms per tick (±20 FPS)
- Aset: sprite PNG yang dimuat lewat `getClass().getResource(...)` (classpath)
- Tidak ada Maven/Gradle, tidak ada dependensi eksternal — kompilasi langsung dengan `javac`

## Instalasi dan Cara Menjalankan

Prasyarat: **JDK** (bukan hanya JRE) terpasang.

```bash
java -version
javac -version
```

Belum ada? Unduh dari [Adoptium](https://adoptium.net/) atau install lewat package manager:

```bash
# Ubuntu / Debian
sudo apt install default-jdk

# macOS (Homebrew)
brew install openjdk

# Windows
# Unduh installer dari https://adoptium.net/ lalu pastikan folder bin JDK masuk ke PATH
```

### 1. Clone, kompilasi, jalankan

```bash
git clone https://github.com/akbarakira/stisman-game.git
cd stisman-game
javac App.java STISMan.java
java App
```

Semua file `.png` **harus berada satu folder** dengan file `.class` (gambar dimuat relatif terhadap class `STISMan`). Jalankan `java App` dari folder tersebut.

### 2. Lewat IDE

Buka folder di VS Code (Extension Pack for Java), IntelliJ IDEA, atau Eclipse, pastikan PNG ikut ter-copy ke folder output/classpath, lalu jalankan `App.java` (satu-satunya class dengan `main`).

> **Penting untuk Linux/macOS** — lihat bagian [Catatan Implementasi Kritis](#catatan-implementasi-kritis) poin 1 soal huruf besar/kecil nama file sprite pemain.

## Alur Layar

```
App.main()
 └─ JFrame "STIS-MAN" (tidak bisa di-resize, CardLayout)
      └─ menuPanel ("menu")
           ├─ MULAI ────► new STISMan(menuAction) ──► layar "game"
           │                    ├─ semua makanan habis ──► "Anda Menang"
           │                    │        ├─ NEXT ──► restartGame()
           │                    │        └─ MENU ──► gameLoop.stop() ──► kembali ke menuPanel
           │                    └─ nyawa habis ──► "GAME OVER"
           │                             ├─ RESTART ──► restartGame()
           │                             └─ MENU ──► gameLoop.stop() ──► kembali ke menuPanel
           ├─ PETUNJUK ──► dialog (JOptionPane): cara gerak & tujuan game
           └─ KELUAR ──► System.exit(0)
```

## Cara Bermain

| Tombol | Fungsi |
|--------|--------|
| ↑ | Bergerak ke atas |
| ↓ | Bergerak ke bawah |
| ← | Bergerak ke kiri |
| → | Bergerak ke kanan |

| Aksi | Poin |
|------|------|
| Makan 1 makanan | +10 |
| Makan hantu saat power mode | +50 |

- Pemain mulai dengan **3 nyawa**. Bersentuhan dengan hantu (di luar power mode) mengurangi 1 nyawa, lalu pemain dan semua hantu dikembalikan ke posisi awal (makanan yang sudah dimakan tetap hilang).
- **Menang**: semua makanan habis. Power-up tidak wajib diambil.
- **Kalah**: nyawa mencapai 0.
- Jika tombol panah tidak merespons, klik sekali pada area game agar panel mendapat fokus keyboard.

## Arsitektur

```
stisman-game/
├── App.java          -> entry point: JFrame, CardLayout, menu utama (MULAI / PETUNJUK / KELUAR)
├── STISMan.java      -> seluruh logika game + rendering (extends JPanel, implements ActionListener, KeyListener)
│   └── class Block   -> objek generik (pemain, hantu, dinding, makanan, power-up): posisi, ukuran, arah, kecepatan
├── *.class           -> hasil kompilasi (bisa dibuat ulang dengan javac)
├── wall.png                                  -> tekstur dinding
├── stismanUp / Down / Left / Right .png      -> sprite pemain per arah
├── redGhost / blueGhost / pinkGhost / orangeGhost .png -> sprite hantu
├── scaredGhost.png                           -> sprite hantu saat power mode
├── powerFood.png                             -> sprite makanan biasa
├── powerUp.png                               -> sprite power-up
└── cherry.png, cherry2.png                   -> belum dipakai di kode (aset cadangan)
```

**`App`**
- `main()` membuat `JFrame` berukuran `columnCount * tileSize` × `rowCount * tileSize` (608 × 672 px) dengan `CardLayout`.
- `createMenuPanel()` membangun menu (`GridBagLayout`, latar hitam, judul kuning) dan menghubungkan tombol.
- Tombol MULAI membuat instance `STISMan` baru setiap kali ditekan, dengan `Runnable menuAction` yang membuang panel game dan kembali ke menu.

**`STISMan`**
- `loadMap()` — membaca array `tileMap` karakter demi karakter, membuat `Block` untuk dinding, hantu, pemain, dan makanan, lalu memanggil `spawnPowerUps()`.
- `move()` — satu tick logika game (lihat [Game Loop](#game-loop-per-tick)).
- `draw()` / `paintComponent()` — menggambar pemain, hantu, dinding, makanan, power-up, lalu HUD (`x<nyawa> Score: <skor>`) atau overlay menang/kalah.
- `collision(a, b)` — tabrakan AABB (kotak vs kotak) sederhana, dipakai untuk semua jenis tabrakan.
- `resetPositions()`, `restartGame()` — reset setelah kehilangan nyawa / mulai ulang total.

**`Block`** (kelas dalam)
- `updateDirection(dir)` — dipakai hantu: set arah, coba maju satu langkah, dan batalkan (kembali ke arah sebelumnya) kalau menabrak dinding.
- `requestDirection(dir)` + `tryRequestedDirection()` — dipakai pemain: *buffered input* (lihat bagian berikutnya).
- `canTurnWithinOneTile()`, `hitsWall()` — pengecekan apakah belokan yang diminta bisa dilakukan dalam jarak satu tile ke depan.
- `updateVelocity()` — kecepatan = `tileSize / 4` = **8 px per tick** (4 tick per tile).

## Peta Level

Layout labirin didefinisikan sebagai `String[] tileMap` di `STISMan.java` (21 baris × 19 kolom):

| Karakter | Arti |
|----------|------|
| `X` | Dinding |
| ` ` (spasi) | Jalan dengan makanan (sprite 16×16 di tengah tile) |
| `P` | Posisi awal pemain (menghadap kanan) |
| `r` `b` `p` `o` | Posisi awal hantu merah, biru, pink, oranye (di "rumah hantu" tengah peta) |
| `O` | Tile kosong tanpa makanan — kantong di tepi kiri/kanan peta yang tidak terhubung ke jalur utama |

Untuk membuat level baru, ubah `tileMap`. Jika ukurannya berubah, sesuaikan juga `rowCount` / `columnCount` di **kedua** file (`App.java` dan `STISMan.java`).

## Game Loop (per tick)

`Timer` memanggil `actionPerformed()` → `move()` + `repaint()` tiap 50 ms. Urutan di dalam `move()`:

```
updatePowerMode()                      kurangi powerModeTicks; kalau habis -> hantu normal lagi
        │
        ▼
STISMan.tryRequestedDirection()        belok jika arah yang diminta memungkinkan
        │
        ▼
gerak pemain + cek dinding             tabrak dinding -> batalkan langkah
        │
        ▼
untuk tiap hantu (yang belum defeated):
   ├─ bersentuhan dengan pemain?
   │     ├─ powerMode  -> hantu defeated, skor +50
   │     └─ normal     -> nyawa -1; 0 = GAME OVER, selain itu resetPositions()
   ├─ hantu di baris y = 9 tile dan sedang bergerak horizontal -> paksa arah 'U' (keluar rumah hantu)
   └─ gerak hantu; tabrak dinding / tepi layar -> mundur + pilih arah acak
        │
        ▼
cek makanan                            kena = hapus, skor +10
        │
        ▼
foods.isEmpty() ?                      ya -> gameWon, tampilkan NEXT + MENU
        │
        ▼
cek power-up                           kena = powerMode ON, powerModeTicks = 100, semua hantu -> scaredGhost
```

Saat `gameOver` atau `gameWon`, `actionPerformed()` menghentikan `Timer`; `restartGame()` memuat ulang peta dan menyalakannya lagi.

## Power-Up dan Power Mode

- `spawnPowerUps()` menaruh **4 power-up** di tile acak yang berisi makanan (karakter spasi di `tileMap`) dan menghapus makanan di tile itu — jadi power-up menggantikan makanan, dan total makanan yang harus dihabiskan berkurang.
- Posisi power-up diacak ulang setiap `loadMap()` (awal game dan setiap restart).
- Mengambil power-up mengaktifkan **power mode** selama `POWER_MODE_DURATION = 100` tick ≈ **5 detik**. Mengambil power-up lagi saat masih aktif mengisi ulang timernya ke 100.
- Selama power mode: sprite semua hantu berganti ke `scaredGhost.png`, hantu **tetap bergerak acak** (tidak kabur), dan bersentuhan dengan hantu = hantu hilang + 50 poin.
- Hantu yang dimakan (`defeated`) tidak digambar dan tidak bergerak sampai power mode berakhir. Setelah itu mereka muncul lagi **di posisi tempat mereka dimakan** (bukan di rumah hantu) dan diberi arah acak baru.

## Buffered Direction Input

Pemain bergerak otomatis ke arah terakhir yang valid; tombol panah hanya *meminta* arah baru (`requestDirection`). Tiap tick, `tryRequestedDirection()`:

1. Mengecek lewat `canTurnWithinOneTile()` apakah dalam jarak satu tile ke depan ada posisi di mana belokan yang diminta tidak menabrak dinding. Kalau tidak, permintaan dibuang.
2. Kalau lolos, mencoba belok sekarang; jika langsung menabrak dinding, arah lama dipertahankan dan permintaan tetap ditunggu tick berikutnya.
3. Jika berhasil belok, sprite pemain diganti sesuai arah (`updateSTISManImage`).

Efeknya belokan terasa mulus: pemain tidak perlu menekan tombol tepat di persimpangan.

## Catatan Implementasi Kritis

1. **Nama file sprite pemain beda huruf besar/kecil dengan kode.** Kode memuat `STISManUp.png`, `STISManDown.png`, `STISManLeft.png`, `STISManRight.png`, tetapi file di repo bernama `stismanUp.png`, dst. (huruf kecil). Windows tidak membedakan, **Linux dan macOS (filesystem case-sensitive) akan crash dengan `NullPointerException` saat `new STISMan()`**. Perbaikan — pilih salah satu: rename file (`mv stismanUp.png STISManUp.png`, dan seterusnya untuk Down/Left/Right), atau ubah string di konstruktor `STISMan` agar cocok dengan nama file.
2. **Dua file harus sinkron:** `rowCount`, `columnCount`, dan `tileSize` didefinisikan terpisah di `App.java` dan `STISMan.java`. Mengubah salah satu tanpa yang lain membuat ukuran jendela tidak cocok dengan papan.
3. **Gerakan berbasis grid kasar:** semua objek bergerak 8 px per tick pada tile 32 px, jadi posisi selalu kelipatan 8. Deteksi "hantu di baris `y == tileSize*9`" bergantung pada kenyataan ini.
4. **Tabrakan memakai AABB** untuk semua hal (dinding, makanan, hantu, power-up) lewat satu fungsi `collision()`. Sprite makanan/power-up berukuran 16×16 dengan offset +8 agar berada di tengah tile 32×32.
5. **Hantu tidak punya AI pengejar**: arah dipilih acak (`Random`) setiap kali menabrak dinding atau tepi layar. Perilaku ini sengaja sederhana.
6. **`gameLoop.stop()` dipanggil sebelum kembali ke menu** (tombol MENU) supaya `Timer` dari panel game lama tidak terus berjalan di belakang layar. Setiap MULAI membuat objek `STISMan` baru.
7. **Tombol MENU/RESTART/NEXT berada di dalam panel game** (`setLayout(null)` + `setBounds`) dan hanya `setVisible(true)` saat game selesai. `NEXT` saat ini memanggil `restartGame()` yang sama dengan RESTART — memuat ulang peta yang sama, skor dan nyawa di-reset.
8. **Resource dimuat lewat classpath** (`getClass().getResource("./nama.png")`), bukan path file. Jadi PNG harus berada di folder/classpath yang sama dengan `.class`, termasuk saat dijalankan dari IDE.

## Status Implementasi

- [x] Menu utama (MULAI, PETUNJUK, KELUAR) dengan `CardLayout`
- [x] Labirin 21×19 dari `tileMap`, rendering sprite per tile
- [x] Gerak pemain dengan tombol panah + buffered direction input
- [x] Sprite pemain berganti sesuai arah
- [x] 4 hantu bergerak acak dengan sprite berbeda (merah, biru, pink, oranye)
- [x] Sistem nyawa (3) dan skor (+10 makanan, +50 hantu), HUD di layar
- [x] Power-up acak (4 buah) + power mode ±5 detik (hantu ketakutan & bisa dimakan)
- [x] Layar menang ("Anda Menang") dan kalah ("GAME OVER") dengan tombol Restart / Next / Menu
- [x] Kembali ke menu dari layar game tanpa menumpuk `Timer`
- [ ] Level berikutnya yang sebenarnya (tombol NEXT saat ini hanya mengulang peta yang sama)
- [ ] High score tersimpan ke file
- [ ] Bonus item memakai `cherry.png` / `cherry2.png` (aset sudah ada, belum dipakai)
- [ ] AI hantu yang mengejar pemain
- [ ] Tunnel wrap di sisi kiri/kanan labirin
- [ ] Efek suara, musik, tombol jeda, kontrol WASD

## Yang Sudah Diverifikasi Selama Penulisan README Ini

- **Kompilasi bersih**: `App.java` + `STISMan.java` dikompilasi dengan `javac` dari OpenJDK 21 tanpa error maupun warning.
- **Bug nama file terkonfirmasi**: `new STISMan(...)` dijalankan secara headless di Linux dengan PNG apa adanya → `NullPointerException` saat memuat sprite. Setelah 4 file sprite pemain di-rename ke `STISManUp/Down/Left/Right.png`, panel berhasil dibuat tanpa error. Ini dasar dari Catatan Implementasi Kritis nomor 1.

**Catatan jujur**: game belum saya mainkan secara visual (sandbox tidak punya layar/display), jadi tampilan, kehalusan gerak, perilaku hantu, dan kenyamanan kontrol tidak ikut diuji. Semua deskripsi gameplay di atas (game loop, power mode, aturan skor, alur menu) berasal dari pembacaan kode baris per baris, bukan dari hasil bermain. Kalau ada hal yang tidak cocok dengan perilaku asli saat kamu menjalankannya, kirim detailnya dan README bisa langsung dikoreksi.
