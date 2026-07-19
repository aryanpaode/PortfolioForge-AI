# PortfolioForge AI

## Project Title
PortfolioForge AI: Mobile-Based Resume and Portfolio Generator

## Project Overview
PortfolioForge AI is a mobile application designed to help students create a professional resume and public portfolio from scattered career-related information. Students often have certificates, resumes, project details, GitHub repositories, LinkedIn/profile data, and academic details stored in different places. This app brings all of that information together, allows the user to verify and edit it, and generates a structured resume and shareable portfolio link.

## Problem Statement
Many students struggle to maintain a proper resume and portfolio because their achievements are scattered across certificates, PDFs, images, GitHub repositories, LinkedIn profiles, and manually maintained documents. This leads to incomplete resumes, weak project descriptions, missing proof of achievements, and poorly organized portfolios.

## Proposed Solution
The proposed mobile app will allow users to scan or upload certificates, resumes, and other career documents. The app will use OCR to extract useful text, allow the user to verify and edit extracted details, organize information into categories such as education, skills, projects, internships, certifications, and achievements, and generate a resume and public portfolio link.

## Key Features
- Student profile creation
- Manual entry of verified fields such as CGPA, branch, semester, and contact details
- Upload/scan certificate images and PDFs
- OCR-based text extraction
- Edit and verify extracted information
- Add projects, internships, certifications, publications, and achievements
- GitHub repository input for project details
- Resume generation
- Portfolio page generation
- Public shareable portfolio link
- SQLite-based local storage
- Firebase authentication and cloud synchronization

## Technology Stack
- Android Studio
- Java / XML for Android UI
- SQLite for local database storage
- Firebase Authentication
- Firebase Firestore
- Firebase Storage
- OCR/Text Recognition module
- Optional web portfolio hosted using Firebase Hosting

## Syllabus Alignment
| Syllabus Topic | Implementation in Project |
|---|---|
| Android Development Platform | Android Studio-based mobile application |
| UI Components | Forms, buttons, text fields, menus, dialogs, portfolio/resume screens |
| Images and File Handling | Uploading certificates, resumes, PDFs, and exporting resume/portfolio data |
| SQLite Database | Offline storage of profile, extracted data, and resume drafts |
| Firebase | Authentication, cloud database, file storage, and sync |
| Client-Server Application | Mobile app communicates with Firebase backend |
| Application Publishing | APK generation and future Play Store deployment |

## Expected Output
The final app will allow a student to:
1. Create a profile
2. Upload or scan documents
3. Extract and verify career information
4. Generate a resume
5. Generate a public portfolio link
6. Share the portfolio link with faculty, recruiters, or peers

## Future Enhancements
- AI-based resume scoring
- ATS keyword matching
- Multiple resume templates
- Multiple portfolio templates
- LinkedIn profile improvement suggestions
- One-click publishing to a custom portfolio domain
- Play Store publishing and monetization

## Project Status
Proposal stage / Under development
