# MediGrid - Healthcare Network Dashboard

A comprehensive Android healthcare management application built with Jetpack Compose, designed to
monitor and manage healthcare networks, clinics, patients, and medical resources.

## Features

### 🏥 Healthcare Network Overview

- Real-time monitoring of clinic status (Online, Backup Power, Offline)
- Network map visualization
- Power status monitoring with load-shedding awareness
- Emergency alert system

### 📊 Dashboard Analytics

- Live statistics for active clinics, patient count, and emergencies
- Network uptime monitoring
- Patient flow analytics
- Beautiful gradient UI cards with status indicators

### 🏥 Clinic Management

- Comprehensive clinic network overview
- Real-time status indicators (online/backup/offline)
- Staff and patient count tracking
- Power status monitoring per clinic

### 👥 Patient Management

- Patient database with detailed information
- Status tracking (Active, Follow-up, Critical)
- Last visit timestamps
- Clinic assignment tracking

### 🎨 Modern UI Design

- Material Design 3 with healthcare-focused color scheme
- Responsive layout with navigation drawer
- Beautiful gradient status indicators
- Clean, professional medical interface
- Status badges with appropriate color coding

## Technical Architecture

### Built With

- **Jetpack Compose** - Modern UI toolkit
- **Material Design 3** - Design system
- **Kotlin** - Programming language
- **MVVM Architecture** - Clean code structure

### Key Components

1. **MainActivity** - Main app entry point with navigation
2. **NavigationDrawer** - Side navigation with healthcare sections
3. **DashboardScreen** - Main overview with statistics and alerts
4. **ClinicsScreen** - Clinic network management
5. **PatientsScreen** - Patient information management
6. **StatCard** - Reusable statistic display component

### Color Scheme

- **Primary Blue**: `#2563eb` - Professional medical blue
- **Success Green**: `#10b981` - Healthy/online status
- **Warning Orange**: `#f59e0b` - Backup power/caution
- **Danger Red**: `#ef4444` - Critical alerts/offline status
- **Background**: `#f1f5f9` - Clean, medical environment

## Features Implemented

### ✅ Completed

- ✅ Dashboard with live statistics
- ✅ Clinic network overview
- ✅ Patient management interface
- ✅ Medicine inventory management
- ✅ Emergency alert center
- ✅ Power status monitoring
- ✅ Healthcare analytics
- ✅ System settings
- ✅ Navigation system
- ✅ Beautiful UI components
- ✅ Status indicators
- ✅ Alert system display

### 🎯 All Core Features Complete

All major healthcare management features have been implemented:

#### **Medicine Inventory Management**

- Comprehensive inventory statistics
- Low stock and expiry alerts
- Medicine categorization
- Multi-clinic inventory tracking
- Stock level monitoring

#### **Emergency Alert System**

- Real-time emergency alerts
- Priority-based alert handling (Urgent/Warning/Info)
- Detailed patient information
- Emergency dispatch functionality
- Response time tracking

#### **Power Status Monitoring**

- Load-shedding schedule integration
- Battery status monitoring across clinics
- Real-time power grid status
- Backup power management
- South African load-shedding awareness

#### **Healthcare Analytics**

- Patient flow trend analysis
- Health condition distribution
- Medicine usage analytics
- Emergency response time metrics
- Interactive chart placeholders (ready for real data)

#### **System Settings**

- Organization configuration
- Emergency contact management
- Notification preferences
- API configuration
- Sync interval management

## Sample Data

The app includes realistic sample data representing:

- **28 Active Clinics** across South African communities
- **247 Patients** with various medical conditions
- **5 Active Emergencies** with different priority levels
- **Clinic locations** including Soweto, Alexandra, Johannesburg, Orange Farm, and Midrand

## South African Context

Designed specifically for the South African healthcare environment:

- Load-shedding awareness and backup power monitoring
- Community clinic focus (Soweto, Alexandra, etc.)
- Resource constraint considerations
- Emergency response optimization

## Installation & Setup

1. Clone the repository
2. Open in Android Studio
3. Sync project dependencies
4. Run on device or emulator

```bash
git clone <repository-url>
cd MediGrid
./gradlew assembleDebug
```

## Requirements

- Android Studio Arctic Fox or newer
- Minimum SDK: 25 (Android 7.1)
- Target SDK: 35 (Android 15)
- Kotlin 2.0.21

## Screenshots

The app features:

- Modern dashboard with health statistics
- Clinic status overview with real-time indicators
- Patient management with status tracking
- Professional medical-grade UI design

## Responsive Design Features

### ✅ **Multi-Device Support**

- **Adaptive Navigation:** Hamburger menu for mobile/portrait, permanent sidebar for
  landscape/desktop
- **Responsive Grid Layouts:** Dynamic column counts based on screen size
- **Flexible Typography:** Font sizes adapt to screen dimensions
- **Touch-Friendly Interface:** Optimized button sizes and spacing for mobile
- **Portrait & Landscape Support:** Seamless orientation changes

### 🎯 **Screen Size Breakpoints**

- **Large Screens (1200dp+):** 4-column layouts, full feature set
- **Medium Screens (800-1200dp):** 3-column layouts, permanent sidebar in landscape
- **Small Tablets (600-800dp):** 2-column layouts, drawer navigation
- **Phones (<600dp):** Single column layouts, compact UI elements, hamburger menu

### 📱 **Mobile-First Optimizations**

- **Hamburger Menu:** Three-line menu icon for easy navigation access
- **Modal Navigation Drawer:** Slide-out sidebar that auto-closes after selection
- **Compact Status Indicators:** Simplified status bar for small screens
- **Responsive Card Layouts:** Adaptive padding and font sizes
- **Touch Targets:** Minimum 44dp touch areas for accessibility

## Future Enhancements

- Real-time data synchronization
- GPS clinic location mapping
- Medicine inventory tracking
- Emergency dispatch system
- Advanced analytics and reporting
- Multi-language support (English, Afrikaans, Zulu, etc.)

## Contributing

Built as a comprehensive healthcare management solution for South African medical networks.

## License

Healthcare management solution for educational and demonstration purposes.