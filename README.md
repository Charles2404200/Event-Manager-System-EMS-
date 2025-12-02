# Event Manager System (EMS)

## 📋 Tổng Quan

Event Manager System là một hệ thống quản lý sự kiện toàn diện được xây dựng bằng JavaFX và PostgreSQL. Hệ thống hỗ trợ quản lý nhiều loại sự kiện như hội nghị, workshop, hòa nhạc, và triển lãm, với các chức năng đăng ký, quản lý diễn giả, lịch trình phiên, quản lý vé, và báo cáo.

## 🎯 Tính Năng Chính

### Quản Lý Người Dùng Đa Role
- **Anonymous Visitor**: Xem sự kiện công khai, lọc và tìm kiếm
- **Attendee**: Đăng ký sự kiện, xem vé, xuất lịch trình
- **Presenter**: Xem phiên được gán, tải tài liệu, xem thống kê
- **Event Admin**: Quản lý sự kiện, phiên, vé, gán diễn giả
- **System Admin**: Quản lý toàn bộ hệ thống, người dùng, báo cáo

### Quản Lý Sự Kiện
- ✅ Tạo, cập nhật, xóa sự kiện
- ✅ Phân loại theo loại: Conference, Workshop, Concert, Exhibition
- ✅ Trạng thái: Scheduled, Ongoing, Completed, Cancelled
- ✅ Lọc và tìm kiếm sự kiện

### Quản Lý Session (Phiên)
- ✅ CRUD operations cho session
- ✅ Gán diễn giả cho session
- ✅ Kiểm tra xung đột lịch trình
- ✅ Quản lý capacity (sức chứa)
- ✅ Lưu trữ tài liệu session

### Quản Lý Vé
- ✅ Phát hành vé tự động khi đăng ký
- ✅ Loại vé: General, VIP, Early Bird
- ✅ Trạng thái: Active, Used, Cancelled
- ✅ QR code cho vé

### Dashboard Theo Role
- ✅ Giao diện tùy chỉnh theo vai trò người dùng
- ✅ Thống kê và số liệu nhanh
- ✅ Quick actions cho từng role
- ✅ Responsive UI với JavaFX

## 🏗️ Kiến Trúc Hệ Thống

### Cấu Trúc Thư Mục
```
Event_Manager_System/
├── src/
│   ├── main/
│   │   ├── java/org/ems/
│   │   │   ├── application/          # Service Layer
│   │   │   │   ├── impl/            # Service implementations
│   │   │   │   └── service/         # Service interfaces
│   │   │   ├── config/              # Configuration
│   │   │   │   ├── AppContext.java  # Singleton context
│   │   │   │   └── DatabaseConfig.java
│   │   │   ├── domain/              # Domain Layer
│   │   │   │   ├── dto/            # Data Transfer Objects
│   │   │   │   ├── model/          # Domain models
│   │   │   │   │   ├── Person.java
│   │   │   │   │   ├── Attendee.java
│   │   │   │   │   ├── Presenter.java
│   │   │   │   │   ├── Event.java
│   │   │   │   │   ├── Session.java
│   │   │   │   │   ├── Ticket.java
│   │   │   │   │   └── enums/
│   │   │   │   └── repository/     # Repository interfaces
│   │   │   ├── infrastructure/      # Infrastructure Layer
│   │   │   │   ├── db/             # Database initialization
│   │   │   │   ├── repository/jdbc/ # JDBC implementations
│   │   │   │   └── util/           # Utilities
│   │   │   └── ui/                 # Presentation Layer
│   │   │       ├── MainApp.java    # JavaFX Application
│   │   │       ├── controller/     # FXML Controllers
│   │   │       ├── stage/          # Scene management
│   │   │       └── view/           # FXML views
│   │   └── resources/
│   │       ├── db/schema.sql       # Database schema
│   │       └── org/ems/ui/view/    # FXML files
│   └── test/java/                  # Unit tests
├── pom.xml                         # Maven configuration
└── README.md
```

### Design Patterns
- **Singleton**: AppContext
- **Repository Pattern**: Data access layer
- **Service Layer Pattern**: Business logic
- **MVC Pattern**: UI architecture
- **Factory Pattern**: Object creation

## 🛠️ Công Nghệ Sử Dụng

### Backend
- **Java 17+**: Core programming language
- **Maven**: Build automation và dependency management
- **PostgreSQL**: Relational database
- **JDBC**: Database connectivity

### Frontend
- **JavaFX 22**: Desktop GUI framework
- **FXML**: Declarative UI markup
- **CSS**: Styling

### Libraries
- **PostgreSQL JDBC Driver**: Database connection
- **JavaFX Controls**: UI components

## 📦 Cài Đặt

### Yêu Cầu Hệ Thống
- Java JDK 17 trở lên
- PostgreSQL 12 trở lên
- Maven 3.6+
- IDE: IntelliJ IDEA / Eclipse / VS Code (khuyến nghị IntelliJ IDEA)

### Bước 1: Clone Repository
```bash
git clone https://github.com/your-username/Event_Manager_System.git
cd Event_Manager_System
```

### Bước 2: Cấu Hình Database
1. Tạo database trong PostgreSQL:
```sql
CREATE DATABASE event_manager_db;
```

2. Cập nhật thông tin kết nối trong `DatabaseConfig.java`:
```java
private static final String URL = "jdbc:postgresql://localhost:5432/event_manager_db";
private static final String USER = "your_username";
private static final String PASSWORD = "your_password";
```

3. Chạy schema.sql để tạo bảng:
```bash
psql -U your_username -d event_manager_db -f src/main/resources/db/schema.sql
```

### Bước 3: Build Project
```bash
mvn clean install
```

### Bước 4: Run Application
```bash
mvn javafx:run
```

Hoặc chạy từ IDE:
- Right-click `MainApp.java` → Run 'MainApp.main()'

## 👥 Tài Khoản Mặc Định

### System Admin
- **Username**: `admin`
- **Password**: `admin123`
- **Email**: `admin@ems.com`

### Test Accounts
- **Attendee**: `tamdang@` / `123456`
- **Presenter**: `presenter1` / `password`
- **Event Admin**: `eventadmin` / `admin123`

## 📖 Hướng Dẫn Sử Dụng

### Đăng Nhập
1. Khởi động ứng dụng
2. Trang Home → Click "Login"
3. Nhập username/email và password
4. Click "Login" → Chuyển đến Dashboard theo role

### Đăng Ký Tài Khoản Mới
1. Trang Home → Click "Sign Up"
2. Điền thông tin:
   - Full Name
   - Email
   - Username (tối thiểu 3 ký tự)
   - Password (tối thiểu 6 ký tự)
   - Phone
   - Role (Attendee/Presenter)
3. Click "Sign Up" → Tự động đăng nhập và chuyển đến Dashboard

### Dashboard Attendee
- **Browse Events**: Xem và lọc sự kiện
- **My Tickets**: Xem vé đã đăng ký
- **My Registrations**: Danh sách session đã đăng ký
- **Export Schedule**: Xuất lịch trình cá nhân

### Dashboard Presenter
- **My Sessions**: Xem session được gán
- **Upload Materials**: Tải tài liệu cho session
- **Statistics**: Xem thống kê (sessions, audience, engagement)
- **Export Summary**: Xuất báo cáo hoạt động

### Dashboard Event Admin
- **Manage Events**: Tạo, sửa, xóa sự kiện
- **Manage Sessions**: Quản lý session trong sự kiện
- **Manage Tickets**: Quản lý loại vé và phát hành
- **Assign Presenters**: Gán diễn giả cho session
- **Generate Reports**: Tạo báo cáo sự kiện

### Dashboard System Admin
- **Manage Users**: CRUD tất cả người dùng
- **Manage Events/Sessions**: Quản lý toàn bộ hệ thống
- **System Reports**: Báo cáo tổng quan hệ thống
- **Activity Logs**: Xem lịch sử thao tác
- **Settings**: Cấu hình hệ thống

## 🎨 UI/UX Features

### Color Scheme
- **Primary Blue**: `#3498db` - Hành động chính
- **Success Green**: `#2ecc71` - Thành công
- **Warning Orange**: `#f39c12` - Cảnh báo
- **Special Purple**: `#9b59b6` - Đặc biệt
- **Error Red**: `#e74c3c` - Lỗi/Báo cáo
- **Secondary Gray**: `#95a5a6` - Phụ

### Responsive Design
- ScrollPane cho nội dung dài
- VBox/HBox layout linh hoạt
- Statistics cards cho số liệu
- Color-coded buttons theo chức năng

## 📊 Database Schema

### Main Tables
- **persons**: Base table cho tất cả users
- **attendees**: Thông tin attendee
- **presenters**: Thông tin presenter
- **events**: Sự kiện
- **sessions**: Phiên trong sự kiện
- **tickets**: Vé
- **session_presenters**: Quan hệ session-presenter (many-to-many)
- **attendee_sessions**: Đăng ký session của attendee

### Key Relationships
```
Person (1) → (many) Attendee/Presenter
Event (1) → (many) Session
Session (many) ↔ (many) Presenter
Attendee (many) ↔ (many) Session
Attendee (1) → (many) Ticket
```

## 🧪 Testing

### Run Tests
```bash
mvn test
```

### Test Coverage
- Unit tests cho services
- Integration tests cho repositories
- UI tests (manual)

## 📁 Tài Liệu Bổ Sung

### Guides
- **SESSION_MANAGER_GUIDE.md**: Hướng dẫn quản lý session chi tiết
- **DASHBOARD_USER_GUIDE.md**: Hướng dẫn sử dụng dashboard theo role
- **DASHBOARD_IMPLEMENTATION.md**: Tài liệu kỹ thuật dashboard
- **DASHBOARD_SUMMARY.md**: Tóm tắt các thay đổi dashboard

## 🔒 Security

### Authentication
- Password hashing (cần implement BCrypt)
- Session management qua AppContext
- Role-based access control (RBAC)

### Authorization
- UI visibility based on role
- Server-side permission checks
- Logout clears session

## 🐛 Known Issues & TODOs

### Current Limitations
- [ ] Password chưa hash (đang lưu plain text)
- [ ] Chưa implement payment processing
- [ ] Statistics cards hiển thị giá trị hardcoded
- [ ] Một số action methods chưa implement

### Future Enhancements
- [ ] Implement BCrypt password hashing
- [ ] Add email notification system
- [ ] Real-time updates với WebSocket
- [ ] Mobile app integration
- [ ] Advanced reporting with charts
- [ ] Export to PDF/Excel
- [ ] Dark mode support
- [ ] Multi-language support

## 🤝 Contributing

### How to Contribute
1. Fork repository
2. Create feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit changes (`git commit -m 'Add AmazingFeature'`)
4. Push to branch (`git push origin feature/AmazingFeature`)
5. Open Pull Request

### Code Style
- Follow Java naming conventions
- Use meaningful variable/method names
- Add JavaDoc comments for public methods
- Write unit tests for new features

## 📝 Changelog

### Version 2.0 (December 3, 2025)
- ✅ Implemented role-based dashboard
- ✅ Added session manager with full CRUD
- ✅ Auto-login after signup
- ✅ Enhanced UI/UX với color scheme
- ✅ Fixed XML parsing errors
- ✅ Added comprehensive documentation

### Version 1.0 (Initial Release)
- ✅ Basic CRUD operations
- ✅ User authentication
- ✅ Event and session management
- ✅ Database integration

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 👨‍💻 Authors

- **EMS Development Team**
- Contact: ems-team@example.com

## 🙏 Acknowledgments

- JavaFX community for excellent documentation
- PostgreSQL team for robust database
- Maven for build automation
- IntelliJ IDEA for best IDE

## 📞 Support

### Getting Help
- Check documentation in `/docs` folder
- Review guides: SESSION_MANAGER_GUIDE.md, DASHBOARD_USER_GUIDE.md
- Submit issues on GitHub
- Contact: support@ems.com

### FAQ

**Q: Không kết nối được database?**  
A: Kiểm tra PostgreSQL đang chạy, credentials trong DatabaseConfig.java đúng, và database đã được tạo.

**Q: Lỗi "cannot find javafx" khi build?**  
A: Đảm bảo đã cài Java 17+ và Maven download đúng dependencies.

**Q: Quên password admin?**  
A: Reset trong database: `UPDATE persons SET password_hash='admin123' WHERE username='admin';`

**Q: Làm sao thêm role mới?**  
A: Thêm vào Role.java enum, update DashboardController, và tạo section mới trong dashboard.fxml.

**Q: Dashboard không hiển thị đúng?**  
A: Clear cache: `mvn clean`, rebuild project, và restart application.

## 🚀 Deployment

### Production Deployment
1. Build JAR file: `mvn clean package`
2. Configure production database
3. Set environment variables for DB credentials
4. Run: `java -jar target/event-manager-system.jar`

### Docker Deployment (TODO)
```dockerfile
# Dockerfile
FROM openjdk:17-jdk-alpine
COPY target/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
```

---

**Built with ❤️ by EMS Team**  
**Last Updated**: December 3, 2025  
**Version**: 2.0

