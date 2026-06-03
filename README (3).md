# 🏷️ AuctionApp — Hệ Thống Đấu Giá Trực Tuyến Thời Gian Thực

## 1. Mô Tả Bài Toán & Phạm Vi Hệ Thống

AuctionApp là một ứng dụng đấu giá trực tuyến đa người dùng, hoạt động theo mô hình **Client-Server** qua giao tiếp TCP Socket. Hệ thống cho phép người dùng đăng ký tài khoản, đăng tin bán đấu giá sản phẩm, đặt giá thầu thủ công hoặc tự động (auto-bid), nhận thông báo thời gian thực khi có bid mới hoặc phiên đấu giá kết thúc. Quản trị viên có giao diện riêng để quản lý người dùng và các phiên đấu giá đang diễn ra.

**Phạm vi hệ thống:**
- **Người dùng thông thường:** đăng ký / đăng nhập, duyệt sản phẩm theo danh mục, đặt giá thầu, xem lịch sử bid, nhận thông báo thời gian thực
- **Người bán:** đăng sản phẩm lên đấu giá (kèm ảnh, mô tả, giá khởi điểm, thời hạn), chỉnh sửa thông tin sản phẩm đang đấu giá
- **Hệ thống tự động:** đóng phiên đấu giá đúng giờ, thông báo người thắng / thua / người bán, gia hạn phiên khi có bid sát giờ kết thúc (anti-sniping)
- **Admin:** xem / khoá tài khoản người dùng, quản lý và xoá tất cả phiên đấu giá

---

## 2. Công Nghệ Sử Dụng, Môi Trường Chạy & Yêu Cầu Cài Đặt

| Thành phần | Công nghệ |
|---|---|
| Ngôn ngữ | Java 21+ |
| Giao diện | JavaFX 21 + FXML |
| Giao tiếp mạng | TCP Socket + JSON (Gson 2.10.1) |
| Cơ sở dữ liệu | PostgreSQL (Supabase cloud) |
| Lưu trữ ảnh | Supabase Storage (REST API) |
| Build tool | Maven 3.8+ |

**Yêu cầu môi trường:**
- JDK 21 trở lên ([Tải tại đây](https://adoptium.net/)) — project tương thích từ JDK 21+, **chọn phiên bản phù hợp với máy mình** (ví dụ: máy đang dùng `jdk-25.0.2` thì vẫn chạy được)
- Maven 3.8+ ([Tải tại đây](https://maven.apache.org/download.cgi))
- Kết nối Internet (để kết nối Supabase cloud DB)
- Biến môi trường: `SUPABASE_SERVICE_KEY` — xem hướng dẫn chi tiết bên dưới

### 📌 SUPABASE_SERVICE_KEY là gì và lấy ở đâu?

> **Giải thích đơn giản:** Database (cơ sở dữ liệu) của project được lưu trên cloud Supabase — tức là không lưu trên máy tính của bạn mà lưu trên internet. `SUPABASE_SERVICE_KEY` là **mật khẩu bí mật** để ứng dụng được phép kết nối và đọc/ghi dữ liệu vào database đó. Không có key này, server sẽ báo lỗi ngay khi khởi động và không thể đăng nhập hay xem sản phẩm gì cả.

**Cách lấy key (dành cho người tạo Supabase project):**
1. Truy cập [https://supabase.com/dashboard](https://supabase.com/dashboard) → đăng nhập
2. Chọn đúng project của nhóm
3. Vào **Settings** → **API**
4. Tìm mục **`service_role`** (⚠️ không phải `anon`) → nhấn **Copy**

> ⚠️ **Lưu ý bảo mật:**
> - Key này **chỉ cần 1 người trong nhóm** (người setup Supabase) cung cấp cho các thành viên còn lại
> - **Tuyệt đối không commit key lên GitHub** — nếu lỡ push lên thì phải vào Supabase reset key ngay lập tức
> - Mỗi lần mở terminal/CMD mới phải thiết lập lại key (hoặc thêm vào file `.env` / script `.bat` để tiện hơn)

**Thiết lập biến môi trường:**

```bash
# Linux / macOS
export SUPABASE_SERVICE_KEY=your_service_role_key_here

# Windows CMD
set SUPABASE_SERVICE_KEY=your_service_role_key_here

# Windows PowerShell
$env:SUPABASE_SERVICE_KEY="your_service_role_key_here"
```

---

## 3. Cấu Trúc Thư Mục & Module Chính

```
LTNC_Project/
├── pom.xml                          # Maven build config
├── mvnw / mvnw.cmd                  # Maven wrapper scripts
├── src/
│   └── main/
│       ├── java/com/example/auctionapp/
│       │   ├── AuctionApplication.java      # Entry point (JavaFX)
│       │   ├── Launcher.java                # Launcher wrapper (tránh lỗi module JavaFX)
│       │   ├── server/
│       │   │   ├── AuctionServer.java        # TCP server, lắng nghe port 5000
│       │   │   ├── ClientHandler.java        # Xử lý lệnh từng client trên luồng riêng
│       │   │   ├── AuctionManager.java       # Singleton quản lý phiên đấu giá trong RAM
│       │   │   └── DatabaseManager.java      # Toàn bộ truy vấn SQL + Supabase Storage API
│       │   ├── model/
│       │   │   ├── AuctionSession.java       # Trạng thái 1 phiên đấu giá + ReentrantLock
│       │   │   ├── AuctionObserver.java      # Observer pattern interface
│       │   │   ├── NetworkMessage.java       # JSON envelope chuẩn client-server
│       │   │   ├── Items.java                # Model sản phẩm
│       │   │   ├── Member.java               # Model người dùng
│       │   │   └── BidTransaction.java       # Model giao dịch bid
│       │   ├── controller/
│       │   │   ├── LoginController.java
│       │   │   ├── SignUpController.java
│       │   │   ├── HomeController.java
│       │   │   ├── BrowseController.java
│       │   │   ├── DetailedBidController.java
│       │   │   ├── SellItemController.java
│       │   │   ├── myBidController.java
│       │   │   ├── myAuctionController.java
│       │   │   ├── NotifController.java
│       │   │   ├── DashboardController.java
│       │   │   ├── AdminDashboardController.java
│       │   │   ├── AdminUserManagementController.java
│       │   │   └── AdminAuctionManagementController.java
│       │   ├── exception/
│       │   │   ├── AuctionClosedException.java
│       │   │   ├── InvalidBidException.java
│       │   │   ├── SelfBiddingException.java
│       │   │   └── AuthenticationException.java
│       │   └── network/
│       │       └── NetworkRouter.java        # Parse JSON, điều hướng message tới Controller
│       └── resources/
│           ├── com/example/auctionapp/      # FXML views + CSS
│           └── images/                      # Asset ảnh danh mục
```

---

## 4. Câu Lệnh Compile & Build

### Yêu cầu trước khi build
Đảm bảo JDK 21+ và Maven 3.8+ đã được cài đặt và có trong PATH.

```bash
# Kiểm tra phiên bản Java
java -version

# Kiểm tra phiên bản Maven
mvn -version
```

### Compile toàn bộ project

**Linux / macOS:**
```bash
cd LTNC_Project
./mvnw clean compile
```

**Windows CMD:**
```cmd
cd D:\LTNC_Project
mvnw.cmd clean compile
```

> Lệnh `clean compile` sẽ xoá thư mục `target/` cũ và biên dịch lại toàn bộ source code. Nếu thành công, bạn sẽ thấy dòng `BUILD SUCCESS` ở cuối output.

### Package thành file JAR (tuỳ chọn)

```bash
# Linux / macOS
./mvnw clean package -DskipTests

# Windows CMD
mvnw.cmd clean package -DskipTests
```

File JAR sẽ được tạo tại `target/auctionapp-*.jar`.

### Xử lý lỗi compile thường gặp

| Lỗi | Nguyên nhân | Cách khắc phục |
|---|---|---|
| `JAVA_HOME not set` | Biến môi trường JAVA_HOME chưa trỏ đúng | Set lại JAVA_HOME về đường dẫn JDK trên máy bạn (kiểm tra trong `C:\Program Files\Java\`) |
| `package javafx.* does not exist` | JavaFX chưa được resolve qua Maven | Chạy `mvnw.cmd dependency:resolve` trước |
| `Could not connect to Supabase` | Thiếu biến môi trường `SUPABASE_SERVICE_KEY` | Thiết lập lại biến môi trường theo mục 2 |
| `Port 5000 already in use` | Server đang chạy ở terminal khác | Tắt process cũ hoặc đổi port trong `AuctionServer.java` |

---

## 5. Câu Lệnh Chạy Chương Trình

> **Lưu ý:** Phải chạy **Server trước**, sau đó mới chạy Client. Server và Client có thể chạy trên các máy khác nhau trong cùng mạng LAN — khi đó sửa địa chỉ IP trong `NetworkRouter.java` từ `localhost` sang IP máy chạy server.

### Bước 1 — Chuyển vào thư mục project & thiết lập môi trường

**Windows CMD:**
```cmd
d:
cd D:\LTNC_Project
rem Thay đường dẫn bên dưới cho khớp với phiên bản JDK trên máy bạn
rem Ví dụ: jdk-21, jdk-23, jdk-25.0.2, ... — kiểm tra trong C:\Program Files\Java\
set JAVA_HOME=C:\Program Files\Java\jdk-25.0.2
set SUPABASE_SERVICE_KEY=your_service_role_key_here
```

**Linux / macOS:**
```bash
cd ~/LTNC_Project
# Thay đường dẫn cho khớp với phiên bản JDK trên máy bạn
# Kiểm tra bằng lệnh: ls /usr/lib/jvm/
export JAVA_HOME=C:\Program Files\Java\jdk-25.0.2
export SUPABASE_SERVICE_KEY=your_service_role_key_here
```

### Bước 2 — Chạy Server (Terminal 1)

**Windows CMD:**
```cmd
mvnw.cmd exec:java -Dexec.mainClass="com.example.auctionapp.server.AuctionServer"
```

**Linux / macOS:**
```bash
./mvnw exec:java -Dexec.mainClass="com.example.auctionapp.server.AuctionServer"
```

Khi thấy dòng sau là server đã sẵn sàng:
```
Auction Server is LIVE on port 5000!
Waiting for users to connect...
```

### Bước 3 — Chạy Client / Giao diện JavaFX (Terminal 2)

**Windows CMD:**
```cmd
mvnw.cmd javafx:run
```

**Linux / macOS:**
```bash
./mvnw javafx:run
```

> Có thể mở **nhiều cửa sổ client** cùng lúc để test concurrent bidding bằng cách mở thêm terminal mới và chạy lại lệnh trên.

---

## 6. Hướng Dẫn Chạy Server / Client Theo Thứ Tự Cụ Thể

1. Đảm bảo biến môi trường `SUPABASE_SERVICE_KEY` đã được thiết lập
2. Mở **Terminal 1** → chuyển vào thư mục project, set `JAVA_HOME`, rồi chạy Server
3. Chờ server log: `Auction Server is LIVE on port 5000!`
4. Mở **Terminal 2** → chuyển vào thư mục project, set `JAVA_HOME`, rồi chạy Client
5. Giao diện JavaFX hiện ra → đăng ký tài khoản hoặc đăng nhập
6. Để test nhiều user đồng thời: mở thêm Terminal 3, 4,... và chạy lại lệnh client

> **Chạy trên mạng LAN:** Sửa `"localhost"` trong `NetworkRouter.java` thành IP của máy chạy server (ví dụ: `"192.168.1.10"`).

---

## 7. Danh Sách Chức Năng Đã Hoàn Thành

### Chức năng người dùng
- [x] Đăng ký tài khoản (validate email format)
- [x] Đăng nhập / Đăng xuất
- [x] Duyệt sản phẩm đấu giá theo danh mục
- [x] Xem chi tiết phiên đấu giá (giá hiện tại, lịch sử bid, countdown timer)
- [x] Đặt giá thầu thủ công (Manual Bid)
- [x] Đặt giá thầu tự động (Auto-Bid) với giới hạn ngân sách
- [x] Xem danh sách các phiên đấu giá mình đã tham gia (My Bids)
- [x] Đăng bán sản phẩm (kèm upload ảnh lên Supabase Storage)
- [x] Xem và chỉnh sửa sản phẩm đang đấu giá (My Auctions)
- [x] Nhận thông báo thời gian thực: `ITEM_SOLD`, `AUCTION_WON`, `AUCTION_LOST`, `ITEM_EXPIRED`
- [x] Xem lịch sử thông báo

### Chức năng hệ thống
- [x] Tự động đóng phiên đấu giá đúng giờ (scheduler mỗi 1 giây)
- [x] Broadcast cập nhật giá đến tất cả client đang online (Observer Pattern)
- [x] Xử lý concurrent bidding an toàn (ReentrantLock per AuctionSession)
- [x] Gia hạn phiên tự động khi có bid trong 120 giây cuối (Anti-Sniping)
- [x] Kiểm tra tự bid vào sản phẩm của chính mình (`SelfBiddingException`)
- [x] Kiểm tra bid thấp hơn mức tối thiểu (`InvalidBidException`)
- [x] Upload và nén ảnh tự động trước khi lưu (resize 800px, 75% JPEG)
- [x] Biểu đồ LineChart cập nhật giá realtime trong phiên đấu giá

### Chức năng Admin
- [x] Xem danh sách toàn bộ người dùng
- [x] Khoá (soft-delete) tài khoản người dùng
- [x] Xem danh sách tất cả phiên đấu giá đang hoạt động
- [x] Xoá phiên đấu giá

---

## 8. Link Báo Cáo PDF & Video Demo

- 📄 **Báo cáo PDF:** https://drive.google.com/file/d/1OvffyA-dUpeOG_9CTfTnerifzRtdUxP5/view?usp=sharing
- 🎥 **Video Demo:** https://screenapp.io/app/v/GMUNRtTjD7
