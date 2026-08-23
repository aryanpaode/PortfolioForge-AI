

## Project Title

**PortfolioForge AI: Mobile-Based Resume and Portfolio Generator with OCR and Skill Intelligence**

---

## Project Overview

PortfolioForge AI is a mobile application designed to help students create a professional resume and public portfolio from scattered career-related information.

Students often have important academic and career data stored in many different places, such as certificates, resumes, PDFs, images, GitHub repositories, LinkedIn/profile screenshots, project details, internship letters, and manually maintained documents. Because of this, many students end up with incomplete resumes, weak project descriptions, missing proof of achievements, and poorly organized portfolios.

PortfolioForge AI solves this problem by allowing students to scan or upload documents, extract useful information using OCR, verify and edit the extracted data, categorize skills and achievements, generate a structured resume, and publish a shareable public portfolio link.

---

## Problem Statement

Many students struggle to maintain a proper resume and portfolio because their achievements are scattered across certificates, PDFs, images, GitHub repositories, LinkedIn/profile data, and manually maintained files.

This leads to:

- incomplete resumes
- weak project descriptions
- missing certificates or achievement proof
- unorganized skill sections
- half-filled LinkedIn/profile details
- difficulty creating a clean portfolio for recruiters or faculty

A mobile-based system is needed to collect, organize, verify, and convert this scattered information into a professional resume and portfolio.

---

## Proposed Solution

The proposed application, **PortfolioForge AI**, will help students collect and organize their career-related data in one place.

The app will allow users to:

- create a student profile
- manually enter verified academic details such as CGPA, branch, semester, and contact information
- scan or upload certificates, resumes, internship letters, and project proof documents
- use OCR-assisted text extraction to read useful information from uploaded documents
- edit and verify extracted information
- add GitHub/profile links
- categorize skills, projects, internships, certifications, publications, and achievements
- generate a structured resume
- generate a public portfolio page
- copy and share a portfolio link that can be opened on any device

---

## Key Features

- Student profile creation
- Manual entry of verified fields such as CGPA, branch, semester, and contact details
- Upload or scan certificate images and PDFs
- OCR-assisted text extraction from documents
- Edit and verify extracted information
- Add projects, internships, certifications, publications, and achievements
- GitHub repository/profile input for project details
- Skill extraction and categorization
- Resume/profile completeness score
- Smart suggestions for improving resume and portfolio sections
- Resume generation
- Portfolio page generation
- Public shareable portfolio link
- SQLite-based local storage
- Firebase authentication and cloud synchronization

---

## AI-Assisted Features

PortfolioForge AI will include AI-assisted features to improve resume and portfolio creation.

### 1. OCR-Based Document Extraction

The app will use OCR to extract text from uploaded or scanned documents such as:

- certificates
- resumes
- internship letters
- project screenshots
- profile screenshots
- achievement documents

The extracted text can then be verified and edited by the user.

### 2. Skill Intelligence

The app will analyze extracted text and identify possible technical skills, tools, and domains.

Example:

If the extracted text contains:

`Python, Firebase, SQLite, Android Studio, Computer Vision, CNN`

The app can categorize them as:

| Category | Skills |
|---|---|
| Programming | Python |
| Mobile Development | Android Studio |
| Database | SQLite, Firebase |
| AI/ML | CNN, Computer Vision |

### 3. Document Classification

Uploaded documents can be classified into categories such as:

- Certification
- Internship
- Project
- Resume
- Publication
- Achievement

### 4. Resume/Profile Completeness Score

The app will calculate a resume readiness score based on missing or weak sections such as:

- missing GitHub link
- missing project descriptions
- missing technical skills
- missing certificates
- missing CGPA or education details
- weak project explanation
- missing measurable outcomes

### 5. Smart Suggestions

The app will provide suggestions such as:

- Add GitHub links for your projects.
- Add measurable outcomes such as accuracy, dataset size, or deployment result.
- Group your skills into categories.
- Add certificates as proof for listed skills.
- Improve project descriptions for better recruiter readability.

---

## Application Workflow

1. User creates an account or logs in.
2. User enters basic profile and academic details.
3. User uploads or scans certificates, resumes, or project documents.
4. OCR extracts text from the uploaded documents.
5. User verifies and edits the extracted information.
6. The app categorizes the data into sections such as education, skills, projects, internships, certifications, and achievements.
7. The app generates a resume preview.
8. The app generates a public portfolio preview.
9. User publishes the portfolio and copies the shareable link.
10. The portfolio link can be opened by anyone on any device.

---

## Technology Stack

| Component | Technology |
|---|---|
| Mobile App Development | Android Studio |
| Frontend UI | Jetpack Compose with Material 3 |
| Programming Language | Kotlin |
| Local Database | Room Database over SQLite |
| Cloud Database | Firebase Firestore |
| Authentication | Firebase Authentication |
| File Storage | Firebase Storage |
| OCR/Text Extraction | Google ML Kit Text Recognition |
| AI/Skill Intelligence | OCR-based extraction, skill categorization, resume scoring, and smart suggestions |
| Portfolio Hosting | Firebase Hosting / GitHub Pages |
| Version Control | GitHub |

---

## Syllabus Alignment

| Syllabus Topic | Implementation in Project |
|---|---|
| Configuration of Development Platform | Android Studio setup and Android app development |
| Application Design Components | Screens, layouts, buttons, forms, dialogs, menus, and navigation |
| Image Input and File Handling | Uploading/scanning certificates, resumes, PDFs, and project documents |
| SQLite Database | Offline storage of student profile, extracted text, resume drafts, and portfolio data |
| Firebase | Authentication, cloud database, file storage, and synchronization |
| Client-Server Application | Mobile app communicates with Firebase backend and hosted portfolio page |
| Application Publishing | APK generation and future Play Store deployment |
| Flutter Introduction | Can be considered as a future enhancement if cross-platform support is needed |

---

## Expected Output

The final application will allow a student to:

1. Create a digital career profile
2. Upload or scan documents
3. Extract text using OCR
4. Verify and edit extracted data
5. Organize skills, projects, internships, certifications, and achievements
6. Generate a resume
7. Generate and publish a portfolio page
8. Copy and share a public portfolio link

Example portfolio link:

`https://portfolioforge.web.app/student-name`

or

`https://aryanpaode.github.io/PortfolioForge-AI/`

---

## Project Modules

### 1. User Profile Module

Stores basic student information such as name, email, branch, semester, CGPA, GitHub link, LinkedIn/profile link, and career target.

### 2. Document Upload Module

Allows users to upload or scan certificates, resumes, internship letters, and other achievement proofs.

### 3. OCR Extraction Module

Extracts useful text from uploaded images and documents.

### 4. Verification and Editing Module

Allows users to check, correct, and approve the extracted information before it is used in the resume or portfolio.

### 5. Skill Intelligence Module

Identifies and categorizes technical skills, tools, and domains from extracted and manually entered data.

### 6. Resume Builder Module

Generates a structured resume using verified profile data.

### 7. Portfolio Builder Module

Generates a public portfolio page containing profile summary, skills, projects, internships, certifications, achievements, and contact links.

### 8. Cloud Sync Module

Stores user data and uploaded files using Firebase.

---

## Future Enhancements

- Multiple resume templates
- Multiple portfolio templates
- ATS keyword matching
- AI-assisted resume bullet generation
- LinkedIn/profile improvement suggestions
- GitHub repository analysis
- One-click portfolio publishing
- Custom portfolio domain support
- Play Store publishing and monetization

---

## Project Status

Proposal stage / Under development
