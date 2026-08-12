# Implementation Plan: Converting WebViews to Native Java Android with REST APIs

Convert the remaining legacy WebView-based modules (`leaveform.php` in `HomeFragment` and `employee_details.php` in `SettingFragment`) into **100% Native Java Android UI components** powered by backend JSON REST APIs, matching the modern architecture established by `MoveSiteFragment`.

---

## Technical Overview & Architecture

Currently, two key tabs in the application rely on WebViews to render server-side PHP pages:
1. **Leave Request Tab (`HomeFragment`)**: Loads `leaveform.php` in a WebView.
2. **Labour Management Tab (`SettingFragment`)**: Loads `employee_details.php` in a WebView.

### Native Conversion Blueprint

```
 ┌─────────────────────────────────────────────────────────────────────────────┐
 │                         Native Android Java UI                              │
 ├──────────────────────────────────────┬──────────────────────────────────────┤
 │   HomeFragment (Native Leave Form)   │ SettingFragment (Native Labour Mgmt) │
 │  • Leave Type Spinner                │  • Department & Project Filter       │
 │  • Date Range Pickers                │  • Worker Search Bar                 │
 │  • Reason Input & Attachment         │  • Worker Cards RecyclerView         │
 │  • Leave History RecyclerView        │  • Native TIME IN / TIME OUT Actions │
 └──────────────────┬───────────────────┴──────────────────┬───────────────────┘
                    │                                      │
                    ▼                                      ▼
 ┌──────────────────────────────────────┐┌──────────────────────────────────────┐
 │   LeaveFormApi (Volley REST JSON)    ││ LabourManagementApi (Volley REST)  │
 ├──────────────────────────────────────┤├──────────────────────────────────────┤
 │  • get_leave_types.php               ││  • api_get_employees.php             │
 │  • submit_leave.php                  ││  • api_mark_attendance.php           │
 │  • get_leave_history.php             ││  • AttendanceRepository (Offline Queue)│
 └──────────────────────────────────────┘└──────────────────────────────────────┘
```

---

## User Review Required

> [!IMPORTANT]
> **Backend JSON Endpoints Required**:
> Converting WebViews to native Java UI requires that the backend server provides JSON REST endpoints for Leave Form operations and Employee Details/Attendance operations.
> - **For Leave Form**: JSON endpoints to fetch leave types, submit leave request, and fetch leave status history.
> - **For Labour Management**: JSON endpoint returning employee list with today's punch statuses (IN/OUT time, location, shift) and JSON endpoint to submit attendance punches.
> If these backend endpoints are currently under development or need to be mapped to existing server APIs, we can mock/integrate them using Volley requests to match your exact backend response schemas.

---

## Proposed Changes

### Phase 1: Native Leave Form Module (`HomeFragment`)

#### [MODIFY] [HomeFragment.java](file:///d:/4s-contracting/app/src/main/java/com/app/fourscontracting/ui/home/HomeFragment.java)
- Remove `WebView`, `WebChromeClient`, and `WebViewClient`.
- Implement native date pickers (`DatePickerDialog`) for From Date and To Date.
- Bind `MaterialAutoCompleteTextView` / `Spinner` for leave type selection.
- Implement `RecyclerView` for viewing past leave requests with status badges (Approved, Pending, Rejected).
- Handle native form submission via `LeaveFormApi`.

#### [MODIFY] [fragment_home.xml](file:///d:/4s-contracting/app/src/main/res/layout/fragment_home.xml)
- Replace WebView layout with a scrollable Material Form:
  - Project Header / Selector.
  - Leave Type dropdown card.
  - Start Date & End Date selection buttons.
  - Reason `TextInputEditText`.
  - Submit Leave Request Button with loading progress indicator.
  - "Recent Leave Applications" section with `RecyclerView`.

#### [NEW] `com.app.fourscontracting.data.LeaveFormApi`
- REST API client using Volley to perform:
  - `fetchLeaveTypes(String uid, ResponseListener)`
  - `submitLeaveRequest(LeaveRequestPayload, ResponseListener)`
  - `fetchLeaveHistory(String uid, ResponseListener)`

#### [NEW] `com.app.fourscontracting.data.LeaveRequestModel` & `LeaveAdapter`
- Data model representing leave records (`id`, `leaveType`, `fromDate`, `toDate`, `reason`, `status`, `appliedOn`).
- `RecyclerView.Adapter` to render leave history cards.

---

### Phase 2: Native Labour Management Module (`SettingFragment`)

#### [MODIFY] [SettingFragment.java](file:///d:/4s-contracting/app/src/main/java/com/app/fourscontracting/SettingFragment.java)
- Replace `mWebView` with native `RecyclerView` displaying worker cards.
- Add Search Bar (`SearchView` / `EditText`) to filter workers dynamically by name or ID.
- Retain existing `LocationVerifyActivity` supervisor session check (`SessionPrefs.isLocationSessionValid()`) before enabling worker punch actions.
- Wire native action buttons on worker cards:
  - **TIME IN**: Triggers native camera photo capture + location verification → `AttendanceRepository.submit()`.
  - **TIME OUT**: Triggers native checkout flow.
  - **MOVE SITE**: Navigates to native `MoveSiteFragment`.

#### [MODIFY] [fragment_setting.xml](file:///d:/4s-contracting/app/src/main/res/layout/fragment_setting.xml)
- Replace WebView layout with native Material dashboard layout:
  - Top Filter Bar (Department Filter Spinner + Search Field).
  - Summary Counters (Total Workers, On Site / In, Out, Absent).
  - Worker List `RecyclerView` with swipe-to-refresh (`SwipeRefreshLayout`).

#### [NEW] `com.app.fourscontracting.data.LabourManagementApi`
- REST API client using Volley for:
  - `fetchEmployees(String uid, String projId, String deptId, ResponseListener)`
  - `markAttendance(AttendancePayload, ResponseListener)` (integrated with existing offline queue).

#### [NEW] `com.app.fourscontracting.data.LabourEmployeeModel` & `LabourEmployeeAdapter`
- Data model for worker card state (`empId`, `empName`, `deptName`, `statusIn`, `timeIn`, `statusOut`, `timeOut`, `photoUrl`).
- `RecyclerView.Adapter` rendering modern worker cards with action buttons and real-time status indicators.

---

## Verification Plan

### Automated Tests
- Run unit tests to verify JSON parsing and URL construction:
  ```bash
  cmd.exe /c gradlew.bat test --no-daemon
  ```

### Manual Verification
- Compile and build debug APK:
  ```bash
  cmd.exe /c gradlew.bat assembleDebug --no-daemon
  ```
- Test Native Leave Form:
  - Select Project -> Pick Date Range -> Choose Leave Type -> Submit.
  - Verify smooth UI rendering without WebView loading delays.
- Test Native Labour Management:
  - Department filtering & real-time search.
  - Tap TIME IN / TIME OUT on worker cards -> Verify camera capture and location validation flow.
