# MusicHub

Aplikasi Android untuk eksplorasi musik yang dibangun sebagai Tugas Final Lab Mobile Programming 2026. MusicHub memungkinkan pengguna menjelajahi lagu-lagu trending, melihat diskografi artis, mengelola playlist pribadi, dan mengakses lirik tersinkronisasi — semuanya dibungkus dalam tampilan gelap futuristik dengan estetika glassmorphism.

---

## Daftar Isi

- [Fitur](#fitur)
- [Tech Stack](#tech-stack)
- [Integrasi API](#integrasi-api)
- [Struktur Project](#struktur-project)
- [Skema Database](#skema-database)
- [Cara Menjalankan](#cara-menjalankan)
- [Cara Penggunaan](#cara-penggunaan)
- [Konvensi Commit](#konvensi-commit)
- [Catatan Implementasi Teknis](#catatan-implementasi-teknis)

---

## Fitur

**Utama**
- Menelusuri lagu trending dan chart teratas yang didukung oleh Last.fm API
- Tampilan now playing layar penuh dengan kontrol pemutaran dan visualizer waveform
- Lirik tersinkronisasi dengan sorotan baris aktif dan fitur berbagi lirik
- Halaman artis lengkap dengan biografi, statistik pendengar bulanan, total stream, dan diskografi penuh
- Tampilan detail album beserta daftar lagu dan jumlah stream per lagu
- Pencarian lagu, artis, album, dan rilis terbaru

**Manajemen Library**
- Menyukai atau batal menyukai lagu, disimpan secara persisten ke SQLite lokal
- Menyimpan lagu ke satu atau beberapa playlist melalui modal bottom sheet
- Membuat, mengganti nama, dan menghapus playlist pribadi
- Mengedit cover playlist dengan preset gradien atau foto kustom dari galeri

**Profil**
- Username dan foto profil yang dapat diedit (galeri atau kamera)
- Jumlah following artis yang real-time dari SQLite, langsung diperbarui saat follow atau unfollow
- Melihat semua artis yang diikuti beserta tag genre dan opsi unfollow
- Pilihan tema gelap dan terang yang disimpan melalui SharedPreferences

**Mode Offline**
- Lagu yang disukai dan playlist tetap dapat diakses penuh tanpa internet
- Gambar dari API di-cache ke SQLite dengan masa berlaku 24 jam
- Tombol retry ditampilkan di semua layar saat koneksi tidak tersedia

---

## Tech Stack

| Lapisan | Teknologi |
|---|---|
| Bahasa | Java |
| Minimum SDK | API 24 (Android 7.0) |
| Komponen UI | RecyclerView, ViewPager2, Navigation Component, BottomNavigationView |
| Networking | Retrofit 2.11.0 + Gson 2.10.1 |
| Pemuatan Gambar | Glide |
| Penyimpanan Lokal | SQLite (DatabaseHelper manual + pola DAO) |
| Preferensi | SharedPreferences |
| Threading | ExecutorService, Handler + Looper |
| Pemilih Gambar | ActivityResultLauncher (galeri dan kamera) |

---

## Integrasi API

MusicHub menggunakan dua API publik secara kombinasi: Last.fm untuk metadata musik dan Deezer untuk gambar resolusi tinggi.

### Last.fm API
Membutuhkan API key gratis dari [last.fm/api](https://www.last.fm/api).

| Endpoint | Kegunaan |
|---|---|
| `chart.getTopTracks` | Lagu trending di layar Home |
| `tag.getTopTracks` | Daftar lagu berdasarkan filter genre |
| `track.search` | Fitur pencarian |
| `track.getInfo` | Detail dan metadata lagu |
| `artist.getInfo` | Biografi artis dan statistik pendengar |
| `artist.getTopAlbums` | Daftar diskografi artis |
| `album.getInfo` | Daftar lagu dalam album |
| `chart.getTopArtists` | Bagian artis teratas |

### Deezer API
Tidak membutuhkan API key. Digunakan khusus untuk aset gambar.

| Endpoint | Kegunaan |
|---|---|
| `GET /search?q={track}` | Cover album (`album.cover_xl`) |
| `GET /search/artist?q={name}` | Foto artis (`picture_xl`) |
| `GET /artist/{id}` | Gambar artis resolusi tinggi |

### Cara Kerja Kombinasi

Saat memuat lagu atau artis, aplikasi pertama-tama mengambil metadata dari Last.fm, kemudian mengirimkan request kedua ke Deezer untuk mendapatkan URL gambar. Kedua hasil digabungkan di memori dan URL gambar di-cache ke SQLite agar tidak perlu request ulang pada pemuatan berikutnya.

---

## Struktur Project

```
com.ishmah.musichub/
├── activity/
│   ├── SplashActivity.java         -- Launcher dengan animasi progress bar
│   ├── MainActivity.java           -- Host Fragment dengan Bottom Navigation
│   ├── DetailActivity.java         -- Layar now playing penuh
│   ├── ArtistActivity.java         -- Halaman artis dengan diskografi
│   ├── AlbumActivity.java          -- Detail album dengan daftar lagu
│   ├── LyricsActivity.java         -- Tampilan lirik tersinkronisasi
│   ├── PlaylistDetailActivity.java -- Tampilan dan manajemen playlist
│   └── EditProfileActivity.java    -- Layar pengeditan profil
├── fragment/
│   ├── HomeFragment.java           -- Lagu trending dan featured cards
│   ├── SearchFragment.java         -- Pencarian multi-kategori
│   ├── FavoriteFragment.java       -- Lagu disukai dan playlist
│   └── ProfileFragment.java        -- Ringkasan profil pengguna
├── adapter/
│   ├── TrackAdapter.java
│   ├── AlbumAdapter.java
│   ├── PlaylistCardAdapter.java
│   ├── PlaylistTrackAdapter.java
│   └── FeaturedCardAdapter.java
├── api/
│   ├── LastFmApi.java              -- Interface Retrofit untuk Last.fm
│   ├── DeezerApi.java              -- Interface Retrofit untuk Deezer
│   └── ApiClient.java              -- Builder instance Retrofit
├── database/
│   ├── DatabaseHelper.java         -- SQLiteOpenHelper, pembuatan skema
│   ├── FavoriteDao.java
│   ├── PlaylistDao.java
│   ├── ArtistDao.java
│   ├── UserProfileDao.java
│   └── CachedImageDao.java
├── model/
│   ├── Track.java
│   ├── Artist.java
│   ├── Album.java
│   └── Playlist.java
├── dialog/
│   └── AddToPlaylistDialog.java    -- Bottom sheet untuk menyimpan ke playlist
└── utils/
    ├── NetworkChecker.java
    └── MusicPlayerManager.java
```

---

## Skema Database

### `favorites`
| Kolom | Tipe | Keterangan |
|---|---|---|
| track_id | TEXT PRIMARY KEY | |
| track_name | TEXT | |
| artist_name | TEXT | |
| album_art | TEXT | URL gambar Deezer |
| duration | INTEGER | Milidetik |

### `playlists`
| Kolom | Tipe | Keterangan |
|---|---|---|
| playlist_id | INTEGER PRIMARY KEY | Autoincrement |
| name | TEXT | |
| cover_type | TEXT | `gradient` atau `uri` |
| cover_value | TEXT | Kunci gradien atau path file |
| created_at | INTEGER | Unix timestamp |

### `playlist_tracks`
| Kolom | Tipe | Keterangan |
|---|---|---|
| id | INTEGER PRIMARY KEY | Autoincrement |
| playlist_id | INTEGER | Foreign key |
| track_id | TEXT | |

### `following_artists`
| Kolom | Tipe | Keterangan |
|---|---|---|
| artist_id | TEXT PRIMARY KEY | |
| artist_name | TEXT | |
| artist_photo | TEXT | URL picture_xl Deezer |
| genre | TEXT | |
| followed_at | INTEGER | Unix timestamp |

### `user_profile`
| Kolom | Tipe | Keterangan |
|---|---|---|
| username | TEXT | |
| bio | TEXT | |
| photo_uri | TEXT | URI file lokal |
| theme | TEXT | `dark` atau `light` |
| notif_enabled | INTEGER | 0 atau 1 |

### `cached_images`
| Kolom | Tipe | Keterangan |
|---|---|---|
| query_key | TEXT PRIMARY KEY | `namaLagu_namaArtis` |
| cover_url | TEXT | |
| artist_photo | TEXT | |
| cached_at | INTEGER | Kedaluwarsa setelah 24 jam |

---

## Cara Menjalankan

### Prasyarat

- Android Studio Hedgehog atau lebih baru
- Perangkat Android atau emulator dengan API 24 ke atas
- API key Last.fm gratis dari [last.fm/api](https://www.last.fm/api)

### Langkah Instalasi

1. Clone repository:
```bash
git clone https://github.com/ishmahsalter/MusicHub.git
cd MusicHub
```

2. Buka project di Android Studio.

3. Tambahkan API key Last.fm. Di file `app/src/main/java/com/ishmah/musichub/api/ApiClient.java`, ganti placeholder berikut:
```java
private static final String LASTFM_API_KEY = "masukkan_api_key_disini";
```

4. Sync Gradle lalu jalankan project di perangkat atau emulator.

---

## Cara Penggunaan

**Menelusuri Musik**
Buka aplikasi dan layar Home akan memuat lagu trending secara otomatis. Ketuk chip genre untuk memfilter berdasarkan kategori. Ketuk lagu mana pun untuk membuka layar Now Playing penuh.

**Now Playing**
Dari layar Now Playing, gunakan tombol aksi untuk melihat lirik tersinkronisasi, menyimpan lagu ke playlist, membuka halaman album, atau mengunjungi halaman artis.

**Halaman Artis**
Menampilkan biografi, jumlah pendengar bulanan, total stream, dan diskografi yang dapat di-scroll. Ketuk Follow untuk menambahkan artis ke daftar following — langsung tercermin di halaman Profil.

**Library**
Tab Favorit menampilkan semua lagu yang disukai dan playlist, dimuat dari SQLite lokal. Bagian ini berfungsi penuh secara offline.

**Profil**
Ketuk ikon edit untuk mengubah username atau foto profil. Jumlah following mencerminkan tepat berapa artis yang telah diikuti melalui tombol Follow di halaman artis.

**Mode Offline**
Saat tidak ada koneksi internet, semua layar yang bergantung pada API menampilkan tombol Retry. Data yang sebelumnya dimuat dari cache lokal tetap ditampilkan seperti biasa.

---

## Konvensi Commit

Project ini mengikuti spesifikasi conventional commits:

| Prefix | Kegunaan |
|---|---|
| `feat:` | Fitur baru atau layar baru |
| `fix:` | Perbaikan bug |
| `refactor:` | Restrukturisasi kode tanpa perubahan perilaku |
| `style:` | Perubahan UI atau layout |
| `docs:` | Pembaruan dokumentasi |
| `chore:` | Konfigurasi build, dependensi, setup project |

---

## Catatan Implementasi Teknis

**Background Threading**
Semua operasi baca dan tulis SQLite dijalankan di thread `ExecutorService` yang didedikasikan melalui lapisan DAO agar tidak memblokir main thread. Progress bar Now Playing dan posisi scroll lirik diperbarui melalui `Handler(Looper.getMainLooper())` dari runnable latar belakang.

**Pipeline Gambar**
Gambar diambil dari Deezer secara paralel menggunakan pool `ExecutorService` 4 thread di HomeFragment. URL yang telah diselesaikan ditulis ke tabel SQLite `cached_images` dan kedaluwarsa setelah 24 jam. Glide menangani cache memori dan disk untuk gambar yang dirender.

**Navigasi**
Aplikasi menggunakan Android Navigation Component dengan satu `nav_graph.xml` yang mendefinisikan empat tujuan Fragment tingkat atas. Activity di luar graf utama (DetailActivity, ArtistActivity, AlbumActivity, LyricsActivity) diluncurkan melalui Intent eksplisit dengan extras yang diketik.

**Tema**
Tema yang dipilih (gelap atau terang) disimpan di SharedPreferences dan diterapkan di setiap `onCreate` Activity sebelum `setContentView` dipanggil, memastikan tidak ada tampilan tanpa gaya saat peluncuran.

---

*Tugas Final Lab Mobile 2026 — Ishmah Nurwasilah*
