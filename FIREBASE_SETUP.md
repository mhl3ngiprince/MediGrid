# Firebase Setup Guide for MediGrid Healthcare Application

## 🔥 Firebase Integration for Healthcare Authentication

This guide will help you set up Firebase Authentication for the MediGrid healthcare management
system with proper security configurations.

---

## 📋 **Prerequisites**

1. **Google Account** with access to Firebase Console
2. **Healthcare Organization Domain** (for email validation)
3. **Android Studio** with the project properly imported
4. **Valid Healthcare License** (for role verification)

---

## 🚀 **Step 1: Create Firebase Project**

### 1.1 Go to Firebase Console

- Visit [Firebase Console](https://console.firebase.google.com/)
- Click "Create a project"
- Enter project name: `medigrid-healthcare-[your-org]`

### 1.2 Configure Project Settings

```yaml
Project Name: MediGrid Healthcare
Project ID: medigrid-healthcare-sa
Analytics: Enable (for security monitoring)
```

### 1.3 Healthcare-Specific Settings

- **Location**: Choose closest to your healthcare facilities
- **Default GCP resource location**: `us-central1` or `europe-west1`
- **Enable Google Analytics**: Yes (for security insights)

---

## 🔐 **Step 2: Configure Authentication**

### 2.1 Enable Authentication Methods

1. Go to **Authentication > Sign-in method**
2. Enable the following providers:

#### Email/Password Authentication

```yaml
Status: Enabled
Email Enumeration Protection: Enabled
Email Link Sign-in: Disabled (security)
```

#### Healthcare Domain Restrictions

Add your healthcare organization domains:

```
- health.gov.za
- [your-hospital].co.za
- [your-clinic].org.za
```

### 2.2 Configure Advanced Settings

```yaml
Authorized Domains:
  - localhost (for development)
  - [your-production-domain].com
  - [your-healthcare-org].co.za

Password Requirements:
  - Minimum Length: 12 characters
  - Require Uppercase: Yes
  - Require Lowercase: Yes
  - Require Numbers: Yes
  - Require Special Characters: Yes
```

---

## 🔧 **Step 3: Set Up Firestore Database**

### 3.1 Create Database

1. Go to **Firestore Database**
2. Click "Create database"
3. Choose **Production mode** for healthcare data
4. Select location closest to your facilities

### 3.2 Healthcare Data Structure

```javascript
// Healthcare Users Collection
healthcare_users: {
  [userUID]: {
    id: string,
    email: string,
    username: string,
    role: 'DOCTOR' | 'NURSE' | 'PHARMACIST' | 'ADMIN' | 'RECEPTIONIST',
    clinicId: string,
    phiAccessLevel: string,
    mfaEnabled: boolean,
    isActive: boolean,
    licenseNumber?: string,
    department?: string,
    lastLogin?: timestamp,
    createdAt: timestamp
  }
}

// Security Logs Collection (POPIA Compliance)
security_logs: {
  [logId]: {
    userId: string,
    action: string,
    timestamp: timestamp,
    details: object,
    ipAddress: string,
    deviceInfo: object
  }
}
```

### 3.3 Firestore Security Rules

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Healthcare users can only access their own data
    match /healthcare_users/{userId} {
      allow read, write: if request.auth != null 
        && request.auth.uid == userId
        && isHealthcareWorker(request.auth.token);
    }
    
    // Security logs - write only, admin read
    match /security_logs/{logId} {
      allow create: if request.auth != null;
      allow read: if request.auth != null 
        && isAdmin(request.auth.token);
    }
    
    // Helper functions
    function isHealthcareWorker(token) {
      return token.email_verified == true 
        && token.email.matches('.*@(health\\.gov\\.za|.*\\.co\\.za)$');
    }
    
    function isAdmin(token) {
      return isHealthcareWorker(token) 
        && get(/databases/$(database)/documents/healthcare_users/$(token.uid)).data.role == 'ADMIN';
    }
  }
}
```

---

## 📱 **Step 4: Android App Configuration**

### 4.1 Download Configuration File

1. Go to **Project Settings > Your apps**
2. Click **Android app**
3. Add app with package name: `com.example.medigrid`
4. Download `google-services.json`

### 4.2 Replace Configuration File

```bash
# Replace the template file with your actual Firebase config
cp ~/Downloads/google-services.json app/google-services.json
```

### 4.3 Verify Configuration

The downloaded file should contain your actual:

```json
{
  "project_info": {
    "project_number": "YOUR_ACTUAL_PROJECT_NUMBER",
    "project_id": "YOUR_ACTUAL_PROJECT_ID",
    "storage_bucket": "YOUR_PROJECT_ID.appspot.com"
  },
  "client": [{
    "client_info": {
      "mobilesdk_app_id": "YOUR_ACTUAL_APP_ID",
      "android_client_info": {
        "package_name": "com.example.medigrid"
      }
    }
  }]
}
```

---

## 🛡️ **Step 5: Healthcare Security Configuration**

### 5.1 Enable Security Features

```yaml
Authentication:
  - Email Verification: Required
  - Multi-factor Authentication: Recommended
  - Account Lockout: After 3 failed attempts
  - Session Timeout: 15 minutes

Data Protection:
  - Encryption at Rest: Enabled
  - Encryption in Transit: TLS 1.3
  - Field-level Encryption: For PHI fields
  - Audit Logging: All access logged
```

### 5.2 POPIA Compliance Settings

```yaml
Data Retention:
  - Medical Records: 6 years
  - Security Logs: 2 years
  - User Sessions: 30 days

Data Subject Rights:
  - Data Export: Enabled
  - Data Deletion: Enabled (with retention rules)
  - Access Logs: Available to data subjects
  - Consent Management: Tracked
```

---

## 🧪 **Step 6: Testing Configuration**

### 6.1 Test Authentication Flow

```kotlin
// Test healthcare worker registration
val testUser = mapOf(
    "email" to "doctor@yourorg.co.za",
    "password" to "SecurePass123!",
    "role" to "DOCTOR",
    "licenseNumber" to "SA12345"
)
```

### 6.2 Verify Security Features

- [ ] Email verification works
- [ ] Domain restriction enforced
- [ ] PHI access logged
- [ ] Session timeout active
- [ ] MFA prompts appear

---

## 🚨 **Step 7: Production Deployment**

### 7.1 Security Checklist

- [ ] Production Firebase project created
- [ ] Healthcare domain verification complete
- [ ] SSL certificates installed
- [ ] Firestore rules deployed
- [ ] Security monitoring enabled
- [ ] Backup procedures established

### 7.2 Healthcare Compliance

- [ ] POPIA compliance verified
- [ ] Data retention policies active
- [ ] Audit logging comprehensive
- [ ] Incident response plan ready
- [ ] Staff training completed

---

## 📞 **Support & Troubleshooting**

### Common Issues

#### 1. Authentication Errors

```
Error: "Please use your official healthcare organization email"
Solution: Add your domain to the healthcare domains list in FirebaseAuthService.kt
```

#### 2. Permission Denied

```
Error: "Firestore permission denied"
Solution: Check Firestore security rules match your user structure
```

#### 3. Email Verification

```
Error: "Email verification not received"
Solution: Check spam folder, verify domain is in authorized domains
```

### Healthcare-Specific Setup

```kotlin
// Add your healthcare domains to FirebaseAuthService.kt
private fun isValidHealthcareEmail(email: String): Boolean {
    val healthcareDomains = listOf(
        "health.gov.za",
        "yourhospital.co.za",    // Add your domain
        "yourclinic.org.za",     // Add your domain
        // Add more as needed
    )
}
```

---

## 🔒 **Security Best Practices**

1. **Never commit** `google-services.json` with real credentials to public repos
2. **Use separate** Firebase projects for development/staging/production
3. **Enable** Firebase App Check for production
4. **Monitor** authentication logs regularly
5. **Update** security rules as your app evolves
6. **Backup** Firestore data regularly
7. **Test** disaster recovery procedures

---

## 📚 **Additional Resources**

- [Firebase Authentication Documentation](https://firebase.google.com/docs/auth)
- [Firestore Security Rules](https://firebase.google.com/docs/firestore/security/rules)
- [POPIA Compliance Guide](https://popia.co.za/)
- [Healthcare Data Security Best Practices](https://www.hhs.gov/hipaa/for-professionals/security/index.html)

---

**Important**: This setup ensures your MediGrid application meets healthcare security standards
while maintaining POPIA compliance for South African healthcare organizations.