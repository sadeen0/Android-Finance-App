# Finance App 💰

A comprehensive personal finance management Android application built for tracking income, expenses, budgets, and financial statistics.

## 📱 Features

### 🔐 Authentication
- **User Registration & Login**: Secure user authentication system
- **Password Management**: Change password functionality
- **Session Management**: Persistent user sessions

### 💼 Financial Management
- **📊 Dashboard**: Overview of financial status with charts and summaries
- **💸 Expense Tracking**: Record and categorize expenses
- **💰 Income Management**: Track various income sources
- **🎯 Budget Planning**: Set and monitor budgets with alerts
- **📈 Statistics**: Visual analytics with charts and breakdowns
- **🧾 Transaction History**: Complete transaction management

### 🎨 User Experience
- **🌙 Dark/Light Theme**: Automatic theme switching
- **📱 Navigation Drawer**: Intuitive side navigation
- **🔄 Swipe Refresh**: Pull-to-refresh functionality
- **📊 Interactive Charts**: Visual data representation using MPAndroidChart
- **⚠️ Smart Notifications**: Budget alerts and warnings

## 🛠 Technical Specifications

### Platform
- **Target SDK**: Android 14 (API 36)
- **Minimum SDK**: Android 8.0 (API 26)
- **Language**: Java
- **Architecture**: MVVM (Model-View-ViewModel)

### Dependencies
- **AndroidX Libraries**: AppCompat, Material Design, ConstraintLayout
- **Navigation Component**: Fragment navigation
- **Lifecycle Components**: LiveData, ViewModel
- **Charts**: MPAndroidChart v3.1.0
- **SwipeRefreshLayout**: Pull-to-refresh support

### Build System
- **Gradle**: Android Gradle Plugin
- **Java**: Version 11 compatibility
- **View Binding**: Enabled for type-safe view references

## 🚀 Getting Started

### Prerequisites
- Android Studio Arctic Fox or later
- JDK 11 or higher
- Android SDK with API level 26+

### Installation
1. Clone the repository:
   ```bash
   git clone [repository-url]
   cd "Finance App"
   ```

2. Open the project in Android Studio

3. Sync Gradle files

4. Run the application on an emulator or physical device

### 🧪 Test Data Generation

The app includes a convenient test data generator for development and testing:

#### Method 1: Easter Egg (Recommended)
1. Open the app and navigate to the Login screen
2. **Tap the "Sign In" title 5 times** quickly
3. Wait for the toast: "Generating test data..."
4. Use the auto-filled credentials:
   - **Email**: `test@finance.com`
   - **Password**: `Test123`

#### Alternative Methods
- Check `TEST_DATA_INSTRUCTIONS.md` for additional data generation methods
- Manual test user registration through the Register screen

## 📁 Project Structure

```
app/
├── src/main/
│   ├── java/com/labproject/app/
│   │   ├── MainActivity.java              # Main navigation activity
│   │   ├── data/                          # Data layer
│   │   │   ├── db/                        # Database helpers
│   │   │   ├── session/                   # Session management
│   │   │   └── prefs/                     # Settings preferences
│   │   ├── ui/                            # UI layer
│   │   │   ├── auth/                      # Authentication screens
│   │   │   ├── home/                      # Dashboard
│   │   │   ├── expenses/                  # Expense tracking
│   │   │   ├── income/                    # Income management
│   │   │   ├── budgets/                   # Budget planning
│   │   │   ├── transactions/              # Transaction history
│   │   │   ├── statistics/                # Analytics & charts
│   │   │   ├── profile/                   # User profile
│   │   │   └── settings/                  # App settings
│   │   └── utils/                         # Utility classes
│   └── res/
│       ├── layout/                        # XML layouts
│       ├── drawable/                      # Icons & graphics
│       ├── values/                        # Colors, strings, styles
│       └── navigation/                    # Navigation graphs
```

## 🎨 UI Components

### Screens
- **Login/Register**: User authentication
- **Dashboard**: Financial overview with charts
- **Expenses**: Add and manage expenses
- **Income**: Track income sources  
- **Budgets**: Set and monitor budgets
- **Transactions**: Complete transaction history
- **Statistics**: Visual analytics and reports
- **Profile**: User account management
- **Settings**: App preferences and themes

### Custom Components
- **Budget Alert Items**: Smart budget notifications
- **Transaction Items**: Detailed transaction displays
- **Category Breakdowns**: Expense categorization
- **Interactive Charts**: Financial data visualization

## 🎯 Key Features

### Smart Budget Management
- Set monthly/yearly budgets
- Real-time spending tracking
- Automated alerts when approaching limits
- Visual progress indicators

### Comprehensive Analytics
- Income vs. Expense charts
- Category-wise breakdowns
- Monthly/yearly trends
- Interactive data visualization

### User-Friendly Design
- Material Design principles
- Responsive layouts for different screen sizes
- Smooth animations and transitions
- Intuitive navigation patterns

## 🔧 Development Notes

### Architecture Patterns
- **MVVM**: Clean separation of concerns
- **Repository Pattern**: Centralized data management
- **Observer Pattern**: Reactive UI updates

### Database
- SQLite with custom DBHelper
- Efficient query optimization
- Data integrity constraints

### Security
- Session management
- Input validation
- Secure credential storage

## 📄 License

This project is part of a course assignment (ADVANCED COMPUTER SYSTEMS ENGINEERING LABORATORY). 


---

**Note**: This is a student project developed as part of the ENCS5150 course. The application demonstrates modern Android development practices and financial management concepts.