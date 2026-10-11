# CampusApp – Architecture Overview

## Team: Android Architecture
Members: Manda Dickson, Thabo muliwana ,Nkonde Rachael zimba

## Clean Architecture Flow

UI (Activities / Fragments)
↓
ViewModel
↓
Repository Interface
↓
┌─────────────────────┬──────────────────────────┐
│ LocalRepository     │ FakeStudentRepository    │
│ (offline / AppData) │ (for testing)            │
└─────────────────────┴──────────────────────────┘

## Key Files Created / Owned by Architecture Team

- viewmodel/AuthViewModel.java
- viewmodel/RegisterViewModel.java
- viewmodel/ProfileViewModel.java
- viewmodel/RosterViewModel.java
- viewmodel/EditorViewModel.java
- data.repository/StudentRepository.java (interface)
- data.repository/AuthRepository.java (interface)
- data.repository/LocalStudentRepository.java
- data.repository/FakeStudentRepository.java

## Testing Evidence

Screen rotation test screenshots are in this docs folder